package ls;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;

public class DirectoryTableModel extends AbstractTableModel {
	
	private final List<File> dirs = new ArrayList<>();
	private final List<Boolean> enabled = new ArrayList<>();
	
	public Set<File> getDirs (boolean enabled) {
		Set<File> dirs = new TreeSet<>();
		for (int n = 0; n < this.dirs.size(); n++) {
			if (this.enabled.get(n).booleanValue() == enabled) {
				dirs.add(this.dirs.get(n));
			}
		}
		return dirs;
	}
	
	public void add (File dir, boolean en) {
		dirs.add(dir);
		enabled.add(en);
		fireTableDataChanged();
	}
	
	public void remove (int row) {
		dirs.remove(row);
		enabled.remove(row);
		fireTableDataChanged();
	}
	
	public void update (int row, File dir) {
		dirs.set(row, dir);
		fireTableDataChanged();
	}
	
	public File getDir (int row) {
		return dirs.get(row);
	}
	
	@Override
	public int getRowCount () {
		return dirs.size();
	}
	
	@Override
	public int getColumnCount () {
		return 2;
	}
	
	@Override
	public String getColumnName (int column) {
		switch (column) {
			case 0:
				return "Enabled";
			case 1:
				return "Dir";
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public Class<?> getColumnClass (int columnIndex) {
		switch (columnIndex) {
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public boolean isCellEditable (int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return true;
			case 1:
				return false;
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public Object getValueAt (int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return enabled.get(rowIndex);
			case 1:
				return dirs.get(rowIndex);
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void setValueAt (Object aValue, int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				enabled.set(rowIndex, (Boolean) aValue);
				break;
			default:
				throw new RuntimeException();
		}
	}
	
}