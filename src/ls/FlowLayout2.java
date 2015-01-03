package ls;

import java.awt.*;

/**
 * flow layout with a preferred size that actually includes rows after the first
 */
public class FlowLayout2 extends FlowLayout {
	@Override
	public Dimension preferredLayoutSize (Container target) {
		System.out.println("preferred layout size");
		int x = 0;
		int y = 0;
		for (int n = 0; n < target.getComponentCount(); n++) {
			Component c = target.getComponent(n);
			x = Math.max(x, c.getX() + c.getWidth());
			y = Math.max(y, c.getY() + c.getHeight());
		}
		System.out.println("max " + x + "," + y);
		return new Dimension(x, y);
	}
}
