package ls;

import java.util.Date;

/** date and source of date for file */
public class FileDate {
	/** date source if any */
	public final String source;
	/** file date if any */
	public final Date date;
	
	public FileDate(String source, Date date) {
		this.source = source;
		this.date = date;
	}
	
	@Override
	public String toString () {
		return "FileDate[date=" + date + " source=" + source + "]";
	}
	
}
