package ls;

import java.io.File;
import java.util.*;

public class Result implements Comparable<Result> {
	public final Map<Integer,String> lines = new TreeMap<>();
	public final String name;
	public final Date date;
	public final File file;
	public final String entry;
	public int matches;
	
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
	
	@Override
	public int compareTo (Result o) {
		int c = date.compareTo(o.date);
		if (c == 0) {
			c = name.compareToIgnoreCase(o.name);
		}
		return -c;
	}
	
}