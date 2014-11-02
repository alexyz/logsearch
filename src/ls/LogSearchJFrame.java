package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

public class LogSearchJFrame extends JFrame {
	
	public static final LogSearchJFrame instance = new LogSearchJFrame();
	
	public static void main (String[] args) {
		instance.setVisible(true);
	}
	
	private final JTextField dirField = new JTextField();
	private final JButton dirButton = new JButton("...");
	private final JTextField nameField = new JTextField();
	private final JTextField searchField = new JTextField();
	private final JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(7, 1, 999, 1));
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JButton openButton = new JButton("Open");
	private final ResultTableModel tableModel = new ResultTableModel();
	private final JTable table = new JTable(tableModel);
	private final JButton editorButton = new JButton("...");
	private final JLabel editorLabel = new JLabel();
	private final JButton previewButton = new JButton("Preview");
	
	private File editor;
	
	public LogSearchJFrame () {
		super("LogSearch");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		load();
		listeners();
		setContentPane(content());
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
			ageSpinner.setValue(Integer.parseInt(p.getProperty("age", "7")));
			editor = new File(p.getProperty("editor", defaultEditor().getAbsolutePath()));
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
		p.setProperty("age", String.valueOf(ageSpinner.getValue()));
		File f = new File(System.getProperty("user.home") + File.separator + "LogSearch.properties");
		try (OutputStream os = new FileOutputStream(f)) {
			p.store(os, null);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.toString(), "Save Properties", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public void add (final Result fd) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				tableModel.add(fd);
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
					file = ungzip(result.file);
				} else if (result.file.getName().toLowerCase().endsWith(".zip")) {
					file = unzip(result.file, result.entry);
				} else {
					file = result.file;
				}
				Runtime.getRuntime().exec(new String[] { editor.getAbsolutePath(), file.getAbsolutePath() });
			}
		}
	}
	
	private static File unzip (File zipfile, String entry) throws Exception {
		System.out.println("unzip " + zipfile + ", " + entry);
		try (ZipFile zf = new ZipFile(zipfile)) {
			ZipEntry ze = zf.getEntry(entry);
			try (InputStream is = zf.getInputStream(ze)) {
				return copy(is, zipfile.getName() + "." + ze.getName());
			}
		}
	}
	
	private static File ungzip (File gzfile) throws Exception {
		System.out.println("ungzip " + gzfile);
		try (InputStream is = new GZIPInputStream(new FileInputStream(gzfile))) {
			return copy(is, gzfile.getName());
		}
	}
	
	private static File copy (InputStream is, String tmpname) throws Exception {
		File file = File.createTempFile(tmpname + ".", null);
		file.deleteOnExit();
		try (OutputStream os = new FileOutputStream(file)) {
			byte[] buf = new byte[65536];
			int l;
			while ((l = is.read(buf)) != -1) {
				os.write(buf, 0, l);
			}
		}
		return file;
	}
	
	private static File defaultEditor () {
		Map<String, String> env = System.getenv();
		String pf86 = env.get("ProgramFiles(x86)");
		String windir = env.get("windir");
		File npp = new File(pf86 + "\\Notepad++\\notepad++.exe");
		if (npp.exists()) {
			return npp;
		}
		return new File(windir + "\\notepad.exe");
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
		Calendar startCal = new GregorianCalendar();
		startCal.add(Calendar.DATE, -(int) ageSpinner.getValue());
		final Date startDate = startCal.getTime();
		final String name = nameField.getText();
		if (name.length() == 0) {
			System.out.println("no name filter");
			return;
		}
		
		save();
		
		FindThread ft = new FindThread(dir, startDate, name, text);
		FindThread.running = true;
		ft.start();
	}
	
	private JPanel content () {
		dirField.setColumns(30);
		nameField.setColumns(10);
		searchField.setColumns(20);
		
		JPanel p = new JPanel();
		p.add(new JLabel("Directory"));
		p.add(dirField);
		p.add(dirButton);
		p.add(new JLabel("Name Contains"));
		p.add(nameField);
		
		JPanel p3 = new JPanel();
		p3.add(new JLabel("File Contains"));
		p3.add(searchField);
		p3.add(new JLabel("Max Age"));
		p3.add(ageSpinner);
		p3.add(startButton);
		p3.add(stopButton);
		
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
}
