package ls;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;

public class LogSearchUtil {
	private static final String OPEN = "/usr/bin/open";

	public static void open (File editor, File file) throws Exception {
		if (editor.getPath().equals(OPEN)) {
			Runtime.getRuntime().exec(new String[] { editor.getAbsolutePath(), "-t", file.getAbsolutePath() });
		} else {
			Runtime.getRuntime().exec(new String[] { editor.getAbsolutePath(), file.getAbsolutePath() });
		}
	}
	
	public static File unzip (File zipfile, String entry) throws Exception {
		System.out.println("unzip " + zipfile + ", " + entry);
		try (ZipFile zf = new ZipFile(zipfile)) {
			ZipEntry ze = zf.getEntry(entry);
			try (InputStream is = zf.getInputStream(ze)) {
				return copy(is, zipfile.getName() + "." + ze.getName());
			}
		}
	}
	
	public static File ungzip (File gzfile) throws Exception {
		System.out.println("ungzip " + gzfile);
		try (InputStream is = new GZIPInputStream(new FileInputStream(gzfile))) {
			return copy(is, gzfile.getName());
		}
	}
	
	public static File copy (InputStream is, String tmpname) throws Exception {
		File file = File.createTempFile(tmpname + ".", null);
		file.deleteOnExit();
		try (OutputStream os = new FileOutputStream(file)) {
			byte[] buf = new byte[65536];
			int l;
			while ((l = is.read(buf)) != -1) {
				os.write(buf, 0, l);
			}
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

	public static Date getFileNameDate (String name) {
		Pattern datePat = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
		Date date = null;
		if (name.length() > 0) {
			Matcher mat = datePat.matcher(name);
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
		return date;
	}
	
	public static void slow() {
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
