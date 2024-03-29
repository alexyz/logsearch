package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class DirectoryJDialog extends JDialog {
	
	private final JButton addButton = new JButton("Add...");
	private final JButton editButton = new JButton("Edit...");
	private final JButton removeButton = new JButton("Remove...");
	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");
	private final JTable dirTable = new JTable(new DirectoryTableModel());
	
	private boolean ok;
	
	public DirectoryJDialog(Frame frame, String title) {
		super(frame, title);
		setContentPane(init());
		setModal(true);
		setPreferredSize(new Dimension(480, 320));
		pack();
		setLocationRelativeTo(frame);
	}
	
	public void addDirs (List<DirOpt> dirs) {
		final DirectoryTableModel m = (DirectoryTableModel) dirTable.getModel();
		m.addAll(dirs);
	}
	
	private JPanel init () {
		addButton.addActionListener(e -> addDir());
		editButton.addActionListener(e -> editDir());
		removeButton.addActionListener(e -> removeDir());
		okButton.addActionListener(e -> close(true));
		cancelButton.addActionListener(e -> close(false));
		dirTable.getColumnModel().getColumn(0).setMaxWidth(100);
		dirTable.getColumnModel().getColumn(2).setMaxWidth(100);
		
		JScrollPane scoller = new JScrollPane(dirTable);
		scoller.setBorder(new TitledBorder("Directories"));
		
		JPanel buttonPanel1 = new JPanel();
		buttonPanel1.add(addButton);
		buttonPanel1.add(editButton);
		buttonPanel1.add(removeButton);
		
		JPanel buttonPanel2 = new JPanel();
		buttonPanel2.add(okButton);
		buttonPanel2.add(cancelButton);
		
		JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
		buttonPanel.add(buttonPanel1);
		buttonPanel.add(buttonPanel2);
		
		JPanel s = new JPanel(new BorderLayout());
		s.add(scoller, BorderLayout.CENTER);
		s.add(buttonPanel, BorderLayout.SOUTH);
		return s;
	}
	
	private void addDir () {
		JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int opt = fc.showOpenDialog(DirectoryJDialog.this);
		if (opt == JFileChooser.APPROVE_OPTION) {
			final DirectoryTableModel m = (DirectoryTableModel) dirTable.getModel();
			m.add(new DirOpt(fc.getSelectedFile().getAbsoluteFile(), true, true));
			dirTable.getSelectionModel().setSelectionInterval(m.getRowCount(), m.getRowCount());
		}
	}
	
	public boolean isOk () {
		return ok;
	}
	
	public List<DirOpt> getDirs () {
		return ((DirectoryTableModel) dirTable.getModel()).getDirs();
	}

	private void editDir () {
		int i = dirTable.getSelectedRow();
		if (i >= 0) {
			DirOpt d = ((DirectoryTableModel) dirTable.getModel()).getDir(i);
			JFileChooser fc = new JFileChooser(d.dir);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int opt = fc.showOpenDialog(DirectoryJDialog.this);
			if (opt == JFileChooser.APPROVE_OPTION) {
				((DirectoryTableModel) dirTable.getModel()).setDir(i, new DirOpt(fc.getSelectedFile().getAbsoluteFile(), d.enabled, d.recursive));
				repaint();
			}
		}
	}

	private void removeDir () {
		int i = dirTable.getSelectedRow();
		if (i >= 0) {
			DirectoryTableModel m = (DirectoryTableModel) dirTable.getModel();
			if (JOptionPane.showConfirmDialog(this, 
					"Remove " + m.getDir(i).dir.getName() + "?",
					"Remove",
					JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				m.remove(i);
			}
		}
	}

	private void close (boolean ok) {
		this.ok = ok;
		setVisible(false);
	}
	
}
