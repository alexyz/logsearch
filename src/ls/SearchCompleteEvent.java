package ls;

public class SearchCompleteEvent {
	/** total files matching name */
	public final int found;
	/** time in seconds */
	public final double time;
	/** total bytes scanned */
	public final long size;
	/** total lines matched */
	public final int matches;
	/** total files scanned */
	public final int scanned;

	public SearchCompleteEvent(int found, int scanned, double time, long size, int matches) {
		this.found = found;
		this.scanned = scanned;
		this.time = time;
		this.size = size;
		this.matches = matches;
	}

	@Override
	public String toString () {
		return "SearchCompleteEvent[found=" + found + " scanned=" + scanned + " time=" + time + " size=" + size + " matches=" + matches + "]";
	}

}
