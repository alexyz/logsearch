package ls;

import java.awt.*;

/**
 * flow layout with a preferred size that actually includes rows after the first
 */
public class FlowLayout2 extends FlowLayout {
	@Override
	public Dimension preferredLayoutSize (final Container cont) {
		System.out.println("container size " + cont.getSize() + " parent " + cont.getParent().getClass() + " size " + cont.getParent().getSize());
		double x = 0, y = 0, totalx = 0, totaly = 0;

		for (int n = 0; n < cont.getComponentCount(); n++) {
			final Component c = cont.getComponent(n);
			final Dimension s = c.getPreferredSize();
			final double w = s.getWidth() + getHgap();
			final double h = s.getHeight() + getVgap();
			if (x == 0 || x + w < cont.getWidth()) {
				x += w;
				y = Math.max(y, h);
			} else {
				totalx = Math.max(x, totalx);
				totaly += y;
				x = w;
				y = h;
			}
		}

		final Insets i = cont.getInsets();
		totalx += x + i.right + getHgap();
		totaly += y + i.bottom + getVgap();
		System.out.println("preferred layout size " + totalx + "," + totaly);
		return new Dimension((int) totalx, (int) totaly);
	}

}
