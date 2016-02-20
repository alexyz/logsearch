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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/**
 * view large files
 * cant use custom document/content - still need to scan for lines
 */
public class ViewJFrame extends JFrame {

	private final JCheckBox wrapCheckBox = new JCheckBox("Line Wrap");
	private final JButton okButton = new JButton("OK");
	private final JTextArea textArea = new JTextArea();
	private final JScrollPane textAreaScroller = new JScrollPane(textArea);
	private final JScrollBar offsetScrollBar;
	private final RandomAccessFile randomAccessFile;
	private final JSpinner offsetSpinner = new JSpinner();
	private final JComboBox<Long> offsetsCombobox = new JComboBox<>();
	private final List<Object> highlights = new ArrayList<>();
	private final Pattern pattern;
	private final Charset charset;

	public ViewJFrame (final JFrame frame, final Result result, Pattern pattern, Charset charset) throws Exception {
		super(result.name);
		this.charset = charset;
		this.pattern = pattern;

		File file = LogSearchUtil.toTempFile(result);

		randomAccessFile = new RandomAccessFile(file, "r");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed (WindowEvent e) {
				try {
					randomAccessFile.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		offsetScrollBar = new JScrollBar(Adjustable.VERTICAL, 0, 1, 0, (int) (randomAccessFile.length() / 1000));
		offsetScrollBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged (AdjustmentEvent e) {
				offsetSpinner.setValue(new Double(e.getValue()) * 1000);
				update();
			}
		});

		offsetSpinner.setModel(new SpinnerNumberModel(0.0, 0, randomAccessFile.length(), 1));
		offsetSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged (ChangeEvent e) {
				offsetScrollBar.setValue((int)((Number)offsetSpinner.getValue()).doubleValue()/1000);
				update();
			}
		});

		Vector<Long> offsets = new Vector<>();
		offsets.add(new Long(0));
		offsets.addAll(result.offsets);
		
		offsetsCombobox.setModel(new DefaultComboBoxModel<>(offsets));
		offsetsCombobox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged (ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					offsetSpinner.setValue(((Number)e.getItem()).doubleValue());
				}
			}
		});

		textArea.setEditable(false);
		textArea.setFont(new Font("monospaced", 0, 12));
		
		wrapCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged (ItemEvent e) {
				textArea.setLineWrap(wrapCheckBox.isSelected());
			}
		});

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				setVisible(false);
			}
		});

		JPanel topPanel = new JPanel();
		topPanel.add(new JLabel("Offset"));
		topPanel.add(offsetSpinner);
		topPanel.add(offsetsCombobox);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(wrapCheckBox);
		buttonPanel.add(okButton);

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(topPanel, BorderLayout.NORTH);
		contentPanel.add(textAreaScroller, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		contentPanel.add(offsetScrollBar, BorderLayout.EAST);
		
		setContentPane(contentPanel);
		update();
		setPreferredSize(new Dimension(640, 480));
		pack();
		setLocationRelativeTo(frame);
	}

	protected void update () {
		try {
			byte[] buf = new byte[10000];
			randomAccessFile.seek(((Number)offsetSpinner.getValue()).longValue()); 
			randomAccessFile.read(buf);
			String text = new String(buf, charset);
			textArea.setText(text);
			textArea.setCaretPosition(0);
			Highlighter h = textArea.getHighlighter();
			for (Object o : highlights) {
				h.removeHighlight(o);
			}
			highlights.clear();
			if (pattern != null) {
				Matcher matcher = pattern.matcher(text);
				DefaultHighlighter.DefaultHighlightPainter p = new DefaultHighlighter.DefaultHighlightPainter(Color.orange);
				while (matcher.find()) {
					int s = matcher.start();
					int e = matcher.end();
					highlights.add(h.addHighlight(s, e, p));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
