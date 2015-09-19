package ls;

import java.awt.FlowLayout;
import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.io.IOUtils;

public class LogSearchUtil {
	private static final String OPEN = "/usr/bin/open";

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
	public static InputStream optionallyDecompress (String name, BufferedInputStream is) throws Exception {
		if (isCompressed(name)) {
			CompressorStreamFactory f = new CompressorStreamFactory();
			f.setDecompressConcatenated(true);
			return f.createCompressorInputStream(is);
			
		} else {
			return is;
		}
	}
	
	/** unzip to temp file */
	public static File unzipFile (File zipFile, String entry) throws Exception {
		System.out.println("unzip " + zipFile + ", " + entry);
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipArchiveEntry ze = zf.getEntry(entry);
			try (InputStream is = optionallyDecompress(entry, new BufferedInputStream(zf.getInputStream(ze)))) {
				return createTempFile(zipFile.getName() + "." + ze.getName(), is);
			}
		}
	}
	
	/** uncompress to temp file if compressed, otherwise return same file */
	public static File decompressFile (File file) throws Exception {
		System.out.println("optionally decompress " + file);
		if (isCompressed(file.getName())) {
			try (InputStream is = optionallyDecompress(file.getName(), new BufferedInputStream(new FileInputStream(file)))) {
				return createTempFile(file.getName(), is);
			}
			
		} else {
			return file;
		}
	}
	
	public static File createTempFile (String tmpname, InputStream is) throws Exception {
		File file = File.createTempFile(tmpname + ".", null);
		file.deleteOnExit();
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
	
	public static JPanel panel(JComponent... comps) {
		FlowLayout fl = new FlowLayout();
		fl.setVgap(0);
		JPanel p = new JPanel(fl);
		for (JComponent c : comps) {
			p.add(c);
		}
		return p;
	}
	
	private LogSearchUtil () {
		//
	}
}
