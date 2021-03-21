package ls;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.io.input.CountingInputStream;

public class SearchThread extends Thread {
	
	public volatile boolean running = true;
	
	public SearchListener listener;
	public List<DirOpt> dirs;
	public Date startDate;
	public Date endDate;
	public int contextLinesBefore;
	public int contextLinesAfter;
	public FileDater dateParser;
	public Charset charset;
	public String filenameLower;
	public String text;
	public String exText;
	public boolean ignoreCase;
	public boolean regex;
	public boolean cacheUncompressed;
	public int maxFiles;
	public int maxMatches;
	
	private final List<Result> results = new ArrayList<>();
	private final Map<File,ZipFile> zipFiles = new TreeMap<>();
	private final byte[] buffer = new byte[65536];
	
	// variables assigned in run
	
	/** pattern to find, null if not regex */
	private Pattern pattern;
	/** pattern to exclude, null if not regex */
	private Pattern exPattern;
	/** text depending on ignoreCase, null if no text */
	private String textOpt;
	/** exText depending on ignoreCase, null if no exText */
	private String exTextOpt;
	/** bytes scanned and cached */
	private long totalSize;
	private int totalMatches;
	private int totalFilesFound;
	private String scanMsg;
	private long startMs;
	
	public SearchThread () {
		super("SearchThread");
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
	}
	
	@Override
	public void run () {
		try {
			System.out.println("run");
			long startNs = System.nanoTime();
			init();
			find();
			scan();
			long endNs = System.nanoTime() - startNs;
			double timeS = ((double)endNs) / LogSearchUtil.NS_IN_S;
			listener.searchComplete(new SearchCompleteEvent(totalFilesFound, results.size(), timeS, totalSize, totalMatches));
			
		} catch (Exception e) {
			e.printStackTrace(System.out);
			listener.searchError(e.toString());
			
		} finally {
			for (ZipFile zf : zipFiles.values()) {
				ZipFile.closeQuietly(zf);
			}
			running = false;
		}
	}
	
	private void init () throws Exception {
		startMs = System.currentTimeMillis();
		if (startDate != null && endDate != null && startDate.compareTo(endDate) >= 0) {
			throw new Exception("Start date equal to or after end date");
		}
		if (filenameLower.length() == 0) {
			throw new Exception("No file name filter");
		}
		if (text == null || exText == null) {
			throw new Exception("No text");
		}
		if (dirs == null || dirs.size() == 0) {
			throw new Exception("No dirs");
		}
		if (text.length() > 0) {
			if (regex) {
				pattern = Pattern.compile(text, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
			} else {
				textOpt = ignoreCase ? text.toUpperCase() : text;
			}
		}
		if (exText.length() > 0) {
			if (regex) {	
				exPattern = Pattern.compile(exText, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
			} else {
				exTextOpt = ignoreCase ? exText.toUpperCase() : exText;
			}
		}
		if (textOpt != null && textOpt.length() > 0 && exTextOpt != null && exTextOpt.length() > 0 && textOpt.contains(exTextOpt)) {
			throw new Exception("Exclude text includes text");
		}
	}
	
	private void find () {
		listener.searchUpdate("finding");
		for (DirOpt dir : dirs) {
			checkRunning();
			findDir(dir.dir, dir.recursive);
		}
		Collections.sort(results);
		if (maxFiles > 0) {
			while (results.size() > maxFiles) {
				results.remove(results.size() - 1);
			}
		}
	}
	
	private boolean testName (String name) {
		return name.length() > 0 && name.toLowerCase().contains(this.filenameLower);
	}
	
	private boolean testDate (Date date) {
		return date != null && (startDate == null || date.compareTo(startDate) >= 0) && (endDate == null || date.compareTo(endDate) < 0);
	}
	
	private void findDir (File dir, boolean recursive) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				checkRunning();
				
				if (file.isFile()) {
					if (file.getName().toLowerCase().endsWith(".zip")) {
						try {
							findZip(file);
						} catch (IOException e) {
							System.out.println("could not open " + file + ": " + e);
						}
						
					} else {
						findFile(file);
					}
					
				} else if (file.isDirectory() && recursive) {
					findDir(file, true);
				}
			}
		} else {
			System.out.println("could not list files in " + dir);
		}
	}
	
	private void findFile (File file) {
		if (testName(file.getName())) {
			FileDate fd = dateParser.getFileDate(file.lastModified(), file.getName());
			if (testDate(fd.date)) {
				results.add(new Result(file, fd, null, file.length()));
			}
			totalFilesFound++;
		}
	}
	
	private void findZip (final File file) throws IOException {
		System.out.println("find zip " + file);
		
		// don't close until scan finished
		ZipFile zf = new ZipFile(file);
		boolean hasResult = false;
		Enumeration<ZipArchiveEntry> e = zf.getEntries();
		
		while (e.hasMoreElements()) {
			checkRunning();
			ZipArchiveEntry ze = e.nextElement();
			String name = ze.getName();
			if (name.contains("/")) {
				name = name.substring(name.lastIndexOf("/") + 1);
			}
			if (testName(name)) {
				FileDate fd = dateParser.getFileDate(ze.getTime(), name);
				if (testDate(fd.date)) {
					results.add(new Result(file, fd, ze.getName(), ze.getCompressedSize()));
					hasResult = true;
				}
				totalFilesFound++;
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
			checkRunning();
			scanMsg = "scanning " + (n + 1) + " of " + results.size();
			Result result = results.get(n);
			scan2(result);
			listener.searchResult(result);
			LogSearchUtil.testSleep();
		}
	}
	
	/** does not throw Exception except RuntimeException */
	private void scan2 (Result result) {
		try {
			// only scan if required
			if (textOpt != null || pattern != null || exTextOpt != null || exPattern != null) {
				if (result.entry != null) {
					listener.searchUpdate(scanMsg);
					ZipFile zf = zipFiles.get(result.file);
					ZipArchiveEntry zae = zf.getEntry(result.entry);
					if (zf.canReadEntryData(zae)) {
						try (InputStream is = zf.getInputStream(zae)) {
							checkRunning();
							result.matches = scan3(result, is);
						}
					} else {
						result.error = "Cannot read " + ZipMethod.getMethodByCode(zae.getMethod());
					}
					
				} else {
					try (InputStream is = openIS(result.file)) {
						checkRunning();
						result.matches = scan3(result, is);
					}
				}
				
				System.gc();
			} else {
				result.error = "*";
			}
			
		} catch (RuntimeException e) {
			throw e;
			
		} catch (Exception e) {
			System.out.println("could not scan " + result);
			e.printStackTrace(System.out);
			result.error = e.toString();
		}
	}
	
	/**
	 * open the file, possible adding and retrieving from cache
	 */
	private InputStream openIS (File file) throws IOException {
		CachedFile cf = FileCache.get(file);
		long modMs = startMs - 1000L*60*60;
		
		if (cf != null) {
			// already cached - check if valid
			if (cf.len != file.length() || file.lastModified() >= modMs) {
				System.out.println("removing from cache: " + cf);
				cf = FileCache.put(file, new CachedFile());
			}
			
		} else if (cacheUncompressed && testCache(file, modMs)) {
			// can cache
			listener.searchUpdate(scanMsg + " (cache)");
			byte[] a = compress(file);
			// require improvement
			if (a.length <= (file.length() / 4)) {
				cf = FileCache.put(file, new CachedFile(a, file.length()));
			} else {
				cf = FileCache.put(file, new CachedFile());
			}
			System.out.println("added to cache: " + cf);
		}
		
		if (cf != null && cf.data != null) {
			listener.searchUpdate(scanMsg);
			return new GzipCompressorInputStream(new ByteArrayInputStream(cf.data));
			
		} else {
			listener.searchUpdate(scanMsg);
			return new FileInputStream(file);
		}
	}
	
	/** true if file can be cached */
	private boolean testCache (File file, long modMs) {
		return !LogSearchUtil.isCompressed(file.getName()) && file.lastModified() < modMs && file.length() > 1_000_000 && FileCache.sumOk();
	}
	
	private byte[] compress (File file) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			GzipParameters p = new GzipParameters();
			p.setCompressionLevel(1);
			try (GzipCompressorOutputStream gos = new GzipCompressorOutputStream(bos, p)) {
				try (FileInputStream fis = new FileInputStream(file)) {
					copyIS(fis, gos);
				}
			}
			return bos.toByteArray();
		}
	}
	
	private void copyIS (InputStream is, OutputStream os) throws IOException {
		long total = 0;
		int i;
		while ((i = is.read(buffer)) > 0) {
			checkRunning();
			os.write(buffer, 0, i);
			total += i;
		}
		totalSize += total;
	}
	
	private int scan3 (final Result result, final InputStream is) throws Exception {
		final List<String> backward = new ArrayList<>();
		int forward = 0;
		int matches = 0;
		
		try (InputStream uis = LogSearchUtil.uncompressedInputStream(result.name, new BufferedInputStream(is))) {
			try (CountingInputStream cis = new CountingInputStream(uis)) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(cis, charset))) {
					int lineno = 1;
					String line;
					
					while ((line = br.readLine()) != null) {
						checkRunning();
						
						if (forward > 0) {
							result.lines.put(lineno, line);
							forward--;
						}
						
						if (testLine(line)) {
							matches++;
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
						
						if (maxMatches > 0 && matches >= maxMatches) {
							break;
						}
						
						lineno++;
					}
				}
				
				totalSize += cis.getByteCount();
				totalMatches += matches;
			}
		}
		
		return matches;
	}
	
	private boolean testLine (String line) {
		boolean found = false;
		String lineOpt = null;
		
		if (pattern != null) {
			found = pattern.matcher(line).find();
		} else if (textOpt != null) {
			lineOpt = ignoreCase ? line.toUpperCase() : line;
			found = lineOpt.contains(textOpt);
		} else {
			found = true;
		}
		
		if (found) {
			if (exPattern != null) {
				found = !exPattern.matcher(line).find();
			} else if (exTextOpt != null) {
				lineOpt = ignoreCase && lineOpt == null ? line.toUpperCase() : lineOpt;
				found = !lineOpt.contains(exTextOpt);
			}
		}
		
		return found;
	}
	
	private void checkRunning() {
		if (!running) {
			throw new RuntimeException("stopped");
		}
	}
	
}
