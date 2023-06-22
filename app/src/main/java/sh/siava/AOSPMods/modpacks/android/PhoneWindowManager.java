package sh.siava.AOSPMods.modpacks.android;

import static android.content.Context.RECEIVER_EXPORTED;
import static android.view.KeyEvent.KEYCODE_POWER;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Insets;
import android.graphics.ParcelableColorSpace;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.view.Display;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class PhoneWindowManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static final int TAKE_SCREENSHOT_PROVIDED_IMAGE = 3;

	private static final String KEY_BUFFER = "bitmap_util_buffer";
	private static final String KEY_COLOR_SPACE = "bitmap_util_color_space";

	private Object windowMan = null;
	private static boolean broadcastRegistered = false;
	private Object mDisplayManagerInternal;
	private Display mDefaultDisplay;
	private Object mDefaultDisplayPolicy;
	private Object mHandler;
	private boolean ScreenshotChordInsecure;
	private final ArrayList<Object> screenshotChords = new ArrayList<>();
	private Class<?> ScreenshotRequestClass;

	public PhoneWindowManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		ScreenshotChordInsecure = XPrefs.Xprefs.getBoolean("ScreenshotChordInsecure", false);
	}

	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				String action = intent.getAction();
				switch (action) {
					case Constants.ACTION_INSECURE_SCREENSHOT:
						takeInsecureScreenshot();
						break;
					case Constants.ACTION_SCREENSHOT:
						try
						{
							callMethod(windowMan, "handleScreenShot", 1); //13 QPR3
						}
						catch(Throwable ignored)
						{
							callMethod(windowMan, "handleScreenShot", 1, 1); //pre 13 QPR3
						}
						break;
					case Constants.ACTION_BACK:
						callMethod(windowMan, "backKeyPress");
						break;
					case Constants.ACTION_SLEEP:
						SystemUtils.Sleep();
						break;
				}
			} catch (Throwable ignored) {}
		}
	};

	IntentFilter intentFilter = new IntentFilter();

	@SuppressLint("WrongConstant")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Collections.addAll(screenshotChords, KEYCODE_POWER, KEYCODE_VOLUME_DOWN);

		if (!broadcastRegistered) {
			broadcastRegistered = true;
			intentFilter.addAction(Constants.ACTION_SCREENSHOT);
			intentFilter.addAction(Constants.ACTION_BACK);
			intentFilter.addAction(Constants.ACTION_INSECURE_SCREENSHOT);
			intentFilter.addAction(Constants.ACTION_SLEEP);
			mContext.registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED); //for Android 14, receiver flag is mandatory
		}

		try {
			Class<?> PhoneWindowManagerClass = findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
			ScreenshotRequestClass = findClassIfExists("com.android.internal.util.ScreenshotRequest", lpparam.classLoader); //13 QPR3
			hookAllMethods(PhoneWindowManagerClass, "enableScreen", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					windowMan = param.thisObject;

					//Apparently some stuff init before Xposed. Will have to find and hack them...
					Object mKeyCombinationManager = getObjectField(param.thisObject, "mKeyCombinationManager");
					ArrayList<?> mRules = (ArrayList<?>) getObjectField(mKeyCombinationManager, "mRules");
					for (Object mRule : mRules) {
						if (screenshotChords.contains(getObjectField(mRule, "mKeyCode1"))
								&& screenshotChords.contains(getObjectField(mRule, "mKeyCode2"))) {
							hookAllMethods(mRule.getClass(), "execute", new XC_MethodHook() {
								@Override
								protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
									if (ScreenshotChordInsecure) {
										try {
											takeInsecureScreenshot();
											param.setResult(null);
										} catch (Throwable ignored) {
										}
									}
								}
							});
							break;
						}
					}
				}
			});

		} catch (Throwable ignored) {}
	}

	private void takeInsecureScreenshot() {
		if (mDisplayManagerInternal == null) {
			initVars();
		}

		Object SCBuffer = callMethod(mDisplayManagerInternal, "systemScreenshot", mDefaultDisplay.getDisplayId());

		if(ScreenshotRequestClass != null)
		{
			Bitmap screenshotBitmap = (Bitmap) callMethod(SCBuffer, "asBitmap");

			Object screenshotRequest = null;
			try {
				for(Constructor<?> constructor: ScreenshotRequestClass.getDeclaredConstructors())
				{
					if(constructor.getParameterCount() == 8)
					{
						screenshotRequest = constructor.newInstance(
								TAKE_SCREENSHOT_PROVIDED_IMAGE /* type */,
								0 /* source. 0 is global actions */,
								new ComponentName("", ""), 1 /* task id*/ ,
								0 /* user id */,
								screenshotBitmap,
								new Rect(0, 0, screenshotBitmap.getWidth(), screenshotBitmap.getHeight()),
								Insets.of(0, 0, 0, 0));
					}
				}
			}
			catch (Throwable ignored){}

			if(screenshotRequest != null)
			{
				callMethod(getObjectField(mDefaultDisplayPolicy, "mScreenshotHelper"),
						"takeScreenshot",
						screenshotRequest,
						mHandler,
						null);
			}
		}
		else
		{
			HardwareBuffer mHardwareBuffer = (HardwareBuffer) getObjectField(SCBuffer, "mHardwareBuffer");

			ParcelableColorSpace colorSpace = new ParcelableColorSpace((ColorSpace) getObjectField(SCBuffer, "mColorSpace"));

			Bundle bundle = new Bundle();
			bundle.putParcelable(KEY_BUFFER, mHardwareBuffer);
			bundle.putParcelable(KEY_COLOR_SPACE, colorSpace);

			callMethod(getObjectField(mDefaultDisplayPolicy, "mScreenshotHelper"),
					"provideScreenshot",
					bundle,
					new Rect(0, 0, mHardwareBuffer.getWidth(), mHardwareBuffer.getHeight()),
					Insets.of(0, 0, 0, 0),
					1,
					1,
					new ComponentName("", ""),
					0,
					mHandler,
					null);
		}
	}

	private void initVars() {
		mDisplayManagerInternal = getObjectField(windowMan, "mDisplayManagerInternal");
		mDefaultDisplay = (Display) getObjectField(windowMan, "mDefaultDisplay");
		mDefaultDisplayPolicy = getObjectField(windowMan, "mDefaultDisplayPolicy");
		mHandler = getObjectField(windowMan, "mHandler");
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}