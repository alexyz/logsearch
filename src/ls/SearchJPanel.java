package ls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ls.LogSearchUtil.*;

public class SearchJPanel extends JPanel implements SearchListener {

    
    private final JTextField containsTextField = new JTextField(20);
    private final JTextField doesNotContainTextField = new JTextField(15);
    private final DateTextFieldJPanel startDatePanel = new DateTextFieldJPanel("Start Date");
    private final DateTextFieldJPanel endDatePanel = new DateTextFieldJPanel("End Date");
    private final JSpinner ageDaysSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    private final JSpinner ageHoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton openButton = new JButton("Open in Editor");
    private final JButton saveButton = new JButton("Save...");
    private final ResultTableModel tableModel = new ResultTableModel();
    private final JTable table = new ResultsJTable(tableModel);

    private final JButton previewButton = new JButton("Preview");
    private final JButton previewAllButton = new JButton("Preview All");
    private final JCheckBox showUnmatchedCheckBox = new JCheckBox("Show Unmatched");

    private final JCheckBox ignoreCaseCheckBox = new JCheckBox("Ignore Case");
    private final JSpinner contextBeforeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JSpinner contextAfterSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JCheckBox regexCheckBox = new JCheckBox("Regex");
    private final JButton openInternalButton = new JButton("Open");
    private final JComboBox<String> rangeComboBox = new JComboBox<>();
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    private final JSpinner matchesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    private final JLabel startDateLabel = new JLabel("Start");
    private final JLabel endDateLabel = new JLabel("End");
    private final JLabel ageDaysLabel = new JLabel("Days");
    private final JLabel ageHoursLabel = new JLabel("Hours");
    private final JLabel countLabel = new JLabel("Count");

    private volatile SearchThread thread;
    
    public SearchJPanel () {
        super(new BorderLayout());
        
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
    
        Vector<String> ranges = new Vector<>(Arrays.asList(ALL_RANGE, COUNT_RANGE, DATE_RANGE, AGE_RANGE));
        Collections.sort(ranges);
        rangeComboBox.setModel(new DefaultComboBoxModel<>(ranges));
    
        int width = 12, height = 20;
    
        startDatePanel.setToolTipText("Earliest file date and time (inclusive)");
        endDatePanel.setToolTipText("Latest file date and time (exclusive)");
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
    
        //
    
        JPanel northPanel = boxPanel(
                flowPanel(rangeComboBox,
                        startDateLabel, startDatePanel, endDateLabel, endDatePanel,
                        ageDaysLabel, ageDaysSpinner, ageHoursLabel, ageHoursSpinner,
                        countLabel, countSpinner),
                flowPanel("Line Contains", containsTextField, "Doesn't Contain", doesNotContainTextField, regexCheckBox, ignoreCaseCheckBox),
                flowPanel("Context Before", contextBeforeSpinner, "After", contextAfterSpinner, "Max Matches", matchesSpinner, startButton, stopButton));
    
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        JScrollPane tableScroller = new JScrollPane(table);
    
        JPanel southPanel = flowPanel(showUnmatchedCheckBox, previewButton, previewAllButton, openInternalButton, saveButton, openButton);
    
        add(northPanel, BorderLayout.NORTH);
        add(tableScroller, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    
        updateRangePanels();
        updateStartStopButtons(true);
    
    }

    public void savePrefs(Preferences prefs,int i) {
        String s = i > 0 ? "."+i :"";
        
        prefs.put(Prefs.SEARCH +s, containsTextField.getText());
        prefs.put(Prefs.EXCLUDE +s, doesNotContainTextField.getText());
        prefs.putInt(Prefs.AGE +s, ((Number) ageDaysSpinner.getValue()).intValue());
        prefs.putInt(Prefs.AGE_HOURS +s, ((Number) ageHoursSpinner.getValue()).intValue());
        prefs.putBoolean(Prefs.CASE +s, ignoreCaseCheckBox.isSelected());
        prefs.putInt(Prefs.CONTEXT_BEFORE +s, ((Number) contextBeforeSpinner.getValue()).intValue());
        prefs.putInt(Prefs.CONTEXT_AFTER +s, ((Number) contextAfterSpinner.getValue()).intValue());
        prefs.putLong(Prefs.STARTDATE +s, startDatePanel.getTime());
        prefs.putLong(Prefs.ENDDATE +s, endDatePanel.getTime());
        prefs.putBoolean(Prefs.REGEX +s, regexCheckBox.isSelected());

        prefs.put(Prefs.RANGE +s, String.valueOf(rangeComboBox.getSelectedItem()));
        prefs.putInt(Prefs.MATCHES +s, ((Number) matchesSpinner.getValue()).intValue());
        prefs.putInt(Prefs.COUNT +s, ((Number) countSpinner.getValue()).intValue());
    }

    public void loadPrefs(Preferences prefs,int i) {
        String s = i > 0 ? "."+i :"";
        containsTextField.setText(prefs.get(Prefs.SEARCH +s, ""));
        doesNotContainTextField.setText(prefs.get(Prefs.EXCLUDE +s, ""));
        ageDaysSpinner.setValue(Integer.valueOf(prefs.getInt(Prefs.AGE +s, 7)));
        ageHoursSpinner.setValue(Integer.valueOf(prefs.getInt(Prefs.AGE_HOURS +s, 0)));
        ignoreCaseCheckBox.setSelected(prefs.getBoolean(Prefs.CASE +s, false));
        contextBeforeSpinner.setValue(Integer.valueOf(prefs.getInt(Prefs.CONTEXT_BEFORE +s, 1)));
        contextAfterSpinner.setValue(Integer.valueOf(prefs.getInt(Prefs.CONTEXT_AFTER +s, 1)));
        regexCheckBox.setSelected(prefs.getBoolean(Prefs.REGEX +s, false));
        Calendar cal = new GregorianCalendar();
        GregorianCalendar midnightCal = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        Date endDate = midnightCal.getTime();
        midnightCal.add(Calendar.DATE, -7);
        Date startDate = midnightCal.getTime();
        startDatePanel.setDate(new Date(prefs.getLong(Prefs.STARTDATE +s, startDate.getTime())));
        endDatePanel.setDate(new Date(prefs.getLong(Prefs.ENDDATE +s, endDate.getTime())));
        rangeComboBox.setSelectedItem(prefs.get(Prefs.RANGE +s, AGE_RANGE));
        matchesSpinner.setValue(Integer.valueOf(prefs.getInt(Prefs.MATCHES +s, 1000)));
        countSpinner.setValue(Integer.valueOf(prefs.getInt(Prefs.COUNT +s, 100)));
    }

    private LogSearchJFrame getLsFrame() {
        return (LogSearchJFrame) SwingUtilities.getAncestorOfClass(LogSearchJFrame.class,this);
    }

    /** update start/stop buttons */
    private void updateStartStopButtons (boolean startEnabled) {
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
                    LogSearchJFrame f = getLsFrame();
                    ViewJFrame dialog = new ViewJFrame(f, file, f.getCharset(), containsTextField.getText(), ignoreCaseCheckBox.isSelected(), regexCheckBox.isSelected());
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

    private void preview () {
        try {
            System.out.println("preview");
            int row = table.getSelectedRow();
            if (row >= 0) {
                Result result = tableModel.getResult(row);
                if (result.lines.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    int prevLine = 0;
                    for (Map.Entry<Integer, String> e : result.lines.entrySet()) {
                        int line = e.getKey().intValue();
                        if (line > prevLine + 1) {
                            sb.append("\n");
                        }
                        sb.append("Line ").append(line).append(": ").append(e.getValue()).append("\n");
                        prevLine = line;
                    }
                    TextJFrame d = new TextJFrame(getLsFrame(), "Preview " + result.name);
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
            StringBuilder sb = new StringBuilder();
            for (Result result : tableModel.getResults()) {
                if (result.lines.size() > 0) {
                    sb.append("\n");
                    sb.append(result.name()).append("\n");
                    sb.append("\n");
                    for (Map.Entry<Integer, String> e : result.lines.entrySet()) {
                        sb.append("Line ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                    }
                }
            }
            TextJFrame d = new TextJFrame(getLsFrame(), "Preview All");
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
            JOptionPane.showMessageDialog(this, e.toString(), "Save", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInEditor () {
        try {
            System.out.println("open");
            File editorFile = getLsFrame().getEditorFile();
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
            LogSearchJFrame f = getLsFrame();
            List<DirOpt> dirs = f.getDirs();
    
            List<DirOpt> dirs2 = dirs.stream().filter(d -> d.dir.isDirectory() && d.enabled).collect(Collectors.toList());

            if (dirs2.size() == 0) {
                throw new Exception("No directories selected (or exist)");
            }

            tableModel.clear();

            // sort with highest dirs first
            Collections.sort(dirs2, DirOpt.LEN_CMP);

            List<DirOpt> dirs3 = new ArrayList<>();

            for (DirOpt d2 : dirs2) {
                String d2str = d2.dir.getAbsolutePath() + File.separator;
                boolean add = true;
                for (DirOpt d3 : dirs3) {
                    String d3str = d3.dir.getAbsolutePath() + File.separator;
                    // is this directory already included
                    if (d3.recursive && d2str.startsWith(d3str)) {
                        System.out.println("exclude dir " + d2 + " due to below recursive dir " + d3);
                        add = false;
                        break;
                    }
                }
                if (add) {
                    dirs3.add(d2);
                }
            }

            System.out.println("dirs to search: " + dirs3);

            if (dirs3.size() == 0) {
                throw new Exception("No directories exist");
            }

            Date startDate = null, endDate = null;
            int maxFiles = 0;

            String range = (String) rangeComboBox.getSelectedItem();
            if (range == DATE_RANGE) {
                startDate = startDatePanel.getDate();
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
            thread.dirs = dirs3;
            thread.startDate = startDate;
            thread.endDate = endDate;
            thread.maxFiles = maxFiles;
            thread.filenameLower = f.getFilenameContains().toLowerCase();
            thread.text = containsTextField.getText().trim();
            thread.exText = doesNotContainTextField.getText().trim();
            thread.regex = regexCheckBox.isSelected();
            thread.ignoreCase = ignoreCaseCheckBox.isSelected();
            thread.contextLinesBefore = ((Number)contextBeforeSpinner.getValue()).intValue();
            thread.contextLinesAfter = ((Number)contextAfterSpinner.getValue()).intValue();
            thread.maxMatches = ((Number)matchesSpinner.getValue()).intValue();
            thread.dateParser = new FileDater(true);
            thread.charset = f.getCharset();
            thread.cacheUncompressed = f.getCache();
            thread.start();

            updateStartStopButtons(false);

        } catch (Exception e) {
            e.printStackTrace(System.out);
            JOptionPane.showMessageDialog(this, e.toString(), "Search", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** change the enabled status of the range controls */
    private void updateRangePanels() {
        String range = (String)rangeComboBox.getSelectedItem();

        boolean date = range.equals(DATE_RANGE);
        startDateLabel.setEnabled(date);
        startDatePanel.setEnabled(date);
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
            getLsFrame().updateTitle(SearchJPanel.this,msg);
        });
    }

    @Override
    public void searchComplete (final SearchCompleteEvent e) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("search complete " + e);
            thread = null;
            getLsFrame().updateTitle(SearchJPanel.this,null);
            updateStartStopButtons(true);
            String durStr = e.time > 0 ? formatTime((int) e.time) : null;
            String sizeStr = e.size > 0 ? formatSize(e.size) : null;
            String sizePerSecStr = e.size > 0 ? formatSize((long) (e.size / e.time)) + "/s" : null;
            String msg = String.format("Search Complete: %d/%d files, %s, %s, %s, %d lines matched",
                    e.scanned, e.found, sizeStr, durStr, sizePerSecStr, e.matches);
            JOptionPane.showMessageDialog(this, msg, "Search Completed", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void searchError (final String msg) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("search error " + msg);
            thread = null;
            updateStartStopButtons(true);
            getLsFrame().updateTitle(SearchJPanel.this,null);
            JOptionPane.showMessageDialog(this, msg, "Search Error", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void stop () {
        if (thread != null) {
            updateStartStopButtons(true);
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
    
    public boolean isRunning() {
        return thread != null;
    }

}
