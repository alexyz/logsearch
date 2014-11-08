package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

public class LogSearchJFrame extends JFrame {
	
	
	private static final String TITLE = "LogSearch";
	private static final LogSearchJFrame instance = new LogSearchJFrame();
	
	public static void main (String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		instance.setVisible(true);
	}
	
	private final JTextField dirField = new JTextField();
	private final JButton dirButton = new JButton("...");
	private final JTextField nameField = new JTextField();
	private final JTextField searchField = new JTextField();
	private final JSpinner startSpinner = new JSpinner();
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JButton openButton = new JButton("Open");
	private final ResultTableModel tableModel = new ResultTableModel();
	private final JTable table = new JTable(tableModel);
	private final JButton editorButton = new JButton("Editor...");
	private final JLabel editorLabel = new JLabel();
	private final JButton previewButton = new JButton("Preview");
	private final JToggleButton showAllButton = new JToggleButton("Show All");
	
	private File editor;
	
	public LogSearchJFrame () {
		super(TITLE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		load();
		listeners();
		setContentPane(createContentPane());
		setPreferredSize(new Dimension(800, 600));
		pack();
	}
	
	private void load () {
		try {
			File f = new File(System.getProperty("user.home") + File.separator + "LogSearch.properties");
			Properties p = new Properties();
			if (f.exists()) {
				try (InputStream is = new FileInputStream(f)) {
					p.load(is);
				}
			}
			dirField.setText(p.getProperty("dir", System.getProperty("user.dir")));
			nameField.setText(p.getProperty("name", "server.log"));
			searchField.setText(p.getProperty("search", "a"));
			//ageSpinner.setValue(Integer.parseInt(p.getProperty("age", "7")));
			editor = new File(p.getProperty("editor", LogSearchUtil.defaultEditor().getAbsolutePath()));
			if (!editor.exists()) {
				editor = null;
			}
			editorLabel.setText(editor != null ? editor.getName() : "no editor");
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Load Properties", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	private void save () {
		Properties p = new Properties();
		p.setProperty("dir", dirField.getText());
		p.setProperty("name", nameField.getText());
		p.setProperty("search", searchField.getText());
		p.setProperty("editor", editor != null ? editor.getAbsolutePath() : "");
		//p.setProperty("age", String.valueOf(ageSpinner.getValue()));
		File f = new File(System.getProperty("user.home") + File.separator + "LogSearch.properties");
		try (OutputStream os = new FileOutputStream(f)) {
			p.store(os, null);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Save Properties", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public void searchAdd (final Result fd) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				int r = table.getSelectedRow();
				Result result = null;
				if (r >= 0) {
					result = tableModel.getResult(r);
				}
				tableModel.add(fd);
				int r2 = tableModel.getRow(result);
				if (r2 >= 0) {
					table.getSelectionModel().setSelectionInterval(r2, r2);
				}
			}
		});
	}
	
	private void listeners () {
		dirButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(dirField.getText()));
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int o = fc.showOpenDialog(LogSearchJFrame.this);
				if (o == JFileChooser.APPROVE_OPTION) {
					dirField.setText(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});
		
		editorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				if (editor != null) {
					fc.setSelectedFile(editor);
				}
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setFileFilter(new FileFilter() {
					
					@Override
					public String getDescription () {
						return "Executables";
					}
					
					@Override
					public boolean accept (File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".exe");
					}
				});
				int o = fc.showOpenDialog(LogSearchJFrame.this);
				if (o == JFileChooser.APPROVE_OPTION) {
					editor = fc.getSelectedFile();
					editorLabel.setText(editor.getName());
				}
			}
		});
		
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				search();
			}
		});
		
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				FindThread.running = false;
			}
		});
		
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent ae) {
				try {
					open();
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(LogSearchJFrame.this, e.toString(), "Open", JOptionPane.ERROR_MESSAGE);
				}
			}
			
		});
		
		previewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent ae) {
				preview();
			}
			
		});
		
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked (MouseEvent e) {
				if (e.getClickCount() == 2) {
					preview();
				}
			}
		});
		
		showAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				int r = table.getSelectedRow();
				Result result = null;
				if (r >= 0) {
					result = tableModel.getResult(r);
				}
				tableModel.setShowAll(showAllButton.isSelected());
				int r2 = tableModel.getRow(result);
				if (r2 >= 0) {
					table.getSelectionModel().setSelectionInterval(r2, r2);
				}
			}
		});
	}
	
	private void preview () {
		System.out.println("preview");
		int row = table.getSelectedRow();
		if (row >= 0) {
			Result result = tableModel.getResult(row);
			if (result.lines.size() > 0) {
				StringBuffer sb = new StringBuffer();
				for (Line line : result.lines) {
					sb.append("Line " + line.number + "\n");
					sb.append(line.line + "\n");
				}
				TextJDialog d = new TextJDialog(this, "Lines", sb.toString());
				d.setVisible(true);
			}
		}
	}
	
	private void open () throws Exception {
		System.out.println("open");
		int r = table.getSelectedRow();
		if (r >= 0) {
			if (editor != null) {
				Result result = tableModel.getResult(r);
				File file;
				if (result.file.getName().toLowerCase().endsWith(".gz")) {
					file = LogSearchUtil.ungzip(result.file);
				} else if (result.file.getName().toLowerCase().endsWith(".zip")) {
					file = LogSearchUtil.unzip(result.file, result.entry);
				} else {
					file = result.file;
				}
				LogSearchUtil.open(editor, file);
			}
		}
	}

	private void search () {
		System.out.println("search");
		
		if (FindThread.running) {
			System.out.println("already running");
			return;
		}
		
		tableModel.clear();
		
		final String text = searchField.getText();
		final File dir = new File(dirField.getText());
		final Date startDate = (Date) startSpinner.getValue();
		final String name = nameField.getText();
		if (name.length() == 0) {
			System.out.println("no name filter");
			return;
		}
		
		save();
		
		FindThread ft = new FindThread(this, dir, startDate, name, text);
		FindThread.running = true;
		ft.start();
	}
	
	private JPanel createContentPane () {
		dirField.setColumns(15);
		nameField.setColumns(15);
		searchField.setColumns(15);
		
		JPanel p = new JPanel();
		p.add(new JLabel("Directory"));
		p.add(dirField);
		p.add(dirButton);
		p.add(new JLabel("Name Contains"));
		p.add(nameField);
		
		{
			Calendar cal = new GregorianCalendar();
			int y = cal.get(Calendar.YEAR);
			int m = cal.get(Calendar.MONTH);
			int d = cal.get(Calendar.DATE);
			GregorianCalendar startCal = new GregorianCalendar(y, m, d);
			startCal.add(Calendar.DATE, -14);
			Date dstart = startCal.getTime();
			Date dmax = new GregorianCalendar(y + 1, m, d).getTime();
			Date dmin = new GregorianCalendar(y - 1, m, d).getTime();
			startSpinner.setModel(new SpinnerDateModel(dstart, dmin, dmax, Calendar.DATE));
			startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, ((SimpleDateFormat)DateFormat.getDateInstance()).toPattern()));
		}
		
		JPanel p3 = new JPanel();
		p3.add(new JLabel("File Contains"));
		p3.add(searchField);
		p3.add(new JLabel("Start Date"));
		p3.add(startSpinner);
		p3.add(startButton);
		p3.add(stopButton);
		p3.add(showAllButton);
		
		JScrollPane sp = new JScrollPane(table);
		
		JPanel p5 = new JPanel();
		p5.add(previewButton);
		p5.add(editorLabel);
		p5.add(editorButton);
		p5.add(openButton);
		
		JPanel p4 = new JPanel(new GridLayout(2, 1));
		p4.add(p);
		p4.add(p3);
		
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(p4, BorderLayout.NORTH);
		p2.add(sp, BorderLayout.CENTER);
		p2.add(p5, BorderLayout.SOUTH);
		return p2;
	}

	public void searchUpdate (final int i) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				if (i >= 0) {
					setTitle(TITLE + " [" + i + "]");
				} else {
					setTitle(TITLE);
				}
			}
		});
	}

	public void searchEnd (String msg) {
		JOptionPane.showMessageDialog(this, msg, "Search Completed", JOptionPane.INFORMATION_MESSAGE);
	}
}
