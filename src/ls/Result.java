package ls;

import java.io.File;
import java.util.*;

/**
 * results of a search in a particular file
 */
public class Result implements Comparable<Result> {
	/** map of line number to line */
	public final NavigableMap<Integer, String> lines = new TreeMap<>();
	/** name of the log file */
	public final String name;
	public final Date date;
	public final File file;
	/** zip file entry name */
	public final String entry;
	
	// updated during search
	/** match description (null for no match, or number of lines, or error etc) */
	public Object matches;

	public Result(File file, Date date, String entry) {
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

	/** human readable name of file plus name of zip file if any */
	public String name () {
		if (entry != null) {
			return name + " [" + file.getName() + "]";
		} else {
			return name;
		}
	}

	/** key representing result */
	public Object key () {
		if (entry != null) {
			return file.getAbsolutePath() + ":" + entry;
		} else {
			return file.getAbsolutePath();
		}
	}

	/** name of temp file */
	public String tempName () {
		if (entry != null) {
			return file.getName() + "." + name;
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

	@Override
	public String toString () {
		return "Result [lines=" + lines.size() + ", name=" + name + ", date=" + date + ", file=" + file + ", entry=" + entry + ", matches=" + matches + "]";
	}

}