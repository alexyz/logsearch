package ls;

public class ComboItem implements Comparable<ComboItem> {
	public final Object object;
	public final String name;
	
	public ComboItem(Object object, String name) {
		this.object = object;
		this.name = name;
	}
	
	@Override
	public String toString () {
		return name;
	}
	
	@Override
	public int compareTo (ComboItem o) {
		return name.compareTo(o.name);
	}
}
