package sh.siava.AOSPMods.modpacks.utils;

import android.graphics.Paint;

//When setting a paint's color, alpha gets reset... naturally. So we refresh the alpha in this class
public class AlphaRefreshedPaint extends Paint {
	public AlphaRefreshedPaint(int flag) {
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
