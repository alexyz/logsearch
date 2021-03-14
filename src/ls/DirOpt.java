package ls;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.File;
import java.util.*;

/** naturally sort by name, enabled, recursive */
public class DirOpt implements Comparable<DirOpt> {
	
	/** sort by recursive then path length */
	public static Comparator<DirOpt> LEN_CMP = new Comparator<DirOpt>() {
		@Override
		public int compare (DirOpt d1, DirOpt d2) {
			CompareToBuilder cb = new CompareToBuilder();
			cb.append(d1.recursive, d2.recursive);
			cb.append(d1.dir.length(), d2.dir.length());
			return cb.toComparison();
		}
	};
	
	public static List<DirOpt> stringToDirs (String dirStr, boolean enabled, boolean nonrecur) {
		List<DirOpt> list = new ArrayList<>();
		StringTokenizer t = new StringTokenizer(dirStr, File.pathSeparator);
		while (t.hasMoreTokens()) {
			File d = new File(t.nextToken());
			if (d.isDirectory()) {
				list.add(new DirOpt(d.getAbsoluteFile(),enabled,nonrecur));
			}
		}
		return list;
	}
	
	public static String dirsToString (Collection<DirOpt> dirs, boolean en, boolean re) {
		StringBuilder dirSb = new StringBuilder();
		for (DirOpt dir : dirs) {
			if (dir.enabled == en && dir.recursive == re) {
				if (dirSb.length() > 0) {
					dirSb.append(File.pathSeparator);
				}
				dirSb.append(dir.dir.getAbsolutePath());
			}
		}
		return dirSb.toString();
	}
	
	public final File dir;
	public final boolean enabled, recursive;
	
	public DirOpt (File dir, boolean enabled, boolean recursive) {
		this.dir = dir;
		this.enabled = enabled;
		this.recursive = recursive;
	}
	
	@Override
	public boolean equals (Object o) {
		if (o instanceof DirOpt) {
			DirOpt d = (DirOpt) o;
			return enabled == d.enabled && recursive == d.recursive && dir.equals(d.dir);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode () {
		return Objects.hash(dir, enabled, recursive);
	}
	
	@Override
	public String toString () {
		return "DirOpt[" + dir + " enabled=" + enabled + " recursive=" + recursive + "]";
	}
	
	@Override
	public int compareTo (DirOpt d) {
		CompareToBuilder cb = new CompareToBuilder();
		cb.append(dir, d.dir);
		cb.append(enabled, d.enabled);
		cb.append(recursive, d.recursive);
		return cb.toComparison();
	}
}
