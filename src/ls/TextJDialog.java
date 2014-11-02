package ls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class TextJDialog extends JDialog implements ActionListener {
	public TextJDialog (JFrame frame, String title, String text) {
		super(frame, title, ModalityType.DOCUMENT_MODAL);
		JTextArea textArea = new JTextArea();
		textArea.setText(text);
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
	
	@Override
	public void actionPerformed (ActionEvent e) {
		setVisible(false);
	}
}
