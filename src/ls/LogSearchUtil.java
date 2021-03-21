package ls;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.*;
import java.math.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class LogSearchUtil {
	
	public static final ScheduledExecutorService EX = Executors.newScheduledThreadPool(1);
	
	public static final long MS_IN_DAY = 1000L * 60 * 60 * 24;
	public static final long NS_IN_S = 1_000_000_000L;
	public static final long CONFIRM_SIZE = 50_000_000L;
	
	public static final String ALL_RANGE = "All Files";
	public static final String DATE_RANGE = "Date Range";
	public static final String AGE_RANGE = "Max Age";
	public static final String COUNT_RANGE = "Max Count";
	
	private static final String OSX_OPEN = "/usr/bin/open";
	private static final Map<Object, File> TEMP_FILES = new TreeMap<>();
	private static final String[] PREFIX = new String[] {
			"B", "KB", "MB", "GB", "TB", "PB", "EB"
	};
	
	public static void execOpen (File editor, File file, int lineno) throws Exception {
		String[] args;
		if (editor.getPath().equals(OSX_OPEN)) {
			args = new String[] { editor.getAbsolutePath(), "-t", file.getAbsolutePath() };
		} else if (editor.getName().equals("notepad++.exe")) {
			args = new String[] { editor.getAbsolutePath(), "-n" + lineno, file.getAbsolutePath() };
		} else {
			args = new String[] { editor.getAbsolutePath(), file.getAbsolutePath() };
		}
		System.out.println("exec " + Arrays.toString(args));
		Runtime.getRuntime().exec(args);
	}

	public static boolean isCompressed (String name) {
		return GzipUtils.isCompressedFilename(name) || BZip2Utils.isCompressedFilename(name) || XZUtils.isCompressedFilename(name);
	}

	/** decompress if compressed, otherwise return same input stream */
	public static InputStream uncompressedInputStream (String name, BufferedInputStream is) throws Exception {
		if (isCompressed(name)) {
			CompressorStreamFactory f = new CompressorStreamFactory(true);
			return f.createCompressorInputStream(is);
		} else {
			return is;
		}
	}
	
	/**
	 * get original file if not compressed, otherwise optionally decompress to temp file
	 */
	public static File getOrCreateFile (final Result result, boolean create) throws Exception {
		File f = TEMP_FILES.get(result.key());
		if (f == null) {
			if (result.entry != null) {
				if (create) {
					TEMP_FILES.put(result.key(), f = entryToFile(result, createTempFile(result)));
				}
			} else if (isCompressed(result.file.getName())) {
				if (create) {
					TEMP_FILES.put(result.key(), f = nonEntryToFile(result, createTempFile(result)));
				}
			} else {
				f = result.file;
			}
		}
		return f;
	}

	/**
	 * get uncompressed file for result
	 */
	public static void copyToFile (final Result result, final File destFile) throws Exception {
		if (result.entry != null) {
			entryToFile(result, destFile);
		} else if (isCompressed(result.file.getName())) {
			nonEntryToFile(result, destFile);
		} else {
			FileUtils.copyFile(result.file, destFile);
		}
	}

	private static File nonEntryToFile (final Result result, final File destFile) throws Exception {
		try (InputStream is = uncompressedInputStream(result.file.getName(), new BufferedInputStream(new FileInputStream(result.file)))) {
			return writeFile(destFile, is);
		}
	}

	private static File entryToFile (Result result, File destFile) throws Exception {
		try (ZipFile zf = new ZipFile(result.file)) {
			ZipArchiveEntry ze = zf.getEntry(result.entry);
			try (InputStream is = uncompressedInputStream(result.entry, new BufferedInputStream(zf.getInputStream(ze)))) {
				return writeFile(destFile, is);
			}
		}
	}

	public static File createTempFile (Result result) throws Exception {
		File file = File.createTempFile(result.suggestedFileName(), null);
		file.deleteOnExit();
		return file;
	}

	public static File writeFile (File file, InputStream is) throws Exception {
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
			IOUtils.copy(is, os);
		}
		return file;
	}

	/** default editor or null */
	public static File defaultEditor () {
		String os = System.getProperty("os.name").toLowerCase();
		File f = null;

		if (os.startsWith("mac os x")) {
			f = new File(OSX_OPEN);

		} else if (os.startsWith("windows")) {
			Map<String, String> env = System.getenv();
			String pf = env.get("ProgramFiles");
			String pf86 = env.get("ProgramFiles(x86)");
			String windir = env.get("windir");
			f = new File(pf + "\\Notepad++\\notepad++.exe");
			if (!f.exists()) {
				f = new File(pf86 + "\\Notepad++\\notepad++.exe");
			}
			if (!f.exists()) {
				f = new File(windir + "\\notepad.exe");
			}
			if (!f.exists()) {
				f = new File(windir + "\\system32\\notepad.exe");
			}
		}

		return f != null && f.exists() ? f : null;
	}

	@Deprecated
	public static JPanel inlineFlowPanel (Object... comps) {
		FlowLayout fl = new FlowLayout();
		fl.setVgap(0);
		fl.setHgap(0);
		JPanel p = new JPanel(fl);
		boolean rest = false;
		for (Object c : comps) {
			if (rest) {
				JPanel q = new JPanel();
				q.setPreferredSize(new Dimension(5,5));
				p.add(q);
			}
			p.add(comp(c));
			rest = true;
		}
		return p;
	}
	
	private static JComponent comp (Object o) {
		if (o instanceof String) {
			return new JLabel((String)o);
		} else if (o instanceof JComponent) {
			return (JComponent)o;
		} else {
			throw new RuntimeException(String.valueOf(o));
		}
	}

	public static JPanel flowPanel (Object... comps) {
		JPanel p = new JPanel();
		for (Object c : comps) {
			p.add(comp(c));
		}
		return p;
	}
	
	public static JPanel boxPanel (Object... comps) {
		JPanel p = new JPanel(null);
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		for (Object c : comps) {
			p.add(comp(c));
		}
		return p;
	}
	
	public static String formatSize (long l) {
		if (l > 0) {
			// can't take log of 0...
			int p = (int) (Math.log10(l) / 3);
			BigDecimal s = new BigDecimal(l).round(new MathContext(3)).scaleByPowerOfTen(-3*p);
			return NumberFormat.getNumberInstance().format(s) + PREFIX[p];
		} else {
			return l + "B";
		}
	}
	
	/**
	 * format duration string
	 */
	public static String formatTime (int t) {
		Duration dur = Duration.ofSeconds(t);
		long d = dur.toDays(), h = dur.toHours() % 24, m = dur.toMinutes() % 60, s = dur.getSeconds() % 60;
		if (d > 0) {
			return String.format("%dd %dh %dm %ds", d, h, m, s);
		} else if (h > 0) {
			return String.format("%dh %dm %ds", h, m, s);
		} else if (m > 0) {
			return String.format("%dm %ds", m, s);
		} else {
			return String.format("%ds", s);
		}
	}
	
	/**
	 * sleep if system property configured
	 */
	public static void testSleep () {
		String maxstr = System.getProperty("ls.sleep");
		if (maxstr != null && maxstr.length() > 0) {
			try {
				Thread.sleep(Integer.parseInt(maxstr));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * get default charset items
	 */
	public static Vector<ComboItem> charsets () {
		Vector<ComboItem> v = new Vector<>();
		Charset dcs = Charset.defaultCharset();
		v.add(new ComboItem(dcs, dcs.name()));
		for (Charset cs : new Charset[] { StandardCharsets.US_ASCII, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8, StandardCharsets.UTF_16,
				StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE }) {
			if (!cs.name().equals(dcs.name())) {
				v.add(new ComboItem(cs, cs.name()));
			}
		}
		Collections.sort(v);
		return v;
	}
	
	private LogSearchUtil() {
		//
	}
}
