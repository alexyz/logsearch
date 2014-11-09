package ls;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.*;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.io.IOUtils;

public class LogSearchUtil {
	private static final String OPEN = "/usr/bin/open";

	public static void open (File editor, File file) throws Exception {
		if (editor.getPath().equals(OPEN)) {
			Runtime.getRuntime().exec(new String[] { editor.getAbsolutePath(), "-t", file.getAbsolutePath() });
		} else {
			Runtime.getRuntime().exec(new String[] { editor.getAbsolutePath(), file.getAbsolutePath() });
		}
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
	public static File unzip (File zipFile, String entry) throws Exception {
		System.out.println("unzip " + zipFile + ", " + entry);
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipArchiveEntry ze = zf.getEntry(entry);
			try (InputStream is = optionallyDecompress(entry, new BufferedInputStream(zf.getInputStream(ze)))) {
				return createTempFile(zipFile.getName() + "." + ze.getName(), is);
			}
		}
	}
	
	/** uncompress to temp file if compressed, otherwise return same file */
	public static File optionallyDecompress (File file) throws Exception {
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

	public static Date getFileDate (String fileName, long fileTime) {
		Pattern datePat = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
		Date date = null;
		if (fileName.length() > 0) {
			Matcher mat = datePat.matcher(fileName);
			if (mat.find()) {
				String dateStr = mat.group(1);
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				try {
					date = df.parse(dateStr);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (date == null && fileTime > 0) {
			date = new Date(fileTime);
		}
		return date;
	}
	
	public static void sleep() {
		String s = System.getProperty("ls.slow");
		if (s != null && s.length() > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private LogSearchUtil () {
		//
	}
}
