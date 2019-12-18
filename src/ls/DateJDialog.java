package ls;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateJDialog extends JDialog {

    private final DateSpinnerJPanel datePanel = new DateSpinnerJPanel();
    private final JButton nowButton = new JButton("Today");
    private final JButton okButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private boolean ok;

    public DateJDialog() {
        okButton.addActionListener(e -> close(true));
        cancelButton.addActionListener(e -> close(false));
        nowButton.addActionListener(e -> now());

        JPanel p = new JPanel();
        p.add(nowButton);
        p.add(okButton);
        p.add(cancelButton);

        setLayout(new BorderLayout());
        add(datePanel, BorderLayout.CENTER);
        add(p, BorderLayout.SOUTH);
        pack();
    }

    private void now() {
        Calendar c = new GregorianCalendar();
        Calendar c2 = new GregorianCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePanel.setDate(c2.getTime());
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
