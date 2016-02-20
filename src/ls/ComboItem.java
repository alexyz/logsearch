package ls;

public class ComboItem {
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
}
