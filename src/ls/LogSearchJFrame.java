package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.text.WordUtils;

import static ls.LogSearchUtil.*;

public class LogSearchJFrame extends JFrame implements SearchListener {

	public static final String TITLE = "LogSearch";

	private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
	private static final String STARTDATE_PREF = "startdate";
	private static final String ENDDATE_PREF = "enddate";
	private static final String PARSEDATE_PREF = "parsedate";
	private static final String CONTEXT_BEFORE_PREF = "contextbefore";
	private static final String CONTEXT_AFTER_PREF = "contextafter";
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
	private final JTextField nameTextField = new JTextField();
	private final JTextField searchTextField = new JTextField();
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
	private final JCheckBox ignoreCaseCheckBox = new JCheckBox("Ignore Case");
	private final JSpinner contextBeforeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
	private final JSpinner contextAfterSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
	private final JCheckBox parseDateCheckBox = new JCheckBox("Date from Filename");
	private final JCheckBox regexCheckBox = new JCheckBox("Regex");
	private final JButton viewButton = new JButton("View");
	private final JComboBox<ComboItem> charsetComboBox = new JComboBox<>();

	private volatile SearchThread thread;

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
		nameTextField.setText(prefs.get(NAME_PREF, "server.log"));
		searchTextField.setText(prefs.get(SEARCH_PREF, "a"));
		ageSpinner.setValue(prefs.getInt(AGE_PREF, 7));
		editor = new File(prefs.get(EDITOR_PREF, defaultEditor().getAbsolutePath()));
		if (!editor.exists()) {
			editor = null;
		}
		editorLabel.setText(editor != null ? editor.getName() : "no editor");
		dateRadioButton.setSelected(prefs.getBoolean(START_OR_AGE_PREF, true));
		ageRadioButton.setSelected(!dateRadioButton.isSelected());
		ignoreCaseCheckBox.setSelected(prefs.getBoolean(CASE_PREF, false));
		contextBeforeSpinner.setValue(prefs.getInt(CONTEXT_BEFORE_PREF, 1));
		contextAfterSpinner.setValue(prefs.getInt(CONTEXT_AFTER_PREF, 1));
		parseDateCheckBox.setSelected(prefs.getBoolean(PARSEDATE_PREF, true));
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
		prefs.put(NAME_PREF, nameTextField.getText());
		prefs.put(SEARCH_PREF, searchTextField.getText());
		prefs.put(EDITOR_PREF, editor != null ? editor.getAbsolutePath() : "");
		prefs.putInt(AGE_PREF, (int) ageSpinner.getValue());
		prefs.putBoolean(START_OR_AGE_PREF, dateRadioButton.isSelected());
		prefs.putBoolean(CASE_PREF, ignoreCaseCheckBox.isSelected());
		prefs.putInt(CONTEXT_BEFORE_PREF, (int) contextBeforeSpinner.getValue());
		prefs.putInt(CONTEXT_AFTER_PREF, (int) contextAfterSpinner.getValue());
		prefs.putBoolean(PARSEDATE_PREF, parseDateCheckBox.isSelected());
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
				System.out.println("log search closing");
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
				if (thread != null) {
					thread.running = false;
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
				File file = LogSearchUtil.toTempFile(result);
				ViewJFrame dialog = new ViewJFrame(this, file, charset(), searchTextField.getText(), ignoreCaseCheckBox.isSelected(), regexCheckBox.isSelected());
				dialog.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(LogSearchJFrame.this, e.toString(), "View", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private Charset charset() {
		return (Charset) ((ComboItem)charsetComboBox.getSelectedItem()).object;
	}

	private void preview () {
		System.out.println("preview");
		final int row = table.getSelectedRow();
		if (row >= 0) {
			final Result result = tableModel.getResult(row);
			if (result.lines.size() > 0) {
				final StringBuffer sb = new StringBuffer();
				int prevLine = 0;
				for (final Map.Entry<Integer,String> e : result.lines.entrySet()) {
					int line = e.getKey();
					if (line > prevLine + 1) {
						sb.append("\n");
					}
					sb.append("Line ").append(line).append(": ").append(e.getValue()).append("\n");
					prevLine = line;
				}
				final TextJDialog d = new TextJDialog(this, "Preview " + result.name);
				d.setTextFont(new Font("monospaced", 0, 12));
				d.setText(sb.toString());
				d.setHighlight(pattern(), Color.orange);
				d.setVisible(true);
			}
		}
	}

	private Pattern pattern () {
		String text = searchTextField.getText();
		if (!regexCheckBox.isSelected()) {
			text = Pattern.quote(text);
		}
		final int f = ignoreCaseCheckBox.isSelected() ? Pattern.CASE_INSENSITIVE : 0;
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
			}
		}
		final TextJDialog d = new TextJDialog(this, "Preview All");
		d.setTextFont(new Font("monospaced", 0, 12));
		d.setText(sb.toString());
		d.setHighlight(pattern(), Color.orange);
		d.setVisible(true);
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

			if (thread != null) {
				throw new Exception("Already running");
			}

			if (dirs.size() == 0) {
				throw new Exception("No directories chosen");
			}

			tableModel.clear();

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
			
			thread = new SearchThread(this);
			thread.dirs = searchDirs;
			if (dateRadioButton.isSelected()) {
				thread.startDate = (Date) startDateSpinner.getValue();
				Date endDateInclusive = (Date) endDateSpinner.getValue();
				thread.endDate = new Date(endDateInclusive.getTime() + MS_IN_DAY);
			} else {
				final Calendar cal = new GregorianCalendar();
				cal.add(Calendar.DATE, -(int) ageSpinner.getValue());
				thread.startDate = cal.getTime();
				thread.endDate = new Date();
			}
			thread.setFilename(nameTextField.getText());
			thread.setText(searchTextField.getText(), regexCheckBox.isSelected(), ignoreCaseCheckBox.isSelected());
			thread.contextLinesBefore = (int) contextBeforeSpinner.getValue();
			thread.contextLinesAfter = (int) contextAfterSpinner.getValue();
			thread.dateParser = new FileDater(parseDateCheckBox.isSelected());
			thread.charset = charset();

			thread.start();
			
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Search", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void initComponents () {
		
		JMenuItem viewItem = new JMenuItem("View...");
		viewItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				viewExternal();
			}
		});
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(viewItem);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		
		setJMenuBar(menuBar);
		
		nameTextField.setColumns(10);
		searchTextField.setColumns(15);

		{
			final Date maxDate = new GregorianCalendar(2099, 11, 31).getTime();
			final Date minDate = new GregorianCalendar(1970, 0, 1).getTime();
			startDateSpinner.setModel(new SpinnerDateModel(minDate, minDate, maxDate, Calendar.DATE));
			startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, DF.toPattern()));
			endDateSpinner.setModel(new SpinnerDateModel(maxDate, minDate, maxDate, Calendar.DATE));
			endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, DF.toPattern()));
		}
		
		{
			Vector<ComboItem> v = new Vector<>();
			Charset dcs = Charset.defaultCharset();
			v.add(new ComboItem(dcs, dcs.name()));
			for (Charset cs : new Charset[] { StandardCharsets.US_ASCII, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8, StandardCharsets.UTF_16 }) {
				if (!cs.name().equals(dcs.name())) {
					v.add(new ComboItem(cs, cs.name()));
				}
			}
			charsetComboBox.setModel(new DefaultComboBoxModel<>(v));
		}

		final ButtonGroup bg = new ButtonGroup();
		bg.add(dateRadioButton);
		bg.add(ageRadioButton);

		final JPanel northPanel = new JPanel(new FlowLayout2());
		northPanel.add(inlineFlowPanel(dirLabel, dirButton));
		northPanel.add(inlineFlowPanel(new JLabel("Filename Contains"), nameTextField));
		northPanel.add(parseDateCheckBox);
		northPanel.add(inlineFlowPanel(new JLabel("Line Contains"), searchTextField, regexCheckBox, ignoreCaseCheckBox, charsetComboBox));
		northPanel.add(inlineFlowPanel(dateRadioButton, startDateSpinner, new JLabel("-"), endDateSpinner, ageRadioButton, ageSpinner));
		northPanel.add(inlineFlowPanel(new JLabel("Context Before"), contextBeforeSpinner, new JLabel("After"), contextAfterSpinner));
		northPanel.add(inlineFlowPanel(startButton, stopButton));

		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(400);
		table.getColumnModel().getColumn(2).setPreferredWidth(200);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);
		final JScrollPane tableScroller = new JScrollPane(table);

		final JPanel southPanel = new JPanel(new GridLayout(2, 1));
		southPanel.add(flowPanel(showAllButton, previewButton, previewAllButton));
		southPanel.add(flowPanel(viewButton, saveButton, inlineFlowPanel(editorLabel, editorButton), openButton));

		final JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(northPanel, BorderLayout.NORTH);
		contentPanel.add(tableScroller, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);

		setContentPane(contentPanel);
		pack();
	}

	protected void viewExternal () {
		JFileChooser f = new JFileChooser();
		if (f.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				ViewJFrame fr = new ViewJFrame(this, f.getSelectedFile(), charset(), "", false, false);
				fr.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, WordUtils.wrap("Could not view external: " + e, 80));
			}
		}
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
	public void searchComplete (final SearchCompleteEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				updateTitle(null);
				String msg = String.format("Files: %d\nSize: %s\nSeconds: %.1f\nSpeed: %s", 
						e.results, formatSize(e.bytes), e.seconds, formatSize((long)(e.bytes/e.seconds)) + "/s");
				JOptionPane.showMessageDialog(LogSearchJFrame.this, msg, "Search Completed", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		thread = null;
	}
	
	@Override
	public void searchError (final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				updateTitle(null);
				JOptionPane.showMessageDialog(LogSearchJFrame.this, msg, "Search Error", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		thread = null;
	}
}
