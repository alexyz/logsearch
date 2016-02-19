package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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
 * 
 */
public class ViewJFrame extends JFrame {

	private final JCheckBox wrapCheckBox = new JCheckBox("Line Wrap");
	private final JButton okButton = new JButton("OK");
	private final JTextArea textArea = new JTextArea();
	private final JScrollPane scroller = new JScrollPane(textArea);
	private final JScrollBar bar;
	private final RandomAccessFile raf;
	private final JSpinner spinner = new JSpinner();
	private final JComboBox<Long> combo = new JComboBox<>();
	private final List<Object> highlights = new ArrayList<>();
	private final Pattern pattern;

	public ViewJFrame (final JFrame frame, final Result result, Pattern pattern) throws Exception {
		super(result.name);
		this.pattern = pattern;

		File file = LogSearchUtil.toTempFile(result);

		raf = new RandomAccessFile(file, "r");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed (WindowEvent e) {
				try {
					raf.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		bar = new JScrollBar(JScrollBar.VERTICAL, 0, 1, 0, (int) (raf.length() / 1000));
		bar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged (AdjustmentEvent e) {
				spinner.setValue(new Double(e.getValue()) * 1000);
				update();
			}
		});

		spinner.setModel(new SpinnerNumberModel(0.0, 0, raf.length(), 1));
		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged (ChangeEvent e) {
				bar.setValue((int)((Number)spinner.getValue()).doubleValue()/1000);
				update();
			}
		});

		Vector<Long> offsets = new Vector<>();
		offsets.add(new Long(0));
		offsets.addAll(result.offsets);
		combo.setModel(new DefaultComboBoxModel<>(offsets));
		combo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged (ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					spinner.setValue(((Number)e.getItem()).doubleValue());
				}
			}
		});

		textArea.setEditable(false);
		textArea.setFont(new Font("monospaced", 0, 12));
		textArea.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized (ComponentEvent e) {
				update();
			}
		});

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
		topPanel.add(spinner);
		topPanel.add(combo);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(wrapCheckBox);
		buttonPanel.add(okButton);

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(topPanel, BorderLayout.NORTH);
		contentPanel.add(scroller, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		contentPanel.add(bar, BorderLayout.EAST);

		setContentPane(contentPanel);
		setPreferredSize(new Dimension(640, 480));
		pack();
		setLocationRelativeTo(frame);
	}

	protected void update () {
		try {
			byte[] buf = new byte[10000];
			raf.seek(((Number)spinner.getValue()).longValue()); 
			raf.read(buf);
			String text = new String(buf, StandardCharsets.ISO_8859_1);
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
