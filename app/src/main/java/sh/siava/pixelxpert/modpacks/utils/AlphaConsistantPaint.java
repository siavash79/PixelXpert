package sh.siava.pixelxpert.modpacks.utils;

import android.graphics.Paint;

/** When setting a paint's color, alpha gets reset... naturally. So this is kind of paint that remembers its alpha and keeps it intact
 */
public class AlphaConsistantPaint extends Paint {
	public AlphaConsistantPaint(int flag) {
		super(flag);
	}

	@Override
	public void setColor(int color)
	{
		int alpha = getAlpha();

		super.setColor(color);
		setAlpha(alpha);
	}
}
