package ls;

import java.io.*;
import java.util.Random;

/**
 * generate test data
 */
public class TestData {
	
	private static final Random r = new Random();

	public static void main (String[] args) throws Exception {
		try (PrintWriter pw = new PrintWriter(new FileWriter("logs/server.log"))) {
			for (int n = 1; n < 1000000; n++) {
				pw.print(n + " ");
				int len = r.nextInt(160);
				for (int m = 0; m < len; m++) {
					pw.print((char) (r.nextInt(64) + 32));
				}
				pw.println();
			}
		}
		// TODO generate some zips/gz etc
	}

}
