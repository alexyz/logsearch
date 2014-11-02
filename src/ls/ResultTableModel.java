package ls;

import java.text.DateFormat;
import java.util.*;

import javax.swing.table.AbstractTableModel;

public class ResultTableModel extends AbstractTableModel {
	
	private final List<Result> list = new ArrayList<>();
	
	public void clear () {
		list.clear();
		fireTableDataChanged();
	}
	
	public void add (Result fd) {
		list.add(fd);
		Collections.sort(list);
		fireTableDataChanged();
	}
	
	public Result getResult (int row) {
		return list.get(row);
	}
	
	@Override
	public int getRowCount () {
		return list.size();
	}
	
	@Override
	public int getColumnCount () {
		return 3;
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0:
				return "Name";
			case 1:
				return "Date";
			case 2:
				return "Matches";
		}
		return null;
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		Result fd = list.get(row);
		switch (col) {
			case 0:
				return fd.name;
			case 1:
				return DateFormat.getDateTimeInstance().format(fd.date);
			case 2:
				return fd.lines.size() != 0 ? fd.lines.size() : "";
		}
		return null;
	}
	
}
