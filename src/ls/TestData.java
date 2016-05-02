package ls;

import java.io.*;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.zip.*;

/**
 * generate test data (plain text, gz and zip)
 */
public class TestData {
	
	private static final Random r = new Random();

	public static void main (String[] args) throws Exception {
		System.out.println("press enter for test data");
		while (System.in.read() != '\n');
		new File("logs").mkdirs();
		try (PrintWriter pw = new PrintWriter(new FileWriter("logs/server.log"))) {
			write(pw);
		}
		try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new File("logs/example.zip"))) {
			ZipArchiveEntry ze = new ZipArchiveEntry("server.log.2");
		    zos.putArchiveEntry(ze);
		    PrintWriter pw = new PrintWriter(new OutputStreamWriter(zos));
		    write(pw);
		    zos.closeArchiveEntry();
		}
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File("logs/server.log.3.gz")))))) {
			write(pw);
		}
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
