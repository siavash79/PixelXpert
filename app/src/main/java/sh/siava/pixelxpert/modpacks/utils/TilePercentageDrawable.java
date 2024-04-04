package sh.siava.pixelxpert.modpacks.utils;

import static android.graphics.Bitmap.createBitmap;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TilePercentageDrawable extends Drawable {
	final Drawable shape;
	private int currentPct = 0;

	@SuppressLint({"UseCompatLoadingForDrawables", "DiscouragedApi"})
	public TilePercentageDrawable(Context context) {
		shape = context.getDrawable(context.getResources().getIdentifier("qs_tile_background_shape", "drawable", context.getPackageName()));
	}

	public void setPct(int pct)
	{
		currentPct = pct;
	}

	@Override
	public void setBounds(Rect bounds) {
		shape.setBounds(bounds);
	}

	@Override
	public void setBounds(int a, int b, int c, int d) {
		shape.setBounds(a, b, c, d);
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		try {
			Bitmap bitmap = createBitmap(round(shape.getBounds().width() * currentPct / 100f), shape.getBounds().height(), Bitmap.Config.ARGB_8888);
			Canvas tempCanvas = new Canvas(bitmap);
			shape.draw(tempCanvas);

			canvas.drawBitmap(bitmap, 0, 0, new Paint());
		}
		catch (Throwable ignored){}
	}

	@Override
	public void setAlpha(int i) {
		shape.setAlpha(i);
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		shape.setColorFilter(colorFilter);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}

	@Override
	public void setTint(int t) {
		shape.setTint(t);
	}
}