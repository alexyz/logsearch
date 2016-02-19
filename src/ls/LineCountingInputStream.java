package ls;

import java.io.*;
import java.util.*;

public class LineCountingInputStream extends InputStream {
	
	public final ArrayList<Long> lines = new ArrayList<>();
	public long count;
	
	private final InputStream is;
	
	public LineCountingInputStream(InputStream is) {
		this.is = is;
		lines.add(new Long(0));
	}
	
	@Override
	public int read () throws IOException {
		int x = is.read();
		if (x >= 0) {
			count++;
		}
		if (x == '\n') {
			lines.add(count);
		}
		return x;
	}
	
	@Override
	public int read (byte[] b, int off, int len) throws IOException {
		int x = is.read(b, off, len);
		for (int n = 0; n < x; n++) {
			count++;
			if (b[n] == '\n') {
				lines.add(count);
			}
		}
		return x;
	}
	
	@Override
	public void close () throws IOException {
		is.close();
	}
	
	@Override
	public int available () throws IOException {
		return is.available();
	}
	
	@Override
	public long skip (long n) throws IOException {
		throw new IOException();
	}
	
	@Override
	public boolean markSupported () {
		return false;
	}
	
	@Override
	public synchronized void mark (int readlimit) {
		//
	}
	
	@Override
	public synchronized void reset () throws IOException {
		throw new IOException();
	}
	
}
