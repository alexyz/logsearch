package ls;

public class CachedFile {
	
	/** gzipped data */
	public final byte[] data;
	/** original file length */
	public final long len;
	public long accessedNs;
	
	public CachedFile() {
		this(null,0);
	}
	
	public CachedFile(byte[] data, long len) {
		this.data = data;
		this.len = len;
	}
	
	@Override
	public String toString () {
		Long pc = data != null ? (100 * data.length) / len : null;
		return String.format("%s[data=%s len=%d pc=%s]", getClass().getSimpleName(), data != null ? data.length : null, len, pc);
	}
	
}
