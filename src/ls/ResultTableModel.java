package ls;

import java.text.DateFormat;
import java.util.*;

import javax.swing.table.AbstractTableModel;

public class ResultTableModel extends AbstractTableModel {
	
	/** results list, this is always sorted */
	private final List<Result> results = new ArrayList<>();
	private final List<Result> matchingResults = new ArrayList<>();
	private boolean showAll;
	
	private List<Result> getCurrentResults() {
		return showAll ? results : matchingResults;
	}
	
	public List<Result> getResults() {
		return results;
	}
	
	public void clear () {
		results.clear();
		matchingResults.clear();
		fireTableDataChanged();
	}
	
	public void add (Result fd) {
		results.add(fd);
		Collections.sort(results);
		if (fd.lines.size() > 0 || fd.matches > 0 || fd.error != null) {
			matchingResults.add(fd);
			Collections.sort(matchingResults);
		}
		fireTableDataChanged();
	}
	
	public Result getResult (int row) {
		return getCurrentResults().get(row);
	}
	
	@Override
	public int getRowCount () {
		return getCurrentResults().size();
	}
	
	@Override
	public int getColumnCount () {
		return 4;
	}
	
	public int getRow (Result r) {
		List<Result> results = getCurrentResults();
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
				return "Size";
			case 3:
				return "Matches";
		}
		return null;
	}
	
	public String getToolTipAt (int row, int col) {
		Result r = getCurrentResults().get(row);
		switch (col) {
			case 0:
				return r.fileDate != null ? r.fileDate.source : null;
			case 1:
				return r.file.getAbsolutePath() + (r.entry != null ? ": " + r.entry : "");
			default:
				return null;
		}
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		Result r = getCurrentResults().get(row);
		switch (col) {
			case 0:
				return r.fileDate.date != null ? DateFormat.getDateTimeInstance().format(r.fileDate.date) : null;
			case 1:
				return r.name();
			case 2:
				return LogSearchUtil.formatSize(r.pSize);
			case 3:
				return r.matches > 0 ? String.valueOf(r.matches) : r.error;
			default:
				return null;
		}
	}
	
	public boolean isShowAll () {
		return showAll;
	}
	
	public void setShowAll (boolean showAll) {
		this.showAll = showAll;
		fireTableDataChanged();
	}
	
}
