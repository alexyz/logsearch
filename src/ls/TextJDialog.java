package ls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

public class TextJDialog extends JDialog implements ActionListener {
	
	private final JTextArea textArea = new JTextArea();
	private final List<Object> highlights = new ArrayList<>();
	
	public TextJDialog (JFrame frame, String title) {
		super(frame, title, ModalityType.DOCUMENT_MODAL);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setCaretPosition(0);
		JScrollPane scroller = new JScrollPane(textArea);
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(scroller, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		setContentPane(contentPanel);
		setPreferredSize(new Dimension(640, 480));
		pack();
		setLocationRelativeTo(frame);
	}
	
	public void setTextFont(Font font) {
		textArea.setFont(font);
	}
	
	public void setText (String text) {
		textArea.setText(text);
	}
	
	public void setHighlight(String hlText, Color col) {
		Highlighter h = textArea.getHighlighter();
		for (Object o : highlights) {
			h.removeHighlight(o);
		}
		highlights.clear();
		if (hlText.length() > 0) {
			int i = -1;
			String text = textArea.getText();
			try {
				while ((i = text.indexOf(hlText, i + 1)) >= 0) {
					highlights.add(h.addHighlight(i, i + hlText.length(), new DefaultHighlighter.DefaultHighlightPainter(col)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void actionPerformed (ActionEvent e) {
		setVisible(false);
	}
}
