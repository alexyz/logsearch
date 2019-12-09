package ls;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

public class StartEndDateJDialog extends JDialog {

    public static void main(String[] args) {
        StartEndDateJDialog d = new StartEndDateJDialog();
        d.getStartDatePanel().setTime(new Date());
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        d.setLocationRelativeTo(null);
        d.setVisible(true);
    }

    private final DateSpinnerJPanel startDatePanel = new DateSpinnerJPanel();
    private final DateSpinnerJPanel endDatePanel = new DateSpinnerJPanel();
    private final JButton okButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private boolean ok;

    public StartEndDateJDialog() {
        startDatePanel.setBorder(new TitledBorder("Start Date"));
        endDatePanel.setBorder(new TitledBorder("End Date"));
        okButton.addActionListener(e -> close(false));
        cancelButton.addActionListener(e -> close(true));

        JPanel p = new JPanel();
        p.add(okButton);
        p.add(cancelButton);

        add(startDatePanel);
        add(endDatePanel);
        add(p);

        setLayout(new GridLayout(3, 1));
        pack();
    }

    public DateSpinnerJPanel getStartDatePanel() {
        return startDatePanel;
    }

    public DateSpinnerJPanel getEndDatePanel() {
        return endDatePanel;
    }

    public boolean getOk() {
        return ok;
    }

    private void close(boolean v) {
        ok = v;
        setVisible(false);
    }
}
