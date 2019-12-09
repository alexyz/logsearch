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

    public DateTextFieldJPanel() {
        float f = dateFormat.format(new Date()).length() * 0.66f;
        dateField.setColumns((int)f);
        editButton.addActionListener(e -> openDialog());
        editButton.setPreferredSize(new Dimension(16,16));
        editButton.setFont(editButton.getFont().deriveFont(8));
        add(dateField);
        add(editButton);
    }

    private void openDialog() {
        DateJDialog d = new DateJDialog();
        d.setLocationRelativeTo(this);
        long v = getTime();
        if (v >= 0) {
            d.getDatePanel().setTime(new Date(v));
        }
        d.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        d.setVisible(true);
        if (d.isOk()) {
            System.out.println("dtfp set date " + d.getDatePanel().getTime());
            setDate(d.getDatePanel().getTime());
        }
    }

    public void setDate(Date v) {
        if (v != null) {
            dateField.setText(dateFormat.format(v));
        } else {
            dateField.setText("");
        }
    }

    public Date getDate() throws ParseException {
        if (dateField.getText().length() > 0) {
            return dateFormat.parse(dateField.getText());
        } else {
            return null;
        }
    }

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
