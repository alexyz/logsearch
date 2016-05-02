package ls;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * view large files cant use custom document/content - still need to scan for
 * lines
 */
public class ViewJFrame extends JFrame {
	
	private final JButton okButton = new JButton("OK");
	private final JTextArea textArea = new JTextArea();
	private final JScrollBar offsetScrollBar;
	private final RandomAccessFile raFile;
	private final JSpinner offsetSpinner = new JSpinner();
	private final List<Object> highlights = new ArrayList<>();
	private final Charset charset;
	private final byte[] buffer = new byte[16384];
	private final StringBuilder builder = new StringBuilder();
	private final JTextField searchTextField = new JTextField();
	private final JCheckBox ignoreCaseCheckBox = new JCheckBox("Ignore Case");
	private final JCheckBox regexCheckBox = new JCheckBox("Regex");
	private final JButton nextButton = new JButton("Next");
	private final JButton prevButton = new JButton("Previous");
	
	private long searchOffset;
	private boolean offsetAdjusting;
	
	// [text] [ci] [rx] previous next
	
	public ViewJFrame(final JFrame frame, File file, Charset charset, String text, boolean ignoreCase, boolean regex) throws Exception {
		super("View " + file.getName());
		this.charset = charset;
		this.raFile = new RandomAccessFile(file, "r");
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed (WindowEvent e) {
				try {
					raFile.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		offsetSpinner.setModel(new SpinnerNumberModel(0.0, 0, file.length(), 1));
		offsetSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged (ChangeEvent e) {
				if (!offsetAdjusting) {
					offsetAdjusting = true;
					double spinnerValue = ((Number) offsetSpinner.getValue()).doubleValue();
					searchOffset = (long) spinnerValue;
					int koffset = (int) (spinnerValue / 1000);
					System.out.println("spinner changed, update scroll bar to " + koffset);
					offsetScrollBar.setValue(koffset);
					update();
					offsetAdjusting = false;
				}
			}
		});
		
		searchTextField.setColumns(10);
		searchTextField.setText(text);
		
		ignoreCaseCheckBox.setSelected(ignoreCase);
		
		regexCheckBox.setSelected(regex);
		
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				search(true);
			}
		});
		
		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				search(false);
			}
		});
		
		textArea.setEditable(false);
		textArea.setFont(new Font("monospaced", 0, 12));
		textArea.setLineWrap(true);

		offsetScrollBar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, (int) (file.length() / 1000) + 1);
		offsetScrollBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged (AdjustmentEvent e) {
				if (!offsetAdjusting) {
					offsetAdjusting = true;
					int koffset = e.getValue();
					searchOffset = koffset * 1000L;
					Double spinnerValue = new Double(koffset * 1000.0);
					System.out.println("scroll changed, update spinner to " + spinnerValue);
					offsetSpinner.setValue(spinnerValue);
					update();
					offsetAdjusting = false;
				}
			}
		});
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				setVisible(false);
			}
		});
		
		JPanel topPanel1 = new JPanel();
		topPanel1.add(new JLabel("Offset"));
		topPanel1.add(offsetSpinner);
		topPanel1.add(new JLabel("Size " + NumberFormat.getIntegerInstance().format(raFile.length())));
		
		JPanel topPanel2 = new JPanel();
		topPanel2.add(new JLabel("Search"));
		topPanel2.add(searchTextField);
		topPanel2.add(ignoreCaseCheckBox);
		topPanel2.add(regexCheckBox);
		topPanel2.add(nextButton);
		topPanel2.add(prevButton);
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.add(topPanel1);
		topPanel.add(topPanel2);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(topPanel, BorderLayout.NORTH);
		contentPanel.add(textArea, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		contentPanel.add(offsetScrollBar, BorderLayout.EAST);
		
		setContentPane(contentPanel);
		update();
		setPreferredSize(new Dimension(640, 480));
		pack();
		setLocationRelativeTo(frame);
	}
	
	protected void search (boolean next) {
		try {
			final Pattern pattern = pattern();
			if (pattern == null) {
				JOptionPane.showMessageDialog(this, "No text");
				return;
			}
			
			final int length = searchTextField.getText().length();
			
			if (next) {
				searchOffset += length;
			} else {
				searchOffset = Math.max(0, searchOffset - buffer.length);
			}
			
			while (true) {
				System.out.println("seek " + searchOffset);
				raFile.seek(searchOffset);
				int read = raFile.read(buffer);
				String bufferStr = new String(buffer, 0, read, charset);
				
				int i = -1;
				Matcher m = pattern.matcher(bufferStr);
				// find the next or the last
				while (m.find()) {
					i = m.start();
					if (next) {
						break;
					}
				}
				
				if (i >= 0) {
					searchOffset += i;
					System.out.println("found at " + searchOffset);
					offsetAdjusting = true;
					int koffset = (int) (searchOffset / 1000);
					offsetSpinner.setValue(new Double(koffset * 1000.0));
					offsetScrollBar.setValue(koffset);
					offsetAdjusting = false;
					update();
					break;
					
				} else if (next) {
					searchOffset = searchOffset + buffer.length - length;
					if (searchOffset >= raFile.length()) {
						JOptionPane.showMessageDialog(this, "No further matches");
						break;
					}
					
				} else {
					// go back
					if (searchOffset == 0) {
						JOptionPane.showMessageDialog(this, "No previous matches");
						break;
					}
					searchOffset = Math.max(0, searchOffset - buffer.length + length);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Could not search: " + e);
		}
	}
	
	private Pattern pattern () {
		String text = searchTextField.getText();
		if (text.length() > 0) {
			final int flags = ignoreCaseCheckBox.isSelected() ? Pattern.CASE_INSENSITIVE : 0;
			boolean regex = regexCheckBox.isSelected();
			final Pattern pattern = Pattern.compile(regex ? text : Pattern.quote(text), flags);
			return pattern;
		} else {
			return null;
		}
	}
	
	protected void update () {
		try {
			Highlighter highlighter = textArea.getHighlighter();
			for (Object o : highlights) {
				highlighter.removeHighlight(o);
			}
			highlights.clear();
			
			// get text
			builder.delete(0, builder.length());
			List<int[]> controlOffsets = new ArrayList<>();
			raFile.seek(((Number) offsetSpinner.getValue()).longValue());
			int read = raFile.read(buffer);
			String bufStr = new String(buffer, 0, read, charset);
			for (int n = 0; n < bufStr.length(); n++) {
				char c = bufStr.charAt(n);
				if (c < 32) {
					int i = builder.length();
					builder.append(StringEscapeUtils.escapeJava(String.valueOf(c)));
					controlOffsets.add(new int[] { i, builder.length() });
				} else {
					builder.append(c);
				}
			}
			textArea.setText(builder.toString());
			textArea.setCaretPosition(0);
			
			DefaultHighlighter.DefaultHighlightPainter controlPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.lightGray);
			for (int[] a : controlOffsets) {
				highlights.add(highlighter.addHighlight(a[0], a[1], controlPainter));
			}
			
			Pattern pattern = pattern();
			if (pattern != null) {
				DefaultHighlighter.DefaultHighlightPainter searchPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.orange);
				Matcher matcher = pattern.matcher(builder);
				while (matcher.find()) {
					int s = matcher.start();
					int e = matcher.end();
					highlights.add(highlighter.addHighlight(s, e, searchPainter));
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			textArea.setText(e.toString());
		}
	}
	
}
