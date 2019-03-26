package ls;

import java.io.File;
import java.util.*;

import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 * results of a search in a particular file
 */
public class Result implements Comparable<Result> {
	/** map of line number to line */
	public final NavigableMap<Integer, String> lines = new TreeMap<>();
	/** name of the log file - file name or zip archive entry name */
	public final String name;
	public final FileDate fileDate;
	public final File file;
	/** zip file entry name */
	public final String entry;
	/** size on disk */
	public final long pSize;
	
	// updated during search
	/** number of lines matches */
	public int matches;
	/** message if no matches */
	public String error;
	
	public Result(File file, FileDate date, String entry, long pSize) {
		this.file = file;
		this.fileDate = date;
		this.entry = entry;
		this.pSize = pSize;
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
	public String suggestedFileName () {
		if (entry != null) {
			return file.getName() + "." + name;
		} else {
			return name;
		}
	}
	
	@Override
	public int compareTo (Result o) {
		CompareToBuilder cb = new CompareToBuilder();
		cb.append(o.fileDate.date, fileDate.date);
		cb.append(name.toLowerCase(), o.name.toLowerCase());
		return cb.toComparison();
	}
	
	@Override
	public String toString () {
		return "Result [lines=" + lines.size() + " name=" + name + " date=" + fileDate + " file=" + file + " entry=" + entry + " matches=" + matches + "]";
	}
	
}