package ls;

import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.TableModel;

public class ResultsJTable extends JTable {
	
	public ResultsJTable (TableModel dm) {
		super(dm);
	}
	
	@Override
	public String getToolTipText(MouseEvent e) {
		int r = rowAtPoint(e.getPoint());
		int c = columnAtPoint(e.getPoint());
		if (r >= 0 && c >= 0) {
			return ((ResultTableModel)getModel()).getToolTipAt(r, c);
		}
		return null;
	}
}
