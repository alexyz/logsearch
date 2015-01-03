package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

public class LogSearchJFrame extends JFrame implements SearchListener {
	
	private static final String CONTEXT_PREF = "context";
	private static final String CHARSET_PREF = "charset";
	private static final String CASE_PREF = "case";
	private static final String START_PREF = "start";
	private static final String EDITOR_PREF = "editor";
	private static final String AGE_PREF = "age";
	private static final String SEARCH_PREF = "search";
	private static final String NAME_PREF = "name";
	private static final String DIR_PREF = "dir";
	private static final String TITLE = "LogSearch";
	
	public static void main (String[] args) {
		LogSearchJFrame instance = new LogSearchJFrame();
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		instance.setVisible(true);
	}
	
	private final JLabel dirLabel = new JLabel();
	private final JButton dirButton = new JButton("Directories...");
	private final JTextField nameField = new JTextField();
	private final JTextField searchField = new JTextField();
	private final JSpinner startDateSpinner = new JSpinner();
	private final JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(7, 1, 999, 1));
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JButton openButton = new JButton("Open");
	private final ResultTableModel tableModel = new ResultTableModel();
	private final JTable table = new JTable(tableModel);
	private final JButton editorButton = new JButton("Editor...");
	private final JLabel editorLabel = new JLabel();
	private final JButton previewButton = new JButton("Preview");
	private final JToggleButton showAllButton = new JToggleButton("Show All");
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final JRadioButton startDateButton = new JRadioButton("Date");
	private final JRadioButton ageButton = new JRadioButton("Age");
	private final List<File> dirs = new ArrayList<>();
	private final JCheckBox ignoreCaseBox = new JCheckBox("Ignore Case");
	private final JTextField charsetField = new JTextField();
	private final JSpinner contextSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 10, 1));
	
	private volatile SearchThread searchThread;
	
	private File editor;
	
	public LogSearchJFrame () {
		super(TITLE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(800, 600));
		initComponents();
		initListeners();
		loadPrefs();
	}
	
	private void loadPrefs () {
		System.out.println("load prefs");
		String dirStr = prefs.get(DIR_PREF, System.getProperty("user.dir"));
		System.out.println("loaded dir pref: " + dirStr);
		StringTokenizer t = new StringTokenizer(dirStr, File.pathSeparator);
		while (t.hasMoreTokens()) {
			File d = new File(t.nextToken());
			if (d.isDirectory()) {
				dirs.add(d);
			}
		}
		dirLabel.setText(String.valueOf(dirs.size()));
		nameField.setText(prefs.get(NAME_PREF, "server.log"));
		searchField.setText(prefs.get(SEARCH_PREF, "a"));
		ageSpinner.setValue(prefs.getInt(AGE_PREF, 7));
		editor = new File(prefs.get(EDITOR_PREF, LogSearchUtil.defaultEditor().getAbsolutePath()));
		if (!editor.exists()) {
			editor = null;
		}
		editorLabel.setText(editor != null ? editor.getName() : "no editor");
		startDateButton.setSelected(prefs.getBoolean(START_PREF, true));
		ageButton.setSelected(!startDateButton.isSelected());
		ignoreCaseBox.setSelected(prefs.getBoolean(CASE_PREF, false));
		charsetField.setText(prefs.get(CHARSET_PREF, "UTF-8"));
		contextSpinner.setValue(prefs.getInt(CONTEXT_PREF, 3));
	}
	
	private void savePrefs () {
		System.out.println("save prefs");
		StringBuilder dirSb = new StringBuilder();
		for (File dir : dirs) {
			if (dirSb.length() > 0) {
				dirSb.append(File.pathSeparator);
			}
			dirSb.append(dir.getAbsolutePath());
		}
		System.out.println("save dir pref: " + dirSb);
		prefs.put(DIR_PREF, dirSb.toString());
		prefs.put(NAME_PREF, nameField.getText());
		prefs.put(SEARCH_PREF, searchField.getText());
		prefs.put(EDITOR_PREF, editor != null ? editor.getAbsolutePath() : "");
		prefs.putInt(AGE_PREF, (int) ageSpinner.getValue());
		prefs.putBoolean(START_PREF, startDateButton.isSelected());
		prefs.putBoolean(CASE_PREF, ignoreCaseBox.isSelected());
		prefs.put(CHARSET_PREF, charsetField.getText());
		prefs.putInt(CONTEXT_PREF, (int) contextSpinner.getValue());
		try {
			prefs.sync();
		} catch (BackingStoreException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Save Preferences", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	private void initListeners () {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent e) {
				System.out.println("window closing");
				savePrefs();
			}
		});
			
		dirButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				DirectoryJDialog d = new DirectoryJDialog(LogSearchJFrame.this, "Log Directories", dirs);
				d.setVisible(true);
				if (d.isOk()) {
					dirs.clear();
					dirs.addAll(d.getDirs());
					dirLabel.setText(String.valueOf(dirs.size()));
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
		
		ItemListener il = new ItemListener() {
			@Override
			public void itemStateChanged (ItemEvent e) {
				startDateSpinner.setEnabled(startDateButton.isSelected());
				ageSpinner.setEnabled(ageButton.isSelected());
			}
		};
		
		startDateButton.addItemListener(il);
		ageButton.addItemListener(il);
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
		System.out.println(SEARCH_PREF);
		
		if (searchThread != null) {
			System.out.println("already running");
			return;
		}
		
		if (dirs.size() == 0) {
			System.out.println("no dirs");
			return;
		}
		
		tableModel.clear();
		
		final String text = searchField.getText();
		
		final TreeSet<File> searchDirs = new TreeSet<>();
		// dirs is sorted, higher dirs are first
		for (File dir : dirs) {
			String dirStr = dir.getAbsolutePath() + File.separator;
			boolean add = true;
			for (File searchDir : searchDirs) {
				String searchDirStr = searchDir.getAbsolutePath() + File.separator;
				// is this directory already included
				if (dirStr.startsWith(searchDirStr)) {
					add = false;
					break;
				}
			}
			if (add) {
				searchDirs.add(dir);
			}
		}
		System.out.println("dirs to search: " + searchDirs);
		
		final Date startDate;
		if (startDateButton.isSelected()) {
			startDate = (Date) startDateSpinner.getValue();
		} else {
			Calendar cal = new GregorianCalendar();
			cal.add(Calendar.DATE, -(int)ageSpinner.getValue());
			startDate = cal.getTime();
		}
		final String name = nameField.getText();
		if (name.length() == 0) {
			System.out.println("no name filter");
			return;
		}
		
		final boolean ignoreCase = ignoreCaseBox.isSelected();
		
		Charset cs = Charset.forName(charsetField.getText());
		
		int context = (int) contextSpinner.getValue();
		
		savePrefs();
		
		searchThread = new SearchThread(this, searchDirs, startDate, null, name, text, ignoreCase, cs, context);
		searchThread.start();
	}
	
	private void initComponents () {
		nameField.setColumns(15);
		searchField.setColumns(15);
		charsetField.setColumns(10);
		
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
			startDateSpinner.setModel(new SpinnerDateModel(dstart, dmin, dmax, Calendar.DATE));
			startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, ((SimpleDateFormat)DateFormat.getDateInstance()).toPattern()));
		}
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(startDateButton);
		bg.add(ageButton);
		
		JPanel northPanel = new JPanel(new FlowLayout2());
		northPanel.add(dirLabel);
		northPanel.add(dirButton);
		northPanel.add(new JLabel("Name Contains"));
		northPanel.add(nameField);
		northPanel.add(new JLabel("File Contains"));
		northPanel.add(searchField);
		northPanel.add(ignoreCaseBox);
		northPanel.add(new JLabel("Charset"));
		northPanel.add(charsetField);
		northPanel.add(startDateButton);
		northPanel.add(startDateSpinner);
		northPanel.add(ageButton);
		northPanel.add(ageSpinner);
		northPanel.add(new JLabel("Context"));
		northPanel.add(contextSpinner);
		northPanel.add(startButton);
		northPanel.add(stopButton);
		northPanel.add(showAllButton);
		
		JScrollPane tableScroller = new JScrollPane(table);
		
		JPanel southPanel = new JPanel();
		southPanel.add(previewButton);
		southPanel.add(editorLabel);
		southPanel.add(editorButton);
		southPanel.add(openButton);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(northPanel, BorderLayout.NORTH);
		contentPanel.add(tableScroller, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);

		setContentPane(contentPanel);
		pack();
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
