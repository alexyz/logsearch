package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.text.*;

public class TextJDialog extends JDialog {
	
	private static final String WRAP_PREF = "wrap";
	
	private final JTextArea textArea = new JTextArea();
	private final List<Object> highlights = new ArrayList<>();
	private final JCheckBox wrapCheckBox = new JCheckBox("Line Wrap");
	
	public TextJDialog (JFrame frame, String title) {
		super(frame, title, ModalityType.DOCUMENT_MODAL);
		
		textArea.setEditable(false);
		JScrollPane scroller = new JScrollPane(textArea);
		
		wrapCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged (ItemEvent e) {
				textArea.setLineWrap(wrapCheckBox.isSelected());
			}
		});
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				savePrefs();
				setVisible(false);
			}
		});
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(wrapCheckBox);
		buttonPanel.add(okButton);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(scroller, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		setContentPane(contentPanel);
		setPreferredSize(new Dimension(640, 480));
		pack();
		setLocationRelativeTo(frame);
		loadPrefs();
	}

	public void setTextFont(Font font) {
		textArea.setFont(font);
	}
	
	public void setText (String text) {
		textArea.setText(text);
		textArea.setCaretPosition(0);
	}
	
	public void setHighlight(Pattern pattern, Color col) {
		Highlighter h = textArea.getHighlighter();
		for (Object o : highlights) {
			h.removeHighlight(o);
		}
		highlights.clear();
		if (pattern != null) {
			String text = textArea.getText();
			Matcher matcher = pattern.matcher(text);
			try {
				while (matcher.find()) {
					int s = matcher.start();
					int e = matcher.end();
					highlights.add(h.addHighlight(s, e, new DefaultHighlighter.DefaultHighlightPainter(col)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void loadPrefs () {
		Preferences prefs = Preferences.userNodeForPackage(TextJDialog.class);
		wrapCheckBox.setSelected(prefs.getBoolean(WRAP_PREF, true));
	}
	
	private void savePrefs () {
		Preferences prefs = Preferences.userNodeForPackage(TextJDialog.class);
		prefs.putBoolean(WRAP_PREF, wrapCheckBox.isSelected());
		try {
			prefs.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
