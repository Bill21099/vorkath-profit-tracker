package com.vorkprofit.vorkath;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Small dragon-themed icon for the sidebar navigation button.
 * Drawn programmatically to keep the plugin self-contained.
 */
final class VorkathIcon
{
	private VorkathIcon() { }

	static BufferedImage create()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Body — deep blue dragon scales
		g.setColor(new Color(30, 60, 140));
		g.fillOval(2, 4, 12, 9);

		// Head
		g.setColor(new Color(40, 80, 180));
		g.fillOval(8, 2, 7, 7);

		// Snout / jaw
		g.setColor(new Color(60, 100, 200));
		g.fillRect(12, 6, 3, 3);

		// Eye — glowing cyan (undead Vorkath has blue eyes)
		g.setColor(new Color(0, 230, 230));
		g.fillOval(11, 3, 3, 3);
		g.setColor(Color.WHITE);
		g.fillOval(12, 4, 1, 1);

		// Wing outline
		g.setColor(new Color(20, 40, 110));
		g.fillPolygon(new int[]{3, 0, 5}, new int[]{5, 1, 8}, 3);

		// Highlight on body
		g.setColor(new Color(80, 130, 220, 160));
		g.fillOval(4, 5, 5, 4);

		g.dispose();
		return image;
	}
}

