package ls;

import java.io.*;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * generate test data (plain text, gz and zip)
 */
public class TestData {
	
	private static final Random r = new Random();

	public static void main (String[] args) throws Exception {
		System.out.println("press enter for test data");
		while (System.in.read() != '\n') ;
		File logDir = new File(args[0]);
		logDir.mkdirs();
		create(logDir);
	}
	
	public static void create(File dir) throws Exception {
		System.out.println("create " + dir);
		dir.mkdirs();
		File textFile = new File(dir, "server.log");
		File zipFile = new File(dir, "example.zip");
		File gzipFile = new File(dir, "server.log.3.gz");
		File bzipFile = new File(dir, "server.log.4.bz2");
		if (textFile.exists() || zipFile.exists() || gzipFile.exists() || bzipFile.exists()) {
			throw new Exception("files exist: " + dir);
		}
		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(textFile)))) {
			System.out.println("write " + textFile);
			write(pw);
		}
		try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(zipFile)) {
			System.out.println("write " + zipFile);
			ZipArchiveEntry ze = new ZipArchiveEntry("server.log.2");
		    zos.putArchiveEntry(ze);
		    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(zos)))) {
				write(pw);
				pw.flush();
		    	zos.closeArchiveEntry();
			}
		}
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(gzipFile)))))) {
			System.out.println("write " + gzipFile);
			write(pw);
		}
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BZip2CompressorOutputStream(new BufferedOutputStream(new FileOutputStream(bzipFile)))))) {
			System.out.println("write " + bzipFile);
			write(pw);
		}
		System.out.println("done");
	}

	private static void write (PrintWriter pw) {
		for (int n = 1; n < 1000000; n++) {
			pw.print(n + " ");
			int len = r.nextInt(160);
			for (int m = 0; m < len; m++) {
				pw.print((char) (r.nextInt(64) + 32));
			}
			pw.println();
		}
		pw.flush();
	}

}
