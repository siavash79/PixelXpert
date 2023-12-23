package sh.siava.pixelxpert;

import android.app.Application;
import android.content.Context;

import com.google.android.material.color.DynamicColors;

import java.lang.ref.WeakReference;

public class PixelXpert extends Application {

	private static PixelXpert instance;
	private static WeakReference<Context> contextReference;

	public void onCreate() {
		super.onCreate();
		instance = this;
		contextReference = new WeakReference<>(getApplicationContext());
		DynamicColors.applyToActivitiesIfAvailable(this);
	}

	public static Context getAppContext() {
		if (contextReference == null || contextReference.get() == null) {
			contextReference = new WeakReference<>(PixelXpert.getInstance().getApplicationContext());
		}
		return contextReference.get();
	}

	private static PixelXpert getInstance() {
		if (instance == null) {
			instance = new PixelXpert();
		}
		return instance;
	}
}
