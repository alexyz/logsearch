package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;

public class ViewJFrame extends JFrame {
	
	private final JTextArea textArea = new JTextArea();
	private final List<Object> highlights = new ArrayList<>();
	private final JCheckBox wrapCheckBox = new JCheckBox("Line Wrap");
	private final JSpinner positionSpinner = new JSpinner();
	private final JButton prevButton = new JButton("<");
	private final JButton nextButton = new JButton(">");
	private final JButton okButton = new JButton("OK");
	private final JScrollPane scroller = new JScrollPane(textArea);
	private final MappedByteBuffer map;
	private final byte[] buf = new byte[16384];
	private final JLabel lengthLabel = new JLabel();
	
	public ViewJFrame (final JFrame frame, final Result result) throws Exception {
		super(result.name);
		
		File file = LogSearchUtil.toFile(result);
		
		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			map = fc.map(MapMode.READ_ONLY, 0, Math.min(file.length(), Integer.MAX_VALUE));
		}
		
		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				map.clear();
			}
		});
		
		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int pos = map.position();
				for (Long l : result.offsets.descendingSet()) {
					if (l.longValue() < pos) {
						positionSpinner.setValue(l.intValue());
					}
				}
			}
		});
		
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int pos = map.position();
				for (Long l : result.offsets) {
					if (l.longValue() > pos) {
						positionSpinner.setValue(l.intValue());
					}
				}
			}
		});
		
		long val = result.offsets.iterator().next();
		if (val > Integer.MAX_VALUE) {
			val = 0;
		}
		
		positionSpinner.setModel(new SpinnerNumberModel((int) val, 0, map.limit(), 1));
		
		positionSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) positionSpinner.getValue();
				map.position(value.intValue());
				int rem = map.remaining();
				map.get(buf, 0, rem);
				textArea.setText(new String(buf, 0, rem, StandardCharsets.ISO_8859_1));
			}
		});
		
		lengthLabel.setText("Limit " + map.limit());
		
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
		topPanel.add(new JLabel("Position"));
		topPanel.add(positionSpinner);
		topPanel.add(lengthLabel);
		topPanel.add(prevButton);
		topPanel.add(nextButton);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(wrapCheckBox);
		buttonPanel.add(okButton);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(topPanel, BorderLayout.NORTH);
		contentPanel.add(scroller, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		setContentPane(contentPanel);
		setPreferredSize(new Dimension(640, 480));
		pack();
		setLocationRelativeTo(frame);
	}

}
