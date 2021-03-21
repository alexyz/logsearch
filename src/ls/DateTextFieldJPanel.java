package ls;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class DateTextFieldJPanel extends JPanel {

    // text field followed by button

    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private final JTextField dateField = new JTextField();
    private final JButton editButton = new JButton("...");
    private final String title;

    public DateTextFieldJPanel(String title) {
        this.title = title;
        float f = dateFormat.format(new Date()).length() * 0.66f;
        dateField.setColumns((int)f);
        editButton.addActionListener(e -> openDialog());
        editButton.setPreferredSize(new Dimension(16,16));
        //editButton.setFont(editButton.getFont().deriveFont(8f));
        add(dateField);
        add(editButton);
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        dateField.setEnabled(b);
        editButton.setEnabled(b);
    }

    private void openDialog() {
        DateJDialog d = new DateJDialog();
        d.setLocationRelativeTo(this);
        d.setTitle(title);
        long v = getTime();
        d.getDatePanel().setDate(v != -1 ? new Date(v) : null);
        d.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        d.setVisible(true);
        if (d.isOk()) {
            System.out.println("dtfp set date " + d.getDatePanel().getDate());
            setDate(d.getDatePanel().getDate());
        }
    }

    public void setDate(Date v) {
        if (v != null && v.getTime() != -1) {
            dateField.setText(dateFormat.format(v));
        } else {
            dateField.setText("");
        }
    }

    public Date getDate() throws ParseException {
        String text = dateField.getText().trim();
        if (text.length() > 0) {
            return dateFormat.parse(text);
        } else {
            return null;
        }
    }

    /** get time in milliseconds, -1 if blank */
    public long getTime() {
        try {
            Date d = getDate();
            if (d != null) {
                return d.getTime();
            }
        } catch (ParseException e) {
            System.out.println("could not parse: " + e);
        }
        return -1;
    }
}
