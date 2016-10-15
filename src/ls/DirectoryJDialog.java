package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class DirectoryJDialog extends JDialog {
	
	public static void main (String[] args) {
		File ud = new File(System.getProperty("user.dir"));
		File uh = new File(System.getProperty("user.home"));
		DirectoryJDialog d = new DirectoryJDialog(null, "Title");
		d.addDirs(Arrays.asList(ud, uh), true);
		d.setVisible(true);
		System.exit(0);
	}
	
	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JButton removeButton = new JButton("Remove");
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
	
	public void addDirs (Collection<File> dirs, boolean en) {
		final DirectoryTableModel m = (DirectoryTableModel) dirTable.getModel();
		for (File d : dirs) {
			m.add(d.getAbsoluteFile(), en);
		}
	}
	
	private JPanel init () {
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int opt = fc.showOpenDialog(DirectoryJDialog.this);
				if (opt == JFileChooser.APPROVE_OPTION) {
					final DirectoryTableModel m = (DirectoryTableModel) dirTable.getModel();
					m.add(fc.getSelectedFile().getAbsoluteFile(), true);
					dirTable.getSelectionModel().setSelectionInterval(m.getRowCount(), m.getRowCount());
				}
			}
		});
		
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				int i = dirTable.getSelectedRow();
				if (i >= 0) {
					JFileChooser fc = new JFileChooser(((DirectoryTableModel) dirTable.getModel()).getDir(i));
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int opt = fc.showOpenDialog(DirectoryJDialog.this);
					if (opt == JFileChooser.APPROVE_OPTION) {
						((DirectoryTableModel) dirTable.getModel()).update(i, fc.getSelectedFile().getAbsoluteFile());
						repaint();
					}
				}
			}
		});
		
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				int i = dirTable.getSelectedRow();
				if (i >= 0) {
					((DirectoryTableModel) dirTable.getModel()).remove(i);
				}
			}
		});
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				ok = true;
				setVisible(false);
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				setVisible(false);
			}
		});
		
		dirTable.getColumnModel().getColumn(0).setMaxWidth(100);
		
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
	
	public boolean isOk () {
		return ok;
	}
	
	public Set<File> getDirs (boolean enabled) {
		return ((DirectoryTableModel) dirTable.getModel()).getDirs(enabled);
	}
}
