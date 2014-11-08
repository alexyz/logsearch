package ls;

import java.text.DateFormat;
import java.util.*;

import javax.swing.table.AbstractTableModel;

public class ResultTableModel extends AbstractTableModel {
	
	private final List<Result> results = new ArrayList<>();
	private final List<Result> matchingResults = new ArrayList<>();
	private boolean showAll;
	
	private List<Result> getResults() {
		return showAll ? results : matchingResults;
	}
	
	public void clear () {
		results.clear();
		matchingResults.clear();
		fireTableDataChanged();
	}
	
	public void add (Result fd) {
		results.add(fd);
		Collections.sort(results);
		if (fd.lines.size() > 0) {
			matchingResults.add(fd);
			Collections.sort(matchingResults);
		}
		fireTableDataChanged();
	}
	
	public Result getResult (int row) {
		return getResults().get(row);
	}
	
	@Override
	public int getRowCount () {
		return getResults().size();
	}
	
	@Override
	public int getColumnCount () {
		return 3;
	}
	
	public int getRow (Result r) {
		List<Result> results = getResults();
		for (int n = 0; n < results.size(); n++) {
			if (results.get(n).equals(r)) {
				return n;
			}
		}
		return -1;
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0:
				return "Date";
			case 1:
				return "Name";
			case 2:
				return "Matches";
		}
		return null;
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		Result fd = getResults().get(row);
		switch (col) {
			case 0:
				return DateFormat.getDateTimeInstance().format(fd.date);
			case 1:
				return fd.name;
			case 2:
				return fd.lines.size() != 0 ? fd.lines.size() : "";
		}
		return null;
	}

	public boolean isShowAll () {
		return showAll;
	}

	public void setShowAll (boolean showAll) {
		this.showAll = showAll;
		fireTableDataChanged();
	}
	
}
