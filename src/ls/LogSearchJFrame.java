package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

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
	private final FDTM tableModel = new FDTM();
	private final JTable table = new JTable(tableModel);
	
	public LogSearchJFrame () {
		super("LogSearch");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		dirField.setText(System.getProperty("user.dir"));
		nameField.setText("server.log");
		searchField.setText("a");
		
		listeners();
		setContentPane(content());
		setPreferredSize(new Dimension(800, 600));
		pack();
	}
	
	public void add (final FD fd) {
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
		
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				start();
			}
		});
		
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				FT.running = false;
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
	}
	
	private void open () throws Exception {
		int r = table.getSelectedRow();
		if (r >= 0) {
			FD fd = tableModel.getFD(r);
			File file;
			if (fd.file.getName().toLowerCase().endsWith(".gz")) {
				file = ungzip(fd.file);
			} else if (fd.file.getName().toLowerCase().endsWith(".zip")) {
				file = unzip(fd.file, fd.entry);
			} else {
				file = fd.file;
			}
			Runtime.getRuntime().exec(new String[] { getEditor().getAbsolutePath(), file.getAbsolutePath() });
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
	
	private static File getEditor () {
		Map<String, String> env = System.getenv();
		String pf86 = env.get("ProgramFiles(x86)");
		String windir = env.get("windir");
		File npp = new File(pf86 + "\\Notepad++\\notepad++.exe");
		if (npp.exists()) {
			return npp;
		}
		return new File(windir + "\\notepad.exe");
	}
	
	private void start () {
		if (FT.running) {
			System.out.println("already running");
			return;
		}
		final String text = searchField.getText();
		if (text.length() == 0) {
			System.out.println("no search text");
			return;
		}
		final File dir = new File(dirField.getText());
		Calendar startCal = new GregorianCalendar();
		startCal.add(Calendar.DATE, -(int) ageSpinner.getValue());
		final Date startDate = startCal.getTime();
		final String name = nameField.getText();
		
		tableModel.clear();
		
		FT ft = new FT(dir, startDate, name, text);
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

class FD implements Comparable<FD> {
	public final int lines;
	public final Date date;
	public final String name;
	public final File file;
	public final String entry;
	
	public FD (String name, Date date, File file, String entry, int lines) {
		this.name = name;
		this.file = file;
		this.date = date;
		this.entry = entry;
		this.lines = lines;
	}
	
	@Override
	public int compareTo (FD o) {
		int c = date.compareTo(o.date);
		if (c == 0) {
			return name.compareToIgnoreCase(o.name);
		}
		return -c;
	}
	
}

class FDTM extends AbstractTableModel {
	
	private final List<FD> list = new ArrayList<>();
	
	public void clear () {
		list.clear();
		fireTableDataChanged();
	}
	
	public void add (FD fd) {
		list.add(fd);
		Collections.sort(list);
		fireTableDataChanged();
	}
	
	public FD getFD (int row) {
		return list.get(row);
	}
	
	@Override
	public int getRowCount () {
		return list.size();
	}
	
	@Override
	public int getColumnCount () {
		return 3;
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0:
				return "Name";
			case 1:
				return "Date";
			case 2:
				return "Matches";
		}
		return null;
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		FD fd = list.get(row);
		switch (col) {
			case 0:
				return fd.name;
			case 1:
				return DateFormat.getDateTimeInstance().format(fd.date);
			case 2:
				return fd.lines != 0 ? fd.lines : "";
		}
		return null;
	}
	
}

class FT extends Thread {
	public static volatile boolean running;
	private final File dir;
	private final Date startDate;
	private final String name;
	private final String text;
	
	public FT (File dir, Date startDate, String name, String text) {
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		this.dir = dir;
		this.startDate = startDate;
		this.name = name;
		this.text = text;
	}
	
	@Override
	public void run () {
		running = true;
		try {
			find(dir);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(LogSearchJFrame.instance, e.toString(), "Could not search", JOptionPane.ERROR_MESSAGE);
		}
		running = false;
	}
	
	private int scan (File f) throws Exception {
		System.out.println("scan " + f);
		try (InputStream is = new FileInputStream(f)) {
			if (f.getName().endsWith(".gz")) {
				try (InputStream is2 = new GZIPInputStream(is)) {
					return scan(is2);
				}
			} else {
				return scan(is);
			}
		}
	}
	
	private int scan (InputStream is) throws Exception {
		int lines = 0;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String l;
			while ((l = br.readLine()) != null) {
				if (!running) {
					break;
				}
				if (l.contains(text)) {
					System.out.println("matched line " + l);
					lines++;
				}
			}
		}
		return lines;
	}
	
	private void find (File dir) throws Exception {
		for (File file : dir.listFiles()) {
			if (!running) {
				break;
			}
			if (file.isFile()) {
				if (file.getName().endsWith(".zip")) {
					findZip(file);
				} else if (file.getName().contains(name)) {
					findFile(file);
				}
			} else if (file.isDirectory()) {
				find(file);
			}
		}
	}
	
	private void findFile (File file) throws Exception {
		Date date = getDate(file.getName());
		if (date == null) {
			date = new Date(file.lastModified());
		}
		if (date.after(startDate)) {
			int lines = scan(file);
			LogSearchJFrame.instance.add(new FD(file.getName(), date, file, null, lines));
		}
	}
	
	private void findZip (File file) throws Exception {
		try (ZipFile zf = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> e = zf.entries();
			while (e.hasMoreElements()) {
				if (!running) {
					break;
				}
				ZipEntry ze = e.nextElement();
				String filename = ze.getName();
				if (filename.contains("/")) {
					filename = filename.substring(filename.lastIndexOf("/") + 1);
				}
				if (filename.contains(name)) {
					Date date = getDate(filename);
					if (date == null) {
						long t = ze.getTime();
						if (t > 0) {
							date = new Date(t);
						}
					}
					if (date != null && date.after(startDate)) {
						try (InputStream is = zf.getInputStream(ze)) {
							int lines = scan(is);
							LogSearchJFrame.instance.add(new FD(filename, date, file, ze.getName(), lines));
						}
					}
				}
			}
		}
	}
	
	private static Date getDate (String name) {
		Pattern datePat = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
		Date date = null;
		if (name.length() > 0) {
			Matcher mat = datePat.matcher(name);
			if (mat.find()) {
				String dateStr = mat.group(1);
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				try {
					date = df.parse(dateStr);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return date;
	}
	
}