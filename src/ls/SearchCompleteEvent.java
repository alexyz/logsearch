package ls;

public class SearchCompleteEvent {
	public final int results;
	public final double seconds;
	public final long bytes;

	public SearchCompleteEvent(int results, double seconds, long bytes) {
		this.results = results;
		this.seconds = seconds;
		this.bytes = bytes;
	}
}
