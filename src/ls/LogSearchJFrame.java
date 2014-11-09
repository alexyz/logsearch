package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

public class LogSearchJFrame extends JFrame implements SearchListener {
	
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
	
	private volatile SearchThread searchThread;
	
	private File editor;
	
	public LogSearchJFrame () {
		super(TITLE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		initComponents();
		loadProperties();
		initListeners();
		setPreferredSize(new Dimension(800, 600));
		pack();
	}
	
	private void loadProperties () {
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
	
	private void saveProperties () {
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
	
	private void initListeners () {
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
				if (searchThread != null) {
					searchThread.running = false;
				}
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
				TextJDialog d = new TextJDialog(this, "Preview", sb.toString());
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
				
				if (result.entry != null) {
					file = LogSearchUtil.unzip(result.file, result.entry);
					
				} else {
					file = LogSearchUtil.optionallyDecompress(result.file);
				}
				
				LogSearchUtil.open(editor, file);
			}
		}
	}

	private void search () {
		System.out.println("search");
		
		if (searchThread != null) {
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
		
		saveProperties();
		
		searchThread = new SearchThread(this, dir, startDate, null, name, text);
		searchThread.start();
	}
	
	private void initComponents () {
		dirField.setColumns(15);
		nameField.setColumns(15);
		searchField.setColumns(15);
		
		JPanel northPanel1 = new JPanel();
		northPanel1.add(new JLabel("Directory"));
		northPanel1.add(dirField);
		northPanel1.add(dirButton);
		northPanel1.add(new JLabel("Name Contains"));
		northPanel1.add(nameField);
		
		{
			Calendar cal = new GregorianCalendar();
			int y = cal.get(Calendar.YEAR);
			int m = cal.get(Calendar.MONTH);
			int d = cal.get(Calendar.DATE);
			GregorianCalendar startCal = new GregorianCalendar(y, m, d);
			startCal.add(Calendar.DATE, -7);
			Date dstart = startCal.getTime();
			Date dmax = new GregorianCalendar(y + 1, m, d).getTime();
			Date dmin = new GregorianCalendar(y - 1, m, d).getTime();
			startSpinner.setModel(new SpinnerDateModel(dstart, dmin, dmax, Calendar.DATE));
			startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, ((SimpleDateFormat)DateFormat.getDateInstance()).toPattern()));
		}
		
		JPanel northPanel2 = new JPanel();
		northPanel2.add(new JLabel("File Contains"));
		northPanel2.add(searchField);
		northPanel2.add(new JLabel("Start Date"));
		northPanel2.add(startSpinner);
		northPanel2.add(startButton);
		northPanel2.add(stopButton);
		northPanel2.add(showAllButton);
		
		JScrollPane tableScroller = new JScrollPane(table);
		
		JPanel southPanel = new JPanel();
		southPanel.add(previewButton);
		southPanel.add(editorLabel);
		southPanel.add(editorButton);
		southPanel.add(openButton);
		
		JPanel northPanel = new JPanel(new GridLayout(2, 1));
		northPanel.add(northPanel1);
		northPanel.add(northPanel2);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(northPanel, BorderLayout.NORTH);
		contentPanel.add(tableScroller, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);

		setContentPane(contentPanel);
	}

	@Override
	public void searchResult (final Result fd) {
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
	
	@Override
	public void searchUpdate (final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				if (msg != null && msg.length() > 0) {
					setTitle(TITLE + " [" + msg + "]");
				} else {
					setTitle(TITLE);
				}
			}
		});
	}

	@Override
	public void searchComplete (final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				setTitle(TITLE);
				JOptionPane.showMessageDialog(LogSearchJFrame.this, msg, "Search Completed", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		searchThread = null;
	}
}
