package ls;

public interface SearchListener {
	public void searchResult (Result fd);
	public void searchComplete (String msg);
	public void searchUpdate (String msg);
}
