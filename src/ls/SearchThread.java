package ls;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.*;

import static ls.LogSearchUtil.*;

public class SearchThread extends Thread {

	private static final int MAX_MATCHES = 1000;

	public volatile boolean running;
	public Set<File> dirs;
	public Date startDate;
	public Date endDate;
	public int contextLinesBefore;
	public int contextLinesAfter;
	public FileDater dateParser;
	public Charset charset;

	private final List<Result> results = new ArrayList<>();
	private final Map<File,ZipFile> zipFiles = new TreeMap<>();
	private final SearchListener listener;
	
	private String filenameLower;
	private String text;
	private boolean ignoreCase;
	private Pattern pattern;

	private long totalCount;

	public SearchThread (SearchListener listener) {
		super("SearchThread");
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		this.listener = listener;
	}
	
	public void setFilename(String name) {
		this.filenameLower = name.toLowerCase();
	}
	
	public void setText(String text, boolean regex, boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
		this.text = ignoreCase ? text.toUpperCase() : text;
		this.pattern = regex && text.length() > 0 ? Pattern.compile(text, ignoreCase ? Pattern.CASE_INSENSITIVE : 0) : null;
	}

	@Override
	public void run () {
		try {
			System.out.println("run");
			if (startDate.compareTo(endDate) >= 0) {
				throw new Exception("Start date equal to or after end date");
			}
			if (filenameLower.length() == 0) {
				throw new Exception("No file name filter");
			}
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
		try (BufferedReader br = new BufferedReader(new InputStreamReader(lcis = new LineCountingInputStream(is), charset))) {
			int lineno = 1;
			String line;

			while (running && (line = br.readLine()) != null) {
				if (forward > 0) {
					result.lines.put(lineno, line);
					forward--;
				}

				if (testLine(line)) {
					matches++;
					// FIXME this doesn't work well...
					if (lcis.lines.size() >= lineno) {
						result.offsets.add(lcis.lines.get(lineno - 1));
					}
					for (int n = 0; n < backward.size(); n++) {
						// backward = [l-3] [l-2] [l-1]
						result.lines.put(lineno - 1 - n, backward.get(backward.size() - 1 - n));
					}
					result.lines.put(lineno, line);
					forward = contextLinesAfter;
				}

				if (contextLinesBefore > 0) {
					if (backward.size() >= contextLinesBefore) {
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
