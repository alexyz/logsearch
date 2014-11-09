package ls;

import java.io.File;
import java.util.*;

public class Result implements Comparable<Result> {
	public final List<Line> lines = new ArrayList<>(0);
	public final String name;
	public final Date date;
	public final File file;
	public final String entry;
	
	public Result (File file, Date date, String entry) {
		this.name = file.getName() + (entry != null ? ":" + entry : "");
		this.file = file;
		this.date = date;
		this.entry = entry;
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