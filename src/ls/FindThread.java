package ls;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CountingInputStream;

public class FindThread extends Thread {

	public static volatile boolean running;
	
	private final LogSearchJFrame frame;
	private final File dir;
	private final Date startDate;
	private final String name;
	private final String text;
	private int files;
	private long bytes;
	
	public FindThread (LogSearchJFrame frame, File dir, Date startDate, String name, String text) {
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		this.frame = frame;
		this.dir = dir;
		this.startDate = startDate;
		this.name = name.toLowerCase();
		this.text = text;
	}
	
	@Override
	public void run () {
		try {
			System.out.println("run");
			running = true;
			long t = System.nanoTime();
			frame.searchUpdate(0);
			find(dir);
			long tns = System.nanoTime() - t;
			long ts = tns / 1000000000;
			long mb = bytes / 1000000;
			frame.searchEnd("Files: " + files + " Megabytes: " + mb + " Seconds: " + ts);
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, e.toString(), "Could not search", JOptionPane.ERROR_MESSAGE);
			
		} finally {
			running = false;
			frame.searchUpdate(-1);
		}
	}
	
	private List<Line> scanFile (File f) throws Exception {
		System.out.println("scan " + f);
		if (text.length() > 0) {
			try (InputStream is = new FileInputStream(f)) {
				if (f.getName().toLowerCase().endsWith(".gz")) {
					try (InputStream is2 = new GZIPInputStream(is)) {
						return scanInputStream(is2);
					}
				} else {
					return scanInputStream(is);
				}
			}
		} else {
			return Collections.emptyList();
		}
	}
	
	private List<Line> scanInputStream (InputStream is) throws Exception {
		List<Line> lines = new ArrayList<>();
		try (CountingInputStream cis = new CountingInputStream(is)) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
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
			bytes += cis.getByteCount();
		}
		System.gc();
		LogSearchUtil.slow();
		frame.searchUpdate(files++);
		return lines;
	}
	
	private void find (File dir) throws Exception {
		for (File file : dir.listFiles()) {
			if (!running) {
				break;
			}
			if (file.isFile()) {
				if (file.getName().toLowerCase().endsWith(".zip")) {
					findZip(file);
				} else if (file.getName().toLowerCase().contains(name)) {
					findFile(file);
				}
			} else if (file.isDirectory()) {
				find(file);
			}
		}
	}
	
	private void findFile (File file) throws Exception {
		Date date = LogSearchUtil.getFileNameDate(file.getName());
		if (date == null) {
			date = new Date(file.lastModified());
		}
		if (date.compareTo(startDate) >= 0) {
			frame.searchAdd(new Result(file.getName(), date, file, null, scanFile(file)));
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
				if (filename.toLowerCase().contains(name)) {
					Date date = LogSearchUtil.getFileNameDate(filename);
					if (date == null) {
						long t = ze.getTime();
						if (t > 0) {
							date = new Date(t);
						}
					}
					if (date != null && date.compareTo(startDate) >= 0) {
						List<Line> lines = Collections.emptyList();
						if (text.length() > 0) {
							try (InputStream is = zf.getInputStream(ze)) {
								lines = scanInputStream(is);
							}
						}
						frame.searchAdd(new Result(filename, date, file, ze.getName(), lines));
					}
				}
			}
		}
	}
	
}
