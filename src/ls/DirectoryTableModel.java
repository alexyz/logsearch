package ls;

import java.util.*;

import javax.swing.table.AbstractTableModel;

public class DirectoryTableModel extends AbstractTableModel {
	
	private final List<DirOpt> dirs = new ArrayList<>();
	
	public List<DirOpt> getDirs () {
		return new ArrayList<>(dirs);
	}
	
	public void add (DirOpt dir) {
		dirs.add(dir);
		Collections.sort(dirs);
		fireTableDataChanged();
	}
	
	public void addAll (List<DirOpt> dirs) {
		this.dirs.addAll(dirs);
		Collections.sort(this.dirs);
		fireTableDataChanged();
	}
	
	public void remove (int row) {
		dirs.remove(row);
		fireTableDataChanged();
	}
	
	public void setDir (int row, DirOpt dir) {
		dirs.set(row, dir);
		Collections.sort(dirs);
		fireTableDataChanged();
	}
	
	public DirOpt getDir (int row) {
		return dirs.get(row);
	}
	
	@Override
	public int getRowCount () {
		return dirs.size();
	}
	
	@Override
	public int getColumnCount () {
		return 3;
	}
	
	@Override
	public String getColumnName (int column) {
		switch (column) {
			case 0:
				return "Enabled";
			case 1:
				return "Dir";
			case 2:
				return "Recursive";
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public Class<?> getColumnClass (int columnIndex) {
		switch (columnIndex) {
			case 0:
			case 2:
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
			case 2:
				return true;
			case 1:
				return false;
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		DirOpt d = dirs.get(row);
		switch (col) {
			case 0:
				return d.enabled;
			case 1:
				return d.dir;
			case 2:
				return d.recursive;
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void setValueAt (Object val, int row, int col) {
		DirOpt d = dirs.get(row);
		switch (col) {
			case 0:
				d = new DirOpt(d.dir, (Boolean) val, d.recursive);
			case 2:
				d = new DirOpt(d.dir, d.enabled, (Boolean) val);
				break;
			default:
				throw new RuntimeException();
		}
		dirs.set(row, d);
	}
	
}