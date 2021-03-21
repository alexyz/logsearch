package ls;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import static ls.LogSearchUtil.*;

public class LogSearchJFrame extends JFrame {
	
	public static void main (final String[] args) {
		try {
			FileCache.init();
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			LogSearchJFrame instance = new LogSearchJFrame();
			instance.setLocationRelativeTo(null);
			instance.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null,e.toString(),"Could not open",JOptionPane.ERROR_MESSAGE);
			// exit the awt thread
			System.exit(1);
		}
	}
	
	private final JLabel dirLabel = new JLabel();
	private final JButton dirButton = new JButton("Directories...");
	private final JButton editorButton = new JButton("Editor...");
	private final JLabel editorLabel = new JLabel();
	private final JMenuItem viewItem = new JMenuItem("Open Large File...");
	private final JMenuItem addTabItem = new JMenuItem("Add Tab");
	private final JMenuItem removeTabItem = new JMenuItem("Remove Tab");
	private final JMenu optionsMenu = new JMenu("Options");
	private final JTextField filenameContainsTextField = new JTextField(10);
	private final JCheckBox cacheCheckBox = new JCheckBox("Cache");
	private final JComboBox<ComboItem> charsetComboBox = new JComboBox<>();
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final List<DirOpt> dirs = new ArrayList<>();
	private final JMenuBar menuBar = new JMenuBar();
	private final JTabbedPane tabs = new JTabbedPane();
	private final SortedMap<Integer, String> titles = new TreeMap<>();
	
	private File currentDir;
	private File editorFile;
	
	public LogSearchJFrame () {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(800, 600));
		setPreferredSize(new Dimension(800, 600));
		initComponents();
		loadPrefs();
		updateTitle(null, null);
		pack();
	}
	
	private void loadPrefs () {
		System.out.println("load prefs");
		
		String userdir = System.getProperty("user.dir");
		dirs.addAll(DirOpt.stringToDirs(prefs.get(Prefs.DIR, userdir), true, true));
		dirs.addAll(DirOpt.stringToDirs(prefs.get(Prefs.DISABLED_DIR, ""), false, true));
		dirs.addAll(DirOpt.stringToDirs(prefs.get(Prefs.NONREC_DIR, ""), true, false));
		dirs.addAll(DirOpt.stringToDirs(prefs.get(Prefs.NONREC_DISABLED_DIR_PREF, ""), false, false));
		updateDirsLabel();
		
		filenameContainsTextField.setText(prefs.get(Prefs.NAME, "server.log"));
		
		File defaultEditor = defaultEditor();
		String editorStr = prefs.get(Prefs.EDITOR, defaultEditor != null ? defaultEditor.getAbsolutePath() : null);
		File editor = editorStr != null && editorStr.length() > 0 ? new File(editorStr) : null;
		editorFile = editor != null && editor.exists() ? editor : null;
		editorLabel.setText(editor != null ? editor.getName() : "no editor");
		
		currentDir = new File(prefs.get(Prefs.CURRENT_DIR, userdir));
		
		cacheCheckBox.setSelected(prefs.getBoolean(Prefs.CACHE, false));
		
		int tabcount = prefs.getInt(Prefs.TABS, 1);
		for (int t = 0; t < tabcount; t++) {
			SearchJPanel p = new SearchJPanel();
			p.loadPrefs(prefs, t);
			tabs.addTab("Search" + (t + 1), p);
		}
	}
	
	private void savePrefs () {
		System.out.println("save prefs");
		prefs.put(Prefs.DIR, DirOpt.dirsToString(dirs, true, true));
		prefs.put(Prefs.DISABLED_DIR, DirOpt.dirsToString(dirs, false, true));
		prefs.put(Prefs.NONREC_DIR, DirOpt.dirsToString(dirs, true, false));
		prefs.put(Prefs.NONREC_DISABLED_DIR_PREF, DirOpt.dirsToString(dirs, false, false));
		prefs.put(Prefs.EDITOR, editorFile != null ? editorFile.getAbsolutePath() : "");
		prefs.put(Prefs.CURRENT_DIR, currentDir.getAbsolutePath());
		prefs.putBoolean(Prefs.CACHE, cacheCheckBox.isSelected());
		prefs.put(Prefs.NAME, filenameContainsTextField.getText());
		
		int tabcount = tabs.getTabCount();
		for (int t = 0; t < tabcount; t++) {
			SearchJPanel p = (SearchJPanel) tabs.getComponentAt(t);
			p.savePrefs(prefs, t);
		}
		
		try {
			prefs.flush();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	private void initComponents () {
		addWindowListener(new SavePrefWA());
		
		cacheCheckBox.setToolTipText("Cache uncompressed files in memory");
		
		charsetComboBox.setModel(new DefaultComboBoxModel<>(LogSearchUtil.charsets()));
		charsetComboBox.setToolTipText("Character set to intepret files as");
		
		dirButton.addActionListener(e -> chooseDirs());
		editorButton.addActionListener(e -> chooseEditor());
		addTabItem.addActionListener(e -> addTab());
		removeTabItem.addActionListener(e -> removeTab());
		viewItem.addActionListener(e -> chooseAndView());
		
		optionsMenu.add(addTabItem);
		optionsMenu.add(removeTabItem);
		optionsMenu.add(viewItem);
		
		menuBar.add(optionsMenu);
		setJMenuBar(menuBar);
		
		JPanel p = new JPanel();
		p.add(dirLabel);
		p.add(dirButton);
		p.add(new JLabel("Filename"));
		p.add(filenameContainsTextField);
		p.add(new JLabel("Charset"));
		p.add(charsetComboBox);
		p.add(editorLabel);
		p.add(editorButton);
		p.add(cacheCheckBox);
		
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(p, BorderLayout.NORTH);
		p2.add(tabs, BorderLayout.CENTER);
		setContentPane(p2);
		
	}
	
	public File getEditorFile () {
		return editorFile;
	}
	
	public List<DirOpt> getDirs () {
		return dirs;
	}
	
	public boolean getCache () {
		return cacheCheckBox.isSelected();
	}
	
	public String getFilenameContains() {
		return filenameContainsTextField.getText();
	}
	
	private void removeTab () {
		int i = tabs.getSelectedIndex();
		tabs.remove(i);
	}
	
	private void addTab () {
		int c = tabs.getTabCount();
		tabs.addTab("Search" + (c + 1), new SearchJPanel());
		tabs.setSelectedIndex(c);
	}
	
	public Charset getCharset () {
		return (Charset) ((ComboItem) charsetComboBox.getSelectedItem()).object;
	}
	
	protected void chooseAndView () {
		try {
			JFileChooser f = new JFileChooser();
			if (currentDir.isDirectory()) {
				f.setCurrentDirectory(currentDir);
			}
			if (f.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				currentDir = f.getSelectedFile().getParentFile();
				ViewJFrame fr = new ViewJFrame(this, f.getSelectedFile(), getCharset(), "", false, false);
				fr.setVisible(true);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this, e.toString(), "View", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void updateDirsLabel () {
		long c = dirs.stream().filter(d -> d.enabled).count();
		dirLabel.setText(c + "/" + dirs.size());
	}
	
	public void updateTitle (SearchJPanel p, String msg) {
		if (p != null) {
			Integer i = Integer.valueOf(tabs.indexOfComponent(p) + 1);
			if (msg != null) {
				titles.put(i, msg);
			} else {
				titles.remove(i);
			}
		}
		String t = "LogSearch";
		if (msg != null && msg.length() > 0) {
			t += " [" + titles + "]";
		}
		setTitle(t);
	}
	
	private void chooseDirs () {
		DirectoryJDialog d = new DirectoryJDialog(this, "Log Directories");
		d.addDirs(dirs);
		d.setVisible(true);
		if (d.isOk()) {
			dirs.clear();
			dirs.addAll(d.getDirs());
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
	
	private class SavePrefWA extends WindowAdapter {
		@Override
		public void windowClosing (final WindowEvent e) {
			System.out.println("log search closing");
			savePrefs();
		}
	}
}
