package ls;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.io.input.CountingInputStream;

import static ls.LogSearchUtil.*;

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

	private long totalCount;

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
			listener.searchComplete(new SearchCompleteEvent(results.size(), ts, totalCount));

		} catch (Exception e) {
			e.printStackTrace();
			listener.searchError(e.toString());

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
						final ZipArchiveEntry zae = zf.getEntry(result.entry);
						if (zf.canReadEntryData(zae)) {
							try (InputStream is = zf.getInputStream(zae)) {
								final int i = scanInputStream(result, result.entry, is);
								if (i > 0) {
									result.matches = Integer.valueOf(i);
								}
							}
						} else {
							result.matches = "Cannot read " + ZipMethod.getMethodByCode(zae.getMethod());
						}

					} else {
						try (InputStream is = new FileInputStream(result.file)) {
							int i = scanInputStream(result, result.file.getName(), is);
							if (i > 0) {
								result.matches = Integer.valueOf(i);
							}
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

	private int scanInputStream (final Result result, String name, InputStream is) throws Exception {
		System.out.println("scan " + name);

		try (InputStream is2 = uncompressedInputStream(name, new BufferedInputStream(is))) {
			return scanInputStream2(result, is2);
		} finally {
			System.gc();
		}
	}

	private int scanInputStream2 (final Result result, final InputStream is) throws Exception {
		final List<String> backward = new ArrayList<>();
		int forward = 0;
		int matches = 0;

		LineCountingInputStream lcis;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(lcis = new LineCountingInputStream(is)))) {
			int lineno = 1;
			String line;

			while (running && (line = br.readLine()) != null) {
				if (forward > 0) {
					result.lines.put(lineno, line);
					forward--;
				}

				if (testLine(line)) {
					matches++;
					result.offsets.add(lcis.lines.get(lineno - 1));
					for (int n = 0; n < backward.size(); n++) {
						// backward = [l-3] [l-2] [l-1]
						result.lines.put(lineno - 1 - n, backward.get(backward.size() - 1 - n));
					}
					result.lines.put(lineno, line);
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
		
		result.size = lcis.count;
		totalCount += lcis.count;

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
