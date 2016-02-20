package ls;

public interface SearchListener {
	public void searchResult (Result fd);
	public void searchComplete (SearchCompleteEvent e);
	public void searchUpdate (String msg);
	public void searchError (String msg);
}
