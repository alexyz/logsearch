package ls;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.input.CountingInputStream;

public class SearchThread extends Thread {

	private static final int MAX_MATCHES = 1000;
	
	private static void sleep () {
		String s = System.getProperty("ls.slow");
		if (s != null && s.length() > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public volatile boolean running;
	
	private final List<Result> results = new ArrayList<>();
	private final Map<File,ZipFile> zipFiles = new TreeMap<>();
	private final SearchListener listener;
	private final Set<File> dirs;
	private final Date startDate;
	private final Date endDate;
	private final String filenameLower;
	private final String text;
	private final boolean ignoreCase;
	private final int contextLines;
	private final FileDater dateParser;
	private final Pattern pattern;
	
	private long bytes;

	public SearchThread (SearchListener listener, Set<File> dirs, Date startDate, Date endDate, String filename, String text, boolean regex, boolean ignoreCase, int contextLines, FileDater dateParser) {
		super("SearchThread");
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		this.ignoreCase = ignoreCase;
		this.endDate = endDate;
		this.listener = listener;
		this.dirs = dirs;
		this.startDate = startDate;
		this.contextLines = contextLines;
		this.dateParser = dateParser;
		this.filenameLower = filename.toLowerCase();
		this.text = ignoreCase ? text.toUpperCase() : text;
		this.pattern = regex && text.length() > 0 ? Pattern.compile(text, ignoreCase ? Pattern.CASE_INSENSITIVE : 0) : null;
	}
	
	@Override
	public void run () {
		try {
			System.out.println("run");
			running = true;
			listener.searchUpdate("finding");
			long t = System.nanoTime();
			for (File dir : dirs) {
				if (!running) {
					break;
				}
				findDir(dir);
			}
			Collections.sort(results);
			scan();
			long tns = System.nanoTime() - t;
			double ts = tns / 1_000_000_000.0;
			double mb = bytes / 1_000_000.0;
			listener.searchComplete(String.format("Files: %d  Megabytes: %.1f  Seconds: %.1f  MB/s: %.1f", results.size(), mb, ts, (mb/ts)));
			
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
		return name.toLowerCase().contains(this.filenameLower);
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
			Date date = dateParser.getFileDate(file.lastModified(), file.getName());
			if (testDate(date)) {
				results.add(new Result(file, date, null));
			}
		}
	}
	
	private void findZip (final File file) throws Exception {
		System.out.println("find zip " + file);
		
		// don't close until scan finished
		ZipFile zf = new ZipFile(file);
		boolean hasResult = false;
		
		Enumeration<ZipArchiveEntry> e = zf.getEntries();
		while (running && e.hasMoreElements()) {
			ZipArchiveEntry ze = e.nextElement();
			String name = ze.getName();
			if (name.contains("/")) {
				name = name.substring(name.lastIndexOf("/") + 1);
			}
			
			if (testName(name)) {
				Date date = dateParser.getFileDate(ze.getTime(), name);
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
		
		for (int n = 0; running && n < results.size(); n++) {
			listener.searchUpdate("scanning " + (n + 1) + " of " + results.size());
			Result result = results.get(n);
			
			try {
				// only scan if required
				if (text.length() > 0) {
					if (result.entry != null) {
						ZipFile zf = zipFiles.get(result.file);
						try (InputStream is = zf.getInputStream(zf.getEntry(result.entry))) {
							result.matches = scanInputStream(result.lines, result.entry, is);
						}
						
					} else {
						try (InputStream is = new FileInputStream(result.file)) {
							result.matches = scanInputStream(result.lines, result.file.getName(), is);
						}
					}
				}
				
				listener.searchResult(result);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			sleep();
		}
	}
	
	private int scanInputStream (final Map<Integer,String> lines, String name, InputStream is) throws Exception {
		System.out.println("scan " + name);
		
		try (InputStream is2 = LogSearchUtil.optionallyDecompress(name, new BufferedInputStream(is))) {
			return scanInputStream2(lines, is2);
			
		} finally {
			System.gc();
		}
	}
	
	private int scanInputStream2 (final Map<Integer,String> lines, final InputStream is) throws Exception {
		final List<String> backward = new ArrayList<>();
		int forward = 0;
		int matches = 0;
		
		try (CountingInputStream cis = new CountingInputStream(is)) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
				int lineno = 1;
				String line;
				
				while (running && (line = br.readLine()) != null) {
					if (forward > 0) {
						lines.put(lineno, line);
						forward--;
					}
					
					if (testLine(line)) {
						matches++;
						
						for (int n = 0; n < backward.size(); n++) {
							// backward = [l-3] [l-2] [l-1]
							lines.put(lineno - 1 - n, backward.get(backward.size() - 1 - n));
						}
						lines.put(lineno, line);
						forward = contextLines;
					}
					
					if (contextLines > 0) {
						if (backward.size() >= contextLines) {
							backward.remove(0);
						}
						backward.add(line);
					}
					
					if (matches >= MAX_MATCHES) {
						System.out.println("too many matches");
						break;
					}
					
					lineno++;
				}
			}
			bytes += cis.getByteCount();
		}
		
		return matches;
	}
	
	private boolean testLine (String line) {
		if (pattern != null) {
			return pattern.matcher(line).find();
		} else {
			String lineUpper = ignoreCase ? line.toUpperCase() : line;
			return lineUpper.contains(text);
		}
	}
	
}
