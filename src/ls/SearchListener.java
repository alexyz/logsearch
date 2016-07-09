package ls;

public interface SearchListener {
	/** search result created (search continues) */
	public void searchResult (Result fd);
	/** search completed normally */
	public void searchComplete (SearchCompleteEvent e);
	/** search update (search continues) */
	public void searchUpdate (String msg);
	/** search completed with error */
	public void searchError (String msg);
}
