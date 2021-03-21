package ls;

public interface SearchListener {
	/** search result created (search continues) */
	void searchResult (Result fd);
	/** search completed normally */
	void searchComplete (SearchCompleteEvent e);
	/** search update (search continues) */
	void searchUpdate (String msg);
	/** search completed with error */
	void searchError (String msg);
}
