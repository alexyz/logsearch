package ls;

import java.io.File;
import java.util.*;

public class Result implements Comparable<Result> {
	public final Date date;
	public final String name;
	public final File file;
	public final String entry;
	public final List<Line> lines;
	
	public Result (String name, Date date, File file, String entry, List<Line> lines) {
		this.name = name;
		this.file = file;
		this.date = date;
		this.entry = entry;
		this.lines = lines;
	}
	
	@Override
	public int compareTo (Result o) {
		int c = date.compareTo(o.date);
		if (c == 0) {
			return name.compareToIgnoreCase(o.name);
		}
		return -c;
	}
	
}