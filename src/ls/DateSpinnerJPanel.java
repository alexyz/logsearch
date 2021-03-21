package ls;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/** date editor panel with spinners */
public class DateSpinnerJPanel extends JPanel {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setContentPane(new DateSpinnerJPanel());
        f.pack();
        f.setVisible(true);
    }

    private final JCheckBox enableBox = new JCheckBox("Enable");
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(2000, 1900, 2100, 1));
    private final JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
    private final JSpinner dateSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
    private final JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
    private final JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
    private final JLabel label = new JLabel();
    private final JSpinner[] spinners = {yearSpinner, monthSpinner, dateSpinner, hourSpinner, minuteSpinner};

    public DateSpinnerJPanel() {
        setLayout(new GridLayout(2, 1));

        enableBox.addChangeListener(e -> updateSpinners());
        for (JSpinner s : spinners) {
            s.addChangeListener(e -> updateLabel());
        }

        JPanel p1 = new JPanel();
        p1.add(enableBox);
        p1.add(new JLabel("Year"));
        p1.add(yearSpinner);
        p1.add(new JLabel("Month"));
        p1.add(monthSpinner);
        p1.add(new JLabel("Day"));
        p1.add(dateSpinner);
        p1.add(new JLabel("Hour"));
        p1.add(hourSpinner);
        p1.add(new JLabel("Min"));
        p1.add(minuteSpinner);

        JPanel p2 = new JPanel();
        p2.add(label);

        updateLabel();

        add(p1);
        add(p2);
    }

    public Date getDate() {
        if (enableBox.isSelected()) {
            int y = getV(yearSpinner), m = getV(monthSpinner) - 1, d = getV(dateSpinner), h = getV(hourSpinner), min = getV(minuteSpinner);
            GregorianCalendar c = new GregorianCalendar(y, m, d, h, min);
            return c.getTime();
        } else {
            return null;
        }
    }

    public void setDate(Date date) {
        enableBox.setSelected(date != null);
        if (date != null) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTimeInMillis(date.getTime());
            setV(yearSpinner, c.get(Calendar.YEAR));
            setV(monthSpinner, c.get(Calendar.MONTH) + 1);
            setV(dateSpinner, c.get(Calendar.DAY_OF_MONTH));
            setV(hourSpinner, c.get(Calendar.HOUR_OF_DAY));
            setV(minuteSpinner, c.get(Calendar.MINUTE));
        }
        updateSpinners();
    }

    private void updateSpinners() {
        for (JSpinner s : spinners) {
            s.setEnabled(enableBox.isSelected());
        }
        updateLabel();
    }

    private void updateLabel () {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        Date date = getDate();
        label.setText(date != null ? df.format(date) : "");
    }

    private static void setV(JSpinner s, int v) {
        s.getModel().setValue(Integer.valueOf(v));
    }

    private static int getV(JSpinner s) {
        return ((SpinnerNumberModel)s.getModel()).getNumber().intValue();
    }
}
