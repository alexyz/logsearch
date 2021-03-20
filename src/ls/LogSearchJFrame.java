package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import static ls.LogSearchUtil.*;

public class LogSearchJFrame extends JFrame  {
	
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
	private final JButton editorButton = new JButton("Editor...");
	private final JLabel editorLabel = new JLabel();
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final List<DirOpt> dirs = new ArrayList<>();
	private final JMenuBar menuBar = new JMenuBar();
	private final JTabbedPane tabs = new JTabbedPane();

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
		dirs.addAll(DirOpt.stringToDirs(prefs.get(DIR_PREF, userdir), true, true));
		dirs.addAll(DirOpt.stringToDirs(prefs.get(DIS_DIR_PREF, ""), false, true));
		dirs.addAll(DirOpt.stringToDirs(prefs.get(NR_DIR_PREF, ""), true, false));
		dirs.addAll(DirOpt.stringToDirs(prefs.get(NR_DIS_DIR_PREF, ""), false, false));
		updateDirsLabel();

		
		File defaultEditor = defaultEditor();
		String editorStr = prefs.get(EDITOR_PREF, defaultEditor != null ? defaultEditor.getAbsolutePath() : null);
		File editor = editorStr != null && editorStr.length() > 0 ? new File(editorStr) : null;
		editorFile = editor != null && editor.exists() ? editor : null;
		
		editorLabel.setText(editor != null ? editor.getName() : "no editor");

		

		
		currentDir = new File(prefs.get(CD_PREF, userdir));

	}

	private void savePrefs () {
		System.out.println("save prefs");
		prefs.put(DIR_PREF, DirOpt.dirsToString(dirs,true,true));
		prefs.put(DIS_DIR_PREF, DirOpt.dirsToString(dirs,false,true));
		prefs.put(NR_DIR_PREF, DirOpt.dirsToString(dirs,true,false));
		prefs.put(NR_DIS_DIR_PREF, DirOpt.dirsToString(dirs,false,false));
		prefs.put(EDITOR_PREF, editorFile != null ? editorFile.getAbsolutePath() : "");
		prefs.put(CD_PREF, currentDir.getAbsolutePath());
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

	}
	

	
	private void initComponents () {
		
		JMenuItem viewItem = new JMenuItem("Open Large File...");
		viewItem.addActionListener(e -> chooseAndView());
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(viewItem);

		menuBar.add(fileMenu);
		setJMenuBar(menuBar);

		// TODO options panel
		// dirs: 1/6 [...] - maybe you want different dirs for each tab?
		// editor: notepad.exe [...]
		// charset: [utf8] - maybe dir specific

		// menu: add tab, close tab, open large file

		setContentPane(tabs);
		
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
		long c = dirs.stream().filter(d -> d.enabled).count();
		dirLabel.setText(c + "/" + dirs.size());
	}

	private void updateTitle (String msg) {
		String t = "LogSearch";
		if (msg != null && msg.length() > 0) {
			t += " [" + msg + "]";
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
	
}
