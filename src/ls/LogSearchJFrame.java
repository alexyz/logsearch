package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import static ls.LogSearchUtil.*;

public class LogSearchJFrame extends JFrame implements SearchListener {

	public static final String TITLE = "LogSearch 2016-02-18";

	private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
	private static final String STARTDATE_PREF = "startdate";
	private static final String ENDDATE_PREF = "enddate";
	private static final String PARSEDATE_PREF = "parsedate";
	private static final String CONTEXT_PREF = "context";
	private static final String CASE_PREF = "case";
	private static final String START_OR_AGE_PREF = "start";
	private static final String EDITOR_PREF = "editor";
	private static final String AGE_PREF = "age";
	private static final String SEARCH_PREF = "search";
	private static final String NAME_PREF = "name";
	private static final String DIR_PREF = "dir";
	private static final String DIS_DIR_PREF = "disdir";
	private static final String REGEX_PREF = "regex";

	public static void main (final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final LogSearchJFrame instance = new LogSearchJFrame();
		instance.setVisible(true);
	}

	private final JLabel dirLabel = new JLabel();
	private final JButton dirButton = new JButton("Directories...");
	private final JTextField nameField = new JTextField();
	private final JTextField searchField = new JTextField();
	private final JSpinner startDateSpinner = new JSpinner();
	private final JSpinner endDateSpinner = new JSpinner();
	private final JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(7, 1, 999, 1));
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JButton openButton = new JButton("Open");
	private final JButton saveButton = new JButton("Save");
	private final ResultTableModel tableModel = new ResultTableModel();
	private final JTable table = new ResultsJTable(tableModel);
	private final JButton editorButton = new JButton("Editor...");
	private final JLabel editorLabel = new JLabel();
	private final JButton previewButton = new JButton("Preview");
	private final JButton previewAllButton = new JButton("Preview All");
	private final JToggleButton showAllButton = new JToggleButton("Show Unmatched");
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final JRadioButton dateRadioButton = new JRadioButton("Date Range");
	private final JRadioButton ageRadioButton = new JRadioButton("Age (Days)");
	private final Set<File> dirs = new TreeSet<>();
	private final Set<File> disDirs = new TreeSet<>();
	private final JCheckBox ignoreCaseBox = new JCheckBox("Ignore Case");
	private final JSpinner contextSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 10, 1));
	private final JCheckBox parseDateBox = new JCheckBox("Date from Filename");
	private final JCheckBox regexCheckBox = new JCheckBox("Regex");
	private final JButton viewButton = new JButton("View");

	private volatile SearchThread searchThread;

	private File editor;

	public LogSearchJFrame () {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(800, 600));
		initComponents();
		initListeners();
		loadPrefs();
		updateTitle(null);
	}

	private void loadPrefs () {
		System.out.println("load prefs");
		stringToDirs(dirs, prefs.get(DIR_PREF, System.getProperty("user.dir")));
		stringToDirs(disDirs, prefs.get(DIS_DIR_PREF, ""));
		dirLabel.setText(String.valueOf(dirs.size()));
		nameField.setText(prefs.get(NAME_PREF, "server.log"));
		searchField.setText(prefs.get(SEARCH_PREF, "a"));
		ageSpinner.setValue(prefs.getInt(AGE_PREF, 7));
		editor = new File(prefs.get(EDITOR_PREF, defaultEditor().getAbsolutePath()));
		if (!editor.exists()) {
			editor = null;
		}
		editorLabel.setText(editor != null ? editor.getName() : "no editor");
		dateRadioButton.setSelected(prefs.getBoolean(START_OR_AGE_PREF, true));
		ageRadioButton.setSelected(!dateRadioButton.isSelected());
		ignoreCaseBox.setSelected(prefs.getBoolean(CASE_PREF, false));
		contextSpinner.setValue(prefs.getInt(CONTEXT_PREF, 2));
		parseDateBox.setSelected(prefs.getBoolean(PARSEDATE_PREF, true));
		regexCheckBox.setSelected(prefs.getBoolean(REGEX_PREF, false));

		final Calendar cal = new GregorianCalendar();
		final GregorianCalendar midnightCal = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		final Date endDate = midnightCal.getTime();
		midnightCal.add(Calendar.DATE, -7);
		final Date startDate = midnightCal.getTime();
		startDateSpinner.setValue(new Date(prefs.getLong(STARTDATE_PREF, startDate.getTime())));
		endDateSpinner.setValue(new Date(prefs.getLong(ENDDATE_PREF, endDate.getTime())));
	}

	private void savePrefs () {
		System.out.println("save prefs");
		prefs.put(DIR_PREF, dirsToString(dirs));
		prefs.put(DIS_DIR_PREF, dirsToString(disDirs));
		prefs.put(NAME_PREF, nameField.getText());
		prefs.put(SEARCH_PREF, searchField.getText());
		prefs.put(EDITOR_PREF, editor != null ? editor.getAbsolutePath() : "");
		prefs.putInt(AGE_PREF, (int) ageSpinner.getValue());
		prefs.putBoolean(START_OR_AGE_PREF, dateRadioButton.isSelected());
		prefs.putBoolean(CASE_PREF, ignoreCaseBox.isSelected());
		prefs.putInt(CONTEXT_PREF, (int) contextSpinner.getValue());
		prefs.putBoolean(PARSEDATE_PREF, parseDateBox.isSelected());
		prefs.putLong(STARTDATE_PREF, ((Date)startDateSpinner.getValue()).getTime());
		prefs.putLong(ENDDATE_PREF, ((Date)endDateSpinner.getValue()).getTime());
		prefs.putBoolean(REGEX_PREF, regexCheckBox.isSelected());
		try {
			prefs.flush();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void initListeners () {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing (final WindowEvent e) {
				System.out.println("window closing");
				savePrefs();
			}
		});

		dirButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent e) {
				final DirectoryJDialog d = new DirectoryJDialog(LogSearchJFrame.this, "Log Directories");
				d.addDirs(dirs, true);
				d.addDirs(disDirs, false);
				d.setVisible(true);
				if (d.isOk()) {
					dirs.clear();
					dirs.addAll(d.getDirs(true));
					dirLabel.setText(String.valueOf(dirs.size()));
					disDirs.clear();
					disDirs.addAll(d.getDirs(false));
				}
			}
		});

		editorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent e) {
				final JFileChooser fc = new JFileChooser();
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
					public boolean accept (final File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".exe");
					}
				});
				final int o = fc.showOpenDialog(LogSearchJFrame.this);
				if (o == JFileChooser.APPROVE_OPTION) {
					editor = fc.getSelectedFile();
					editorLabel.setText(editor.getName());
				}
			}
		});

		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent ae) {
				try {
					search();
				} catch (final Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(LogSearchJFrame.this, e.toString());
				}
			}
		});

		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent e) {
				if (searchThread != null) {
					searchThread.running = false;
				}
			}
		});

		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent ae) {
				open();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent ae) {
				save();
			}
		});

		previewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent ae) {
				preview();
			}
		});

		previewAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent ae) {
				previewAll();
			}
		});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked (final MouseEvent e) {
				if (e.getClickCount() == 2) {
					preview();
				}
			}
		});

		showAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent e) {
				final int r = table.getSelectedRow();
				Result result = null;
				if (r >= 0) {
					result = tableModel.getResult(r);
				}
				tableModel.setShowAll(showAllButton.isSelected());
				final int r2 = tableModel.getRow(result);
				if (r2 >= 0) {
					table.getSelectionModel().setSelectionInterval(r2, r2);
				}
			}
		});

		final ItemListener radioButtonListener = new ItemListener() {
			@Override
			public void itemStateChanged (final ItemEvent e) {
				startDateSpinner.setEnabled(dateRadioButton.isSelected());
				endDateSpinner.setEnabled(dateRadioButton.isSelected());
				ageSpinner.setEnabled(ageRadioButton.isSelected());
			}
		};

		dateRadioButton.addItemListener(radioButtonListener);
		ageRadioButton.addItemListener(radioButtonListener);

		viewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				view();
			}
		});
	}

	private void view() {
		System.out.println("view");
		final int r = table.getSelectedRow();
		if (r >= 0) {
			final Result result = tableModel.getResult(r);
			try {
				ViewJFrame dialog = new ViewJFrame(this, result, pattern());
				dialog.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(LogSearchJFrame.this, e.toString(), "View", JOptionPane.ERROR_MESSAGE);
			}
		}
	}


	private void preview () {
		System.out.println("preview");
		final int row = table.getSelectedRow();
		if (row >= 0) {
			final Result result = tableModel.getResult(row);
			if (result.lines.size() > 0) {
				final StringBuffer sb = new StringBuffer();
				for (final Map.Entry<Integer,String> e : result.lines.entrySet()) {
					sb.append("Line ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
				}
				final TextJDialog d = new TextJDialog(this, "Preview " + result.name);
				d.setTextFont(new Font("monospaced", 0, 12));
				d.setText(sb.toString());
				final Pattern p = pattern();
				d.setHighlight(p, Color.orange);
				d.setVisible(true);
			}
		}
	}

	private Pattern pattern () {
		String text = searchField.getText();
		if (!regexCheckBox.isSelected()) {
			text = Pattern.quote(text);
		}
		final int f = ignoreCaseBox.isSelected() ? Pattern.CASE_INSENSITIVE : 0;
		final Pattern p = Pattern.compile(text, f);
		return p;
	}

	private void previewAll () {
		System.out.println("preview all");
		final StringBuffer sb = new StringBuffer();
		for (Result result : tableModel.getResults()) {
			if (result.lines.size() > 0) {
				sb.append("\n");
				sb.append(result.name() + "\n");
				sb.append("\n");
				for (final Map.Entry<Integer,String> e : result.lines.entrySet()) {
					sb.append("Line ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
				}
				final TextJDialog d = new TextJDialog(this, "Preview All");
				d.setTextFont(new Font("monospaced", 0, 12));
				d.setText(sb.toString());
				final Pattern p = pattern();
				d.setHighlight(p, Color.orange);
				d.setVisible(true);
			}
		}
	}

	private void save () {
		try {
			System.out.println("save");
			final int r = table.getSelectedRow();
			if (r >= 0) {
				final Result result = tableModel.getResult(r);
				JFileChooser fc = new JFileChooser();
				fc.setSelectedFile(new File(result.tempName()));
				if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					toFile(result, fc.getSelectedFile());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(LogSearchJFrame.this, e.toString(), "Save", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void open () {
		try {
			System.out.println("open");
			if (editor == null) {
				throw new Exception("no editor selected");
			}
			final int r = table.getSelectedRow();
			if (r >= 0) {
				final Result result = tableModel.getResult(r);
				File file = toTempFile(result);

				int lineno = 0;
				if (result.lines.size() > 0) {
					lineno = result.lines.keySet().iterator().next();
				}
				execOpen(editor, file, lineno);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Open", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void search () throws Exception {
		try {
			System.out.println("search");

			if (searchThread != null) {
				throw new Exception("Already running");
			}

			if (dirs.size() == 0) {
				throw new Exception("No directories chosen");
			}

			tableModel.clear();

			final String text = searchField.getText();

			// sort with highest dirs first
			final List<File> sortedDirs = new ArrayList<>(dirs);
			Collections.sort(sortedDirs, new Comparator<File>() {
				@Override
				public int compare (final File o1, final File o2) {
					return o1.getAbsolutePath().length() - o2.getAbsolutePath().length();
				}
			});

			final TreeSet<File> searchDirs = new TreeSet<>();
			for (final File dir : sortedDirs) {
				final String dirStr = dir.getAbsolutePath() + File.separator;
				boolean add = true;
				for (final File searchDir : searchDirs) {
					final String searchDirStr = searchDir.getAbsolutePath() + File.separator;
					// is this directory already included
					if (dirStr.startsWith(searchDirStr)) {
						System.out.println("exclude dir " + dir + " due to below " + searchDir);
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
			final Date endDate;

			if (dateRadioButton.isSelected()) {
				startDate = (Date) startDateSpinner.getValue();
				long msInDay = 1000 * 60 * 60 * 24;
				Date endDateInclusive = (Date) endDateSpinner.getValue();
				endDate = new Date(endDateInclusive.getTime() + msInDay);

			} else {
				final Calendar cal = new GregorianCalendar();
				cal.add(Calendar.DATE, -(int)ageSpinner.getValue());
				startDate = cal.getTime();
				endDate = new Date();
			}

			if (startDate.compareTo(endDate) >= 0) {
				throw new Exception("Start date equal to or after end date");
			}

			final String name = nameField.getText();
			if (name.length() == 0) {
				throw new Exception("No file name filter");
			}

			final boolean ignoreCase = ignoreCaseBox.isSelected();

			final boolean regex = regexCheckBox.isSelected();

			final int context = (int) contextSpinner.getValue();

			final FileDater fd = new FileDater(parseDateBox.isSelected());

			savePrefs();

			searchThread = new SearchThread(this, searchDirs, startDate, endDate, name, text, regex, ignoreCase, context, fd);
			searchThread.start();
			
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Search", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void initComponents () {
		nameField.setColumns(10);
		searchField.setColumns(15);

		{
			final Date maxDate = new GregorianCalendar(2099, 11, 31).getTime();
			final Date minDate = new GregorianCalendar(2000, 0, 1).getTime();
			startDateSpinner.setModel(new SpinnerDateModel(minDate, minDate, maxDate, Calendar.DATE));
			startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, DF.toPattern()));
			endDateSpinner.setModel(new SpinnerDateModel(maxDate, minDate, maxDate, Calendar.DATE));
			endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, DF.toPattern()));
		}

		final ButtonGroup bg = new ButtonGroup();
		bg.add(dateRadioButton);
		bg.add(ageRadioButton);

		final JPanel northPanel = new JPanel(new FlowLayout2());
		northPanel.add(inlineFlowPanel(dirLabel, dirButton));
		northPanel.add(inlineFlowPanel(new JLabel("Filename Contains"), nameField));
		northPanel.add(parseDateBox);
		northPanel.add(inlineFlowPanel(new JLabel("Line Contains"), searchField, regexCheckBox, ignoreCaseBox));
		northPanel.add(inlineFlowPanel(dateRadioButton, startDateSpinner, new JLabel("-"), endDateSpinner, ageRadioButton, ageSpinner));
		northPanel.add(inlineFlowPanel(new JLabel("Context Lines"), contextSpinner));
		northPanel.add(inlineFlowPanel(startButton, stopButton));

		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(400);
		table.getColumnModel().getColumn(2).setPreferredWidth(200);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);
		final JScrollPane tableScroller = new JScrollPane(table);

		final JPanel southPanel = new JPanel(new GridLayout(2, 1));
		southPanel.add(flowPanel(showAllButton, previewButton, previewAllButton));
		southPanel.add(flowPanel(viewButton, inlineFlowPanel(editorLabel, editorButton), openButton, saveButton));

		final JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(northPanel, BorderLayout.NORTH);
		contentPanel.add(tableScroller, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);

		setContentPane(contentPanel);
		pack();
	}

	private void updateTitle(String msg) {
		String t = TITLE;
		if (msg != null && msg.length() > 0) {
			t += " [" + msg + "]";
		}
		setTitle(t);
	}

	@Override
	public void searchResult (final Result fd) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				final int r = table.getSelectedRow();
				Result result = null;
				if (r >= 0) {
					result = tableModel.getResult(r);
				}
				tableModel.add(fd);
				final int r2 = tableModel.getRow(result);
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
				updateTitle(msg);
			}
		});
	}

	@Override
	public void searchComplete (final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				updateTitle(null);
				JOptionPane.showMessageDialog(LogSearchJFrame.this, msg, "Search Completed", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		searchThread = null;
	}
}
