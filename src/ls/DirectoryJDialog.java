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
		String ud = System.getProperty("user.dir");
		String uh = System.getProperty("user.home");
		DirectoryJDialog d = new DirectoryJDialog(null, "Title", Arrays.asList(ud, uh));
		d.setVisible(true);
		System.exit(0);
	}
	
	private final DefaultListModel<File> model = new DefaultListModel<>();
	private final JList<File> list = new JList<>(model);
	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JButton removeButton = new JButton("Remove");
	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");
	private boolean ok;
	
	public DirectoryJDialog (Frame frame, String title, List<String> dirs) {
		super(frame, title);
		for (String d : dirs) {
			model.addElement(new File(d).getAbsoluteFile());
		}
		setContentPane(init());
		setModal(true);
		setPreferredSize(new Dimension(480, 320));
		pack();
		setLocationRelativeTo(frame);
	}

	private JPanel init () {
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int opt = fc.showOpenDialog(DirectoryJDialog.this);
				if (opt == JFileChooser.APPROVE_OPTION) {
					model.addElement(fc.getSelectedFile().getAbsoluteFile());
					list.setSelectedIndex(model.getSize() - 1);
				}
			}
		});
		
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				int i = list.getSelectedIndex();
				if (i >= 0) {
					JFileChooser fc = new JFileChooser(model.get(i));
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int opt = fc.showOpenDialog(DirectoryJDialog.this);
					if (opt == JFileChooser.APPROVE_OPTION) {
						model.set(i, fc.getSelectedFile().getAbsoluteFile());
						repaint();
					}
				}
			}
		});
		
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				int i = list.getSelectedIndex();
				if (i >= 0) {
					model.remove(i);
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
		
		JScrollPane scoller = new JScrollPane(list);
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
	
	public List<String> getDirs() {
		ArrayList<String> l = new ArrayList<String>();
		for (int n = 0; n < model.getSize(); n++) {
			l.add(model.get(n).getAbsolutePath());
		}
		return l;
	}
}
