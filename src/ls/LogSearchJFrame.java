package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import static ls.LogSearchUtil.*;

public class LogSearchJFrame extends JFrame implements SearchListener {
	
	public static void main (final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		FileCache.init();
		LogSearchJFrame instance = new LogSearchJFrame();
		instance.setLocationRelativeTo(null);
		instance.setVisible(true);
	}
	
	private final JLabel dirLabel = new JLabel();
	private final JButton dirButton = new JButton("Directories...");
	private final JTextField nameTextField = new JTextField(10);
	private final JTextField containsTextField = new JTextField(20);
	private final JTextField doesNotContainTextField = new JTextField(15);
	//private final JSpinner startDateSpinner = new JSpinner();
	//private final JSpinner endDateSpinner = new JSpinner();
	private final DateTextFieldJPanel startDatePanel = new DateTextFieldJPanel();
	private final DateTextFieldJPanel endDatePanel = new DateTextFieldJPanel();
	private final JSpinner ageDaysSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
	private final JSpinner ageHoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JButton openButton = new JButton("Open in Editor");
	private final JButton saveButton = new JButton("Save...");
	private final ResultTableModel tableModel = new ResultTableModel();
	private final JTable table = new ResultsJTable(tableModel);
	private final JButton editorButton = new JButton("Editor...");
	private final JLabel editorLabel = new JLabel();
	private final JButton previewButton = new JButton("Preview");
	private final JButton previewAllButton = new JButton("Preview All");
	private final JCheckBox showUnmatchedCheckBox = new JCheckBox("Show Unmatched");
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final Set<File> dirs = new TreeSet<>();
	private final Set<File> disabledDirs = new TreeSet<>();
	private final JCheckBox ignoreCaseCheckBox = new JCheckBox("Ignore Case");
	private final JSpinner contextBeforeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
	private final JSpinner contextAfterSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
	private final JCheckBox regexCheckBox = new JCheckBox("Regex");
	private final JButton openInternalButton = new JButton("Open");
	private final JComboBox<ComboItem> charsetComboBox = new JComboBox<>();
	private final JCheckBox cacheCheckBox = new JCheckBox("Cache");
	private final JComboBox<String> rangeComboBox = new JComboBox<>();
	private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
	private final JSpinner matchesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
	private final JLabel startDateLabel = new JLabel("Start");
	private final JLabel endDateLabel = new JLabel("End");
	private final JLabel ageDaysLabel = new JLabel("Days");
	private final JLabel ageHoursLabel = new JLabel("Hours");
	private final JLabel countLabel = new JLabel("Count");

	
	private volatile SearchThread thread;
	
	private File currentDir;
	private File editorFile;
	
	public LogSearchJFrame() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(800, 600));
		setPreferredSize(new Dimension(800, 600));
		initComponents();
		initListeners();
		loadPrefs();
		updateTitle(null);
		pack();
	}
	
	private void loadPrefs () {
		System.out.println("load prefs");
		String userdir = System.getProperty("user.dir");
		stringToDirs(dirs, prefs.get(DIR_PREF, userdir));
		stringToDirs(disabledDirs, prefs.get(DIS_DIR_PREF, ""));
		updateDirsLabel();
		nameTextField.setText(prefs.get(NAME_PREF, "server.log"));
		containsTextField.setText(prefs.get(SEARCH_PREF, ""));
		doesNotContainTextField.setText(prefs.get(EXCLUDE_PREF, ""));
		ageDaysSpinner.setValue(Integer.valueOf(prefs.getInt(AGE_PREF, 7)));
		ageHoursSpinner.setValue(Integer.valueOf(prefs.getInt(AGE_HOURS_PREF, 0)));
		
		File defaultEditor = defaultEditor();
		String editorStr = prefs.get(EDITOR_PREF, defaultEditor != null ? defaultEditor.getAbsolutePath() : null);
		File editor = editorStr != null && editorStr.length() > 0 ? new File(editorStr) : null;
		editorFile = editor != null && editor.exists() ? editor : null;
		
		editorLabel.setText(editor != null ? editor.getName() : "no editor");
		ignoreCaseCheckBox.setSelected(prefs.getBoolean(CASE_PREF, false));
		contextBeforeSpinner.setValue(Integer.valueOf(prefs.getInt(CONTEXT_BEFORE_PREF, 1)));
		contextAfterSpinner.setValue(Integer.valueOf(prefs.getInt(CONTEXT_AFTER_PREF, 1)));
		regexCheckBox.setSelected(prefs.getBoolean(REGEX_PREF, false));
		
		Calendar cal = new GregorianCalendar();
		GregorianCalendar midnightCal = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		Date endDate = midnightCal.getTime();
		midnightCal.add(Calendar.DATE, -7);
		Date startDate = midnightCal.getTime();

		// FIXME ability to load null
		startDatePanel.setDate(new Date(prefs.getLong(STARTDATE_PREF, startDate.getTime())));
		endDatePanel.setDate(new Date(prefs.getLong(ENDDATE_PREF, endDate.getTime())));
		
		currentDir = new File(prefs.get(CD_PREF, userdir));
		rangeComboBox.setSelectedItem(prefs.get(RANGE_PREF, AGE_RANGE));
		cacheCheckBox.setSelected(prefs.getBoolean(CACHE_PREF, false));
		matchesSpinner.setValue(Integer.valueOf(prefs.getInt(MATCHES_PREF, 1000)));
		countSpinner.setValue(Integer.valueOf(prefs.getInt(COUNT_PREF, 100)));
	}



	private long getMillis(Date d) {
		return d != null ? d.getTime() : null;
	}
	
	private void savePrefs () {
		System.out.println("save prefs");
		prefs.put(DIR_PREF, dirsToString(dirs));
		prefs.put(DIS_DIR_PREF, dirsToString(disabledDirs));
		prefs.put(NAME_PREF, nameTextField.getText());
		prefs.put(SEARCH_PREF, containsTextField.getText());
		prefs.put(EXCLUDE_PREF, doesNotContainTextField.getText());
		prefs.put(EDITOR_PREF, editorFile != null ? editorFile.getAbsolutePath() : "");
		prefs.putInt(AGE_PREF, ((Number) ageDaysSpinner.getValue()).intValue());
		prefs.putInt(AGE_HOURS_PREF, ((Number) ageHoursSpinner.getValue()).intValue());
		prefs.putBoolean(CASE_PREF, ignoreCaseCheckBox.isSelected());
		prefs.putInt(CONTEXT_BEFORE_PREF, ((Number) contextBeforeSpinner.getValue()).intValue());
		prefs.putInt(CONTEXT_AFTER_PREF, ((Number) contextAfterSpinner.getValue()).intValue());
		prefs.putLong(STARTDATE_PREF, startDatePanel.getTime());
		prefs.putLong(ENDDATE_PREF, endDatePanel.getTime());
		prefs.putBoolean(REGEX_PREF, regexCheckBox.isSelected());
		prefs.put(CD_PREF, currentDir.getAbsolutePath());
		prefs.put(RANGE_PREF, String.valueOf(rangeComboBox.getSelectedItem()));
		prefs.putBoolean(CACHE_PREF, cacheCheckBox.isSelected());
		prefs.putInt(MATCHES_PREF, ((Number) matchesSpinner.getValue()).intValue());
		prefs.putInt(COUNT_PREF, ((Number) countSpinner.getValue()).intValue());
		try {
			prefs.flush();
		} catch (Exception e) {
			e.printStackTrace(System.out);
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
		dirButton.addActionListener(e -> chooseDirs());
		editorButton.addActionListener(e -> chooseEditor());
		startButton.addActionListener(e -> search());
		stopButton.addActionListener(e -> stop());
		openButton.addActionListener(e -> openInEditor());
		saveButton.addActionListener(e -> save());
		previewButton.addActionListener(e -> preview());
		previewAllButton.addActionListener(e -> previewAll());
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked (final MouseEvent e) {
				if (e.getClickCount() == 2) {
					preview();
				}
			}
		});
		showUnmatchedCheckBox.addActionListener(e -> showUnmatched());
		rangeComboBox.addItemListener(e -> updateRangePanels());
		openInternalButton.addActionListener(e -> view());
	}
	
	/** update start/stop buttons */
	private void updateStartStop (boolean startEnabled) {
		startButton.setEnabled(startEnabled);
		stopButton.setEnabled(!startEnabled);
		repaint();
	}
	
	private void view () {
		try {
			System.out.println("view");
			int r = table.getSelectedRow();
			if (r >= 0) {
				Result result = tableModel.getResult(r);
				File file = getOrCreateFileConfirm(result, "View");
				if (file != null) {
					ViewJFrame dialog = new ViewJFrame(this, file, charset(), containsTextField.getText(), ignoreCaseCheckBox.isSelected(), regexCheckBox.isSelected());
					dialog.setVisible(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "View", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/** confirm file create over 25MB compressed */
	private File getOrCreateFileConfirm (final Result result, final String title) throws Exception {
		File file = getOrCreateFile(result, false);
		if (file == null) {
			String msg = "Create large temp file from " + result.name + " size " + formatSize(result.pSize) + "?";
			if (result.pSize < CONFIRM_SIZE || JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				file = getOrCreateFile(result, true);
			}
		}
		return file;
	}
	
	private Charset charset () {
		return (Charset) ((ComboItem) charsetComboBox.getSelectedItem()).object;
	}
	
	private void preview () {
		try {
			System.out.println("preview");
			int row = table.getSelectedRow();
			if (row >= 0) {
				Result result = tableModel.getResult(row);
				if (result.lines.size() > 0) {
					StringBuffer sb = new StringBuffer();
					int prevLine = 0;
					for (Map.Entry<Integer, String> e : result.lines.entrySet()) {
						int line = e.getKey().intValue();
						if (line > prevLine + 1) {
							sb.append("\n");
						}
						sb.append("Line ").append(line).append(": ").append(e.getValue()).append("\n");
						prevLine = line;
					}
					TextJDialog d = new TextJDialog(this, "Preview " + result.name);
					d.setTextFont(new Font("monospaced", 0, 12));
					d.setText(sb.toString());
					d.setHighlight(pattern(), Color.orange);
					d.setVisible(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "Preview", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private Pattern pattern () {
		String text = containsTextField.getText();
		if (!regexCheckBox.isSelected()) {
			text = Pattern.quote(text);
		}
		int f = ignoreCaseCheckBox.isSelected() ? Pattern.CASE_INSENSITIVE : 0;
		return Pattern.compile(text, f);
	}
	
	private void previewAll () {
		try {
			System.out.println("preview all");
			StringBuffer sb = new StringBuffer();
			for (Result result : tableModel.getResults()) {
				if (result.lines.size() > 0) {
					sb.append("\n");
					sb.append(result.name() + "\n");
					sb.append("\n");
					for (Map.Entry<Integer, String> e : result.lines.entrySet()) {
						sb.append("Line ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
					}
				}
			}
			TextJDialog d = new TextJDialog(this, "Preview All");
			d.setTextFont(new Font("monospaced", 0, 12));
			d.setText(sb.toString());
			d.setHighlight(pattern(), Color.orange);
			d.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "Preview All", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void save () {
		try {
			System.out.println("save");
			int r = table.getSelectedRow();
			if (r >= 0) {
				Result result = tableModel.getResult(r);
				JFileChooser fc = new JFileChooser();
				fc.setSelectedFile(new File(result.suggestedFileName()));
				if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					copyToFile(result, fc.getSelectedFile());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(LogSearchJFrame.this, e.toString(), "Save", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void openInEditor () {
		try {
			System.out.println("open");
			if (editorFile == null) {
				throw new Exception("no editor selected");
			}
			int r = table.getSelectedRow();
			if (r >= 0) {
				Result result = tableModel.getResult(r);
				File file = getOrCreateFileConfirm(result, "Open");
				if (file != null) {
					int lineno = 0;
					if (result.lines.size() > 0) {
						lineno = result.lines.keySet().iterator().next().intValue();
					}
					execOpen(editorFile, file, lineno);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "Open", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void search () {
		try {
			System.out.println("search");
			
			if (dirs.size() == 0) {
				throw new Exception("No directories chosen");
			}
			
			tableModel.clear();
			
			// sort with highest dirs first
			List<File> sortedDirs = new ArrayList<>(dirs);
			Collections.sort(sortedDirs, new Comparator<File>() {
				@Override
				public int compare (final File o1, final File o2) {
					return o1.getAbsolutePath().length() - o2.getAbsolutePath().length();
				}
			});
			
			TreeSet<File> searchDirs = new TreeSet<>();
			
			for (File dir : sortedDirs) {
				String dirStr = dir.getAbsolutePath() + File.separator;
				boolean add = true;
				for (File searchDir : searchDirs) {
					String searchDirStr = searchDir.getAbsolutePath() + File.separator;
					// is this directory already included
					if (dirStr.startsWith(searchDirStr)) {
						System.out.println("exclude dir " + dir + " due to below " + searchDir);
						add = false;
						break;
					}
				}
				if (add && dir.isDirectory()) {
					searchDirs.add(dir);
				}
			}
			
			System.out.println("dirs to search: " + searchDirs);
			
			if (dirs.size() == 0) {
				throw new Exception("No directories exist");
			}
			
			Date startDate = null, endDate = null;
			int maxFiles = 0;
			
			String range = (String) rangeComboBox.getSelectedItem();
			if (range == DATE_RANGE) {
				startDate = startDatePanel.getDate();
				//Date endDateInclusive = (Date) endDateSpinner.getValue();
				//endDate = new Date(endDateInclusive.getTime() + MS_IN_DAY);
				endDate = endDatePanel.getDate();
			} else if (range == AGE_RANGE) {
				Calendar cal = new GregorianCalendar();
				cal.add(Calendar.DATE, -((Number)ageDaysSpinner.getValue()).intValue());
				cal.add(Calendar.HOUR_OF_DAY, -((Number)ageHoursSpinner.getValue()).intValue());
				startDate = cal.getTime();
			} else if (range == COUNT_RANGE) {
				maxFiles = ((Number)countSpinner.getValue()).intValue();
			}
			
			thread = new SearchThread();
			thread.listener = this;
			thread.dirs = searchDirs;
			thread.startDate = startDate;
			thread.endDate = endDate;
			thread.maxFiles = maxFiles;
			thread.filenameLower = nameTextField.getText().toLowerCase();
			thread.text = containsTextField.getText().trim();
			thread.exText = doesNotContainTextField.getText().trim();
			thread.regex = regexCheckBox.isSelected();
			thread.ignoreCase = ignoreCaseCheckBox.isSelected();
			thread.contextLinesBefore = ((Number)contextBeforeSpinner.getValue()).intValue();
			thread.contextLinesAfter = ((Number)contextAfterSpinner.getValue()).intValue();
			thread.maxMatches = ((Number)matchesSpinner.getValue()).intValue();
			thread.dateParser = new FileDater(true);
			thread.charset = charset();
			thread.cacheUncompressed = cacheCheckBox.isSelected();
			thread.start();
			
			updateStartStop(false);
			
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "Search", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void initComponents () {
		
		JMenuItem viewItem = new JMenuItem("View...");
		viewItem.addActionListener(e -> chooseAndView());
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(viewItem);
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
		
		//
		
		charsetComboBox.setModel(new DefaultComboBoxModel<>(LogSearchUtil.charsets()));
		charsetComboBox.setToolTipText("Character set to intepret files as");
		
		//
		
		Vector<String> ranges = new Vector<>(Arrays.asList(ALL_RANGE, COUNT_RANGE, DATE_RANGE, AGE_RANGE));
		Collections.sort(ranges);
		rangeComboBox.setModel(new DefaultComboBoxModel<>(ranges));
		
		int width = 12, height = 20;
		
		{
			Date maxDate = new GregorianCalendar(2099, 11, 31).getTime();
			Date minDate = new GregorianCalendar(1970, 0, 1).getTime();
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			SimpleDateFormat sdf = df instanceof SimpleDateFormat ? (SimpleDateFormat)df : null;
			String pattern = sdf != null ? sdf.toPattern() : "yyyy-MM-dd HH:mm";
			int len = (int) (df.format(new Date()).length() * 0.75);

			// FIXME
//			startDateSpinner.setPreferredSize(new Dimension(len * width, height));
//			startDateSpinner.setModel(new SpinnerDateModel(minDate, minDate, maxDate, Calendar.DATE));
//			startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, pattern));
//			startDateSpinner.setToolTipText("Earliest file date and time (inclusive)");
//
//			endDateSpinner.setPreferredSize(new Dimension(len * width, height));
//			endDateSpinner.setModel(new SpinnerDateModel(maxDate, minDate, maxDate, Calendar.DATE));
//			endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, pattern));
//			endDateSpinner.setToolTipText("Latest file date and time (exclusive)");
		}
		
		ageDaysSpinner.setPreferredSize(new Dimension(5 * width, height));
		ageDaysSpinner.setToolTipText("Maximum file age (days component)");
		ageHoursSpinner.setPreferredSize(new Dimension(4 * width, height));
		ageHoursSpinner.setToolTipText("Maximum file age (hours component)");
		countSpinner.setPreferredSize(new Dimension(5 * width, height));
		countSpinner.setToolTipText("Maximum number of files to scan");
		
		//
		
		regexCheckBox.setToolTipText("Interpret Line Contains and Doesn't Contain as Java regular expression");
		
		//
		
		contextBeforeSpinner.setPreferredSize(new Dimension(4 * width, height));
		contextBeforeSpinner.setToolTipText("Number of lines before match to include in preview");
		contextAfterSpinner.setPreferredSize(new Dimension(4 * width, height));
		contextAfterSpinner.setToolTipText("Number of lines after match to include in preview");
		matchesSpinner.setPreferredSize(new Dimension(5 * width, height));
		matchesSpinner.setToolTipText("Maximum number of matches per file (0 = unlimited)");
		cacheCheckBox.setToolTipText("Cache uncompressed files in memory");
		
		//
		
		JPanel northPanel = boxPanel(
				flowPanel(dirLabel, dirButton, "Filename Contains", nameTextField, "Charset", charsetComboBox),
				flowPanel(rangeComboBox, 
						startDateLabel, startDatePanel, endDateLabel, endDatePanel,
						ageDaysLabel, ageDaysSpinner, ageHoursLabel, ageHoursSpinner, 
						countLabel, countSpinner),
				flowPanel("Line Contains", containsTextField, "Doesn't Contain", doesNotContainTextField, regexCheckBox, ignoreCaseCheckBox),
				flowPanel("Context Before", contextBeforeSpinner, "After", contextAfterSpinner, "Max Matches", matchesSpinner, cacheCheckBox),
				flowPanel(startButton, stopButton));
		
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(400);
		table.getColumnModel().getColumn(2).setPreferredWidth(200);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);
		JScrollPane tableScroller = new JScrollPane(table);
		
		JPanel southPanel = new JPanel(new GridLayout(2, 1));
		southPanel.add(flowPanel(showUnmatchedCheckBox, previewButton, previewAllButton));
		southPanel.add(flowPanel(openInternalButton, saveButton, inlineFlowPanel(editorLabel, editorButton), openButton));
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(northPanel, BorderLayout.NORTH);
		contentPanel.add(tableScroller, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);
		
		updateRangePanels();
		updateStartStop(true);
		
		setContentPane(contentPanel);
		
	}
	
	/** change the enabled status of the range controls */
	private void updateRangePanels() {
		String range = (String)rangeComboBox.getSelectedItem();
		
		boolean date = range.equals(DATE_RANGE);
		startDateLabel.setEnabled(date);
		startDatePanel.setEnabled(date); // FIXME
		endDateLabel.setEnabled(date);
		endDatePanel.setEnabled(date);
		
		boolean age = range.equals(AGE_RANGE);
		ageDaysLabel.setEnabled(age);
		ageDaysSpinner.setEnabled(age);
		ageHoursLabel.setEnabled(age);
		ageHoursSpinner.setEnabled(age);
		
		boolean count = range.equals(COUNT_RANGE);
		countLabel.setEnabled(count);
		countSpinner.setEnabled(count);
		
		repaint();
	}
	
	protected void chooseAndView () {
		try {
			JFileChooser f = new JFileChooser();
			if (currentDir.isDirectory()) {
				f.setCurrentDirectory(currentDir);
			}
			if (f.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				currentDir = f.getSelectedFile().getParentFile();
				ViewJFrame fr = new ViewJFrame(this, f.getSelectedFile(), charset(), "", false, false);
				fr.setVisible(true);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "View", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void updateDirsLabel () {
		dirLabel.setText(dirs.size() + "/" + (dirs.size() + disabledDirs.size()));
	}
	
	@Override
	public void searchResult (final Result fd) {
		SwingUtilities.invokeLater(() -> {
			System.out.println("search result " + fd);
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
		});
	}
	
	@Override
	public void searchUpdate (String msg) {
		SwingUtilities.invokeLater(() -> {
			System.out.println("search update " + msg);
			updateTitle(msg);
		});
	}
	
	private void updateTitle (String msg) {
		String t = "LogSearch";
		if (msg != null && msg.length() > 0) {
			t += " [" + msg + "]";
		}
		setTitle(t);
	}
	
	@Override
	public void searchComplete (final SearchCompleteEvent e) {
		SwingUtilities.invokeLater(() -> {
			System.out.println("search complete " + e);
			thread = null;
			updateTitle(null);
			updateStartStop(true);
			String durStr = e.time > 0 ? formatTime((int) e.time) : null;
			String sizeStr = e.size > 0 ? formatSize(e.size) : null;
			String sizePerSecStr = e.size > 0 ? formatSize((long) (e.size / e.time)) + "/s" : null;
			String msg = String.format("Search Complete: %d/%d files, %s, %s, %s, %d lines matched", 
					e.scanned, e.found, sizeStr, durStr, sizePerSecStr, e.matches);
			JOptionPane.showMessageDialog(LogSearchJFrame.this, msg, "Search Completed", JOptionPane.INFORMATION_MESSAGE);
		});
	}
	
	@Override
	public void searchError (final String msg) {
		SwingUtilities.invokeLater(() -> {
			System.out.println("search error " + msg);
			thread = null;
			updateStartStop(true);
			updateTitle(null);
			JOptionPane.showMessageDialog(LogSearchJFrame.this, msg, "Search Error", JOptionPane.INFORMATION_MESSAGE);
		});
	}
	
	private void stop () {
		if (thread != null) {
			updateStartStop(true);
			thread.running = false;
		}
	}
	
	private void showUnmatched () {
		final int r = table.getSelectedRow();
		Result result = null;
		if (r >= 0) {
			result = tableModel.getResult(r);
		}
		tableModel.setShowAll(showUnmatchedCheckBox.isSelected());
		final int r2 = tableModel.getRow(result);
		if (r2 >= 0) {
			table.getSelectionModel().setSelectionInterval(r2, r2);
		}
	}
	
	private void chooseDirs () {
		DirectoryJDialog d = new DirectoryJDialog(this, "Log Directories");
		d.addDirs(dirs, true);
		d.addDirs(disabledDirs, false);
		d.setVisible(true);
		if (d.isOk()) {
			dirs.clear();
			dirs.addAll(d.getDirs(true));
			disabledDirs.clear();
			disabledDirs.addAll(d.getDirs(false));
			updateDirsLabel();
		}
	}
	
	private void chooseEditor () {
		JFileChooser fc = new JFileChooser();
		if (editorFile != null) {
			fc.setSelectedFile(editorFile);
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
		int o = fc.showOpenDialog(LogSearchJFrame.this);
		if (o == JFileChooser.APPROVE_OPTION) {
			editorFile = fc.getSelectedFile();
			editorLabel.setText(editorFile.getName());
		}
	}
	
}
