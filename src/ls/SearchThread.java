package ls;

import java.io.*;
import java.util.*;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.input.CountingInputStream;

public class SearchThread extends Thread {

	public volatile boolean running;
	
	private final List<Result> results = new ArrayList<>();
	private final Map<File,ZipFile> zipFiles = new TreeMap<>();
	private final SearchListener listener;
	private final Set<File> dirs;
	private final Date startDate;
	private final Date endDate;
	private final String nameLower;
	private final String text;
	
	private long bytes;

	public SearchThread (SearchListener listener, Set<File> dirs, Date startDate, Date endDate, String name, String text) {
		super("SearchThread");
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		this.endDate = endDate;
		this.listener = listener;
		this.dirs = dirs;
		this.startDate = startDate;
		this.nameLower = name.toLowerCase();
		this.text = text;
	}
	
	@Override
	public void run () {
		try {
			System.out.println("run");
			running = true;
			listener.searchUpdate("finding");
			long t = System.nanoTime();
			for (File dir : dirs) {
				findDir(dir);
			}
			Collections.sort(results);
			scan();
			long tns = System.nanoTime() - t;
			long ts = tns / 1000000000;
			long mb = bytes / 1000000;
			listener.searchComplete("Files: " + results.size() + " Megabytes: " + mb + " Seconds: " + ts);
			
		} catch (Exception e) {
			e.printStackTrace();
			listener.searchComplete(e.toString());
			
		} finally {
			for (ZipFile zf : zipFiles.values()) {
				ZipFile.closeQuietly(zf);
			}
			running = false;
		}
	}

	private boolean testName (String name) {
		return name.toLowerCase().contains(this.nameLower);
	}
	
	private boolean testDate (Date date) {
		return date != null && (startDate == null || date.compareTo(startDate) >= 0) && (endDate == null || date.compareTo(endDate) < 0);
	}
	
	private void findDir (File dir) {
		for (File file : dir.listFiles()) {
			if (!running) {
				break;
			}
			
			try {
				if (file.isFile()) {
					if (file.getName().toLowerCase().endsWith(".zip")) {
						findZip(file);
						
					} else {
						findFile(file);
					}
					
				} else if (file.isDirectory()) {
					findDir(file);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void findFile (File file) {
		if (testName(file.getName())) {
			Date date = LogSearchUtil.getFileDate(file.getName(), file.lastModified());
			if (testDate(date)) {
				results.add(new Result(file, date, null));
			}
		}
	}
	
	private void findZip (final File file) throws Exception {
		System.out.println("find zip " + file);
		
		ZipFile zf = new ZipFile(file);
		boolean hasResult = false;
		
		Enumeration<ZipArchiveEntry> e = zf.getEntries();
		while (e.hasMoreElements()) {
			if (!running) {
				break;
			}
			
			ZipArchiveEntry ze = e.nextElement();
			String name = ze.getName();
			if (name.contains("/")) {
				name = name.substring(name.lastIndexOf("/") + 1);
			}
			
			if (testName(name)) {
				Date date = LogSearchUtil.getFileDate(name, ze.getTime());
				if (testDate(date)) {
					results.add(new Result(file, date, ze.getName()));
					hasResult = true;
				}
			}
		}
		
		if (hasResult) {
			zipFiles.put(file, zf);
			
		} else {
			ZipFile.closeQuietly(zf);
		}
	}

	private void scan () {
		System.out.println("scan");
		
		for (int n = 0; n < results.size(); n++) {
			listener.searchUpdate("scanning " + (n + 1) + " of " + results.size());
			Result result = results.get(n);
			
			try {
				// only scan if required
				if (text != null && text.length() > 0) {
					if (result.entry != null) {
						ZipFile zf = zipFiles.get(result.file);
						try (InputStream is = zf.getInputStream(zf.getEntry(result.entry))) {
							scanInputStream(result.lines, result.entry, is);
						}
						
					} else {
						try (InputStream is = new FileInputStream(result.file)) {
							scanInputStream(result.lines, result.file.getName(), is);
						}
					}
				}
				
				listener.searchResult(result);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			LogSearchUtil.sleep();
		}
	}
	
	private void scanInputStream (final List<Line> lines, String name, InputStream is) throws Exception {
		System.out.println("scan " + name);
		
		try (InputStream is2 = LogSearchUtil.optionallyDecompress(name, new BufferedInputStream(is))) {
			scanInputStream2(lines, is2);
		}
			
		System.gc();
	}
	
	private void scanInputStream2 (final List<Line> lines, final InputStream is) throws Exception {
		try (CountingInputStream cis = new CountingInputStream(is)) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
				int number = 0;
				String line;
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
	}
	
}
