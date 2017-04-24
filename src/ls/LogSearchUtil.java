package ls;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.*;
import java.math.*;
import java.text.NumberFormat;
import java.util.*;

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

	public static final long MS_IN_DAY = 1000 * 60 * 60 * 24;
	
	public static final String STARTDATE_PREF = "startdate";
	public static final String ENDDATE_PREF = "enddate";
	public static final String PARSEDATE_PREF = "parsedate";
	public static final String CONTEXT_BEFORE_PREF = "contextbefore";
	public static final String CONTEXT_AFTER_PREF = "contextafter";
	public static final String CASE_PREF = "case";
	public static final String START_OR_AGE_PREF = "start";
	public static final String EDITOR_PREF = "editor";
	public static final String AGE_PREF = "age";
	public static final String SEARCH_PREF = "search";
	public static final String NAME_PREF = "name";
	public static final String DIR_PREF = "dir";
	public static final String DIS_DIR_PREF = "disdir";
	public static final String REGEX_PREF = "regex";
	public static final String EXCLUDE_PREF = "exclude";
	public static final String CD_PREF = "cd";
	
	private static final String OPEN = "/usr/bin/open";

	private static Map<Object, File> TEMP_FILES = new TreeMap<>();
	
	private static final String[] PREFIX = new String[] {
			"B", "KB", "MB", "GB", "TB", "PB", "EB"
	};

	public static void execOpen (File editor, File file, int lineno) throws Exception {
		String[] args;
		if (editor.getPath().equals(OPEN)) {
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
	 * get uncompressed file for result (cached)
	 */
	public static File toTempFile (final Result result) throws Exception {
		File file;
		if (result.entry != null) {
			file = TEMP_FILES.get(result.key());
			if (file == null) {
				TEMP_FILES.put(result.key(), file = entryToFile(result, createTempFile(result)));
			}
		} else if (isCompressed(result.file.getName())) {
			file = TEMP_FILES.get(result.key());
			if (file == null) {
				TEMP_FILES.put(result.key(), file = decompressToFile(result, createTempFile(result)));
			}
		} else {
			file = result.file;
		}
		return file;
	}

	/**
	 * get uncompressed file for result
	 */
	public static void toFile (final Result result, final File destFile) throws Exception {
		if (result.entry != null) {
			entryToFile(result, destFile);
		} else if (isCompressed(result.file.getName())) {
			decompressToFile(result, destFile);
		} else {
			FileUtils.copyFile(result.file, destFile);
		}
	}

	private static File decompressToFile (final Result result, final File destFile) throws Exception {
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
		File file = File.createTempFile(result.tempName(), null);
		file.deleteOnExit();
		return file;
	}

	public static File writeFile (File file, InputStream is) throws Exception {
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
			IOUtils.copy(is, os);
		}
		return file;
	}

	public static File defaultEditor () {
		String os = System.getProperty("os.name").toLowerCase();
		File f = null;

		if (os.startsWith("mac os x")) {
			f = new File(OPEN);

		} else if (os.startsWith("windows")) {
			Map<String, String> env = System.getenv();
			String pf86 = env.get("ProgramFiles(x86)");
			String windir = env.get("windir");
			f = new File(pf86 + "\\Notepad++\\notepad++.exe");
			if (!f.exists()) {
				f = new File(windir + "\\notepad.exe");
			}
			if (!f.exists()) {
				f = new File(windir + "\\system32\\notepad.exe");
			}
		}

		if (f != null && !f.exists()) {
			f = null;
		}

		return f;
	}

	public static void stringToDirs (Collection<File> dirs, String dirStr) {
		StringTokenizer t = new StringTokenizer(dirStr, File.pathSeparator);
		while (t.hasMoreTokens()) {
			File d = new File(t.nextToken());
			if (d.isDirectory()) {
				dirs.add(d);
			}
		}
	}

	public static String dirsToString (Collection<File> dirs) {
		StringBuilder dirSb = new StringBuilder();
		for (File dir : dirs) {
			if (dirSb.length() > 0) {
				dirSb.append(File.pathSeparator);
			}
			dirSb.append(dir.getAbsolutePath());
		}
		return dirSb.toString();
	}

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
			if (c instanceof String) {
				c = new JLabel((String)c);
			}
			if (c instanceof JComponent) {
				p.add((JComponent)c);
			} else {
				throw new RuntimeException(String.valueOf(c));
			}
			rest = true;
		}
		return p;
	}

	public static JPanel flowPanel (JComponent... comps) {
		JPanel p = new JPanel();
		for (JComponent c : comps) {
			p.add(c);
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
			return String.valueOf(l) + "B";
		}
	}
	
	private LogSearchUtil() {
		//
	}
}
