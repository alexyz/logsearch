package ls;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateJDialog extends JDialog {

    private final DateSpinnerJPanel datePanel = new DateSpinnerJPanel();
    private final JButton midnightButton = new JButton("Midnight");
    private final JButton okButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private boolean ok;

    public DateJDialog() {
        okButton.addActionListener(e -> close(true));
        cancelButton.addActionListener(e -> close(false));
        midnightButton.addActionListener(e -> midnight());

        JPanel p = new JPanel();
        p.add(midnightButton);
        p.add(okButton);
        p.add(cancelButton);

        setLayout(new BorderLayout());
        add(datePanel, BorderLayout.CENTER);
        add(p, BorderLayout.SOUTH);
        pack();
    }

    private void midnight() {
        Calendar c = new GregorianCalendar();
        Calendar c2 = new GregorianCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePanel.setTime(c2.getTime());
    }

    public DateSpinnerJPanel getDatePanel() {
        return datePanel;
    }

    public boolean isOk() {
        return ok;
    }

    private void close(boolean v) {
        ok = v;
        setVisible(false);
    }
}
