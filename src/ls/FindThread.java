package ls;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

public class FindThread extends Thread {
	public static volatile boolean running;
	
	private final LogSearchJFrame frame;
	private final File dir;
	private final Date startDate;
	private final String name;
	private final String text;
	private int files;
	
	public FindThread (LogSearchJFrame frame, File dir, Date startDate, String name, String text) {
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		this.frame = frame;
		this.dir = dir;
		this.startDate = startDate;
		this.name = name;
		this.text = text;
	}
	
	@Override
	public void run () {
		try {
			System.out.println("run");
			running = true;
			frame.update(0);
			find(dir);
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, e.toString(), "Could not search", JOptionPane.ERROR_MESSAGE);
			
		} finally {
			running = false;
			frame.update(-1);
		}
	}
	
	private List<Line> scan (File f) throws Exception {
		System.out.println("scan " + f);
		if (text.length() > 0) {
			try (InputStream is = new FileInputStream(f)) {
				if (f.getName().endsWith(".gz")) {
					try (InputStream is2 = new GZIPInputStream(is)) {
						return scan(is2);
					}
				} else {
					return scan(is);
				}
			}
		} else {
			return Collections.emptyList();
		}
	}
	
	private List<Line> scan (InputStream is) throws Exception {
		List<Line> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line;
			int number = 0;
			while ((line = br.readLine()) != null) {
				if (!running) {
					break;
				}
				number++;
				if (line.contains(text)) {
					System.out.println("matched line " + line);
					if (line.length() > 1000) {
						line = line.substring(0, 1000);
					}
					lines.add(new Line(line, number));
					if (lines.size() > 100) {
						break;
					}
				}
			}
		}
		return lines;
	}
	
	private void find (File dir) throws Exception {
		for (File file : dir.listFiles()) {
			if (!running) {
				break;
			}
			if (file.isFile()) {
				if (file.getName().endsWith(".zip")) {
					findZip(file);
				} else if (file.getName().contains(name)) {
					findFile(file);
				}
			} else if (file.isDirectory()) {
				find(file);
			}
			frame.update(files++);
		}
	}
	
	private void findFile (File file) throws Exception {
		Date date = getDate(file.getName());
		if (date == null) {
			date = new Date(file.lastModified());
		}
		if (date.after(startDate)) {
			frame.add(new Result(file.getName(), date, file, null, scan(file)));
		}
	}
	
	private void findZip (File file) throws Exception {
		try (ZipFile zf = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> e = zf.entries();
			while (e.hasMoreElements()) {
				if (!running) {
					break;
				}
				ZipEntry ze = e.nextElement();
				String filename = ze.getName();
				if (filename.contains("/")) {
					filename = filename.substring(filename.lastIndexOf("/") + 1);
				}
				if (filename.contains(name)) {
					Date date = getDate(filename);
					if (date == null) {
						long t = ze.getTime();
						if (t > 0) {
							date = new Date(t);
						}
					}
					if (date != null && date.after(startDate)) {
						List<Line> lines = Collections.emptyList();
						if (text.length() > 0) {
							try (InputStream is = zf.getInputStream(ze)) {
								lines = scan(is);
							}
						}
						frame.add(new Result(filename, date, file, ze.getName(), lines));
					}
				}
				frame.update(files++);
			}
		}
	}
	
	private static Date getDate (String name) {
		Pattern datePat = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
		Date date = null;
		if (name.length() > 0) {
			Matcher mat = datePat.matcher(name);
			if (mat.find()) {
				String dateStr = mat.group(1);
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				try {
					date = df.parse(dateStr);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return date;
	}
	
}
