package ls;

import java.io.File;
import java.util.*;

public class Result implements Comparable<Result> {
	/** map of line number to line */
	public final NavigableMap<Integer,String> lines = new TreeMap<>();
	public final NavigableSet<Long> offsets = new TreeSet<>();
	public final String name;
	public final Date date;
	public final File file;
	/** zip file entry name */
	public final String entry;
	// updated during search
	public Object matches;
	
	public Result (File file, Date date, String entry) {
		this.file = file;
		this.date = date;
		this.entry = entry;
		if (entry != null) {
			int i = entry.lastIndexOf("/");
			if (i >= 0 && i < entry.length()) {
				entry = entry.substring(i + 1);
			}
			this.name = entry;
		} else {
			this.name = file.getName();
		}
	}
	
	/** name of file plus name of zip file if any */
	public String name() {
		if (entry != null) {
			return name + " [" + file.getName() + "]";
		} else {
			return name;
		}
	}
	
	@Override
	public int compareTo (Result o) {
		int c = date.compareTo(o.date);
		if (c == 0) {
			c = name.compareToIgnoreCase(o.name);
		}
		return -c;
	}
	
}