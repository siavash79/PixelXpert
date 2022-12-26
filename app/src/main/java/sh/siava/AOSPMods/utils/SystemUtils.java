package sh.siava.AOSPMods.utils;

import static com.topjohnwu.superuser.Shell.cmd;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.XPrefs;

public class SystemUtils {
	private static final int THREAD_PRIORITY_BACKGROUND = 10;

	@SuppressLint("StaticFieldLeak")
	static SystemUtils instance;
	private final boolean mCameraListenerEnabled;
	private Handler mHandler = null;

	Context mContext;
	CameraManager mCameraManager;
	VibratorManager mVibrationManager;
	AudioManager mAudioManager;
	PowerManager mPowerManager;
	ConnectivityManager mConnectivityManager;
	TelephonyManager mTelephonyManager;
	AlarmManager mAlarmManager;
	NetworkStats mNetworkStats;
	DownloadManager mDownloadManager = null;
	boolean hasVibrator;
	int maxFlashLevel = -1;

	ArrayList<FlashlighLevelListener> flashlighLevelListeners = new ArrayList<>();

	TorchCallback torchCallback = new TorchCallback();

	public static void RestartSystemUI() {
		cmd("killall com.android.systemui").submit();
	}

	public static void Restart() {
		cmd("am start -a android.intent.action.REBOOT").submit();
	}

	public static boolean isFlashOn() {
		if (instance == null) return false;
		return TorchCallback.torchOn;
	}

	public static void ToggleFlash() {
		if (instance == null) return;
		instance.toggleFlashInternal();
	}

	public static NetworkStats NetworkStats() {
		if (instance == null) return null;
		instance.initiateNetworkStats();
		return instance.mNetworkStats;
	}

	private void initiateNetworkStats() {
		if (mNetworkStats == null) {
			mNetworkStats = new NetworkStats(mContext);
		}
	}

	public static void setFlash(boolean enabled, float pct) {
		if (instance == null) return;
		instance.setFlashInternal(enabled, pct);
	}

	public static void setFlash(boolean enabled) {
		if (instance == null) return;
		instance.setFlashInternal(enabled);
	}

	@Nullable
	@Contract(pure = true)
	public static AudioManager AudioManager() {
		if (instance == null) return null;
		return instance.getAudioManager();
	}

	private AudioManager getAudioManager() { //we don't init audio manager unless it's requested by someone
		if (mAudioManager == null) {
			//Audio
			try {
				mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					t.printStackTrace();
				}
			}
		}
		return mAudioManager;
	}

	@Nullable
	@Contract(pure = true)
	public static ConnectivityManager ConnectivityManager() {
		if (instance == null) return null;
		return instance.mConnectivityManager;
	}

	@Nullable
	@Contract(pure = true)
	public static PowerManager PowerManager() {
		if (instance == null) return null;
		return instance.mPowerManager;
	}

	@Nullable
	@Contract(pure = true)
	public static AlarmManager AlarmManager() {
		if (instance == null) return null;
		return instance.mAlarmManager;
	}


	@Nullable
	@Contract(pure = true)
	public static TelephonyManager TelephonyManager() {
		if (instance == null) return null;
		return instance.mTelephonyManager;
	}

	public static DownloadManager DownloadManager() {
		if (instance == null) return null;
		return instance.getDownloadManager();
	}

	public static void vibrate(int effect) {
		vibrate(VibrationEffect.createPredefined(effect));
	}

	@SuppressLint("MissingPermission")
	public static void vibrate(VibrationEffect effect) {
		if (instance == null || !instance.hasVibrator) return;
		try {
			instance.mVibrationManager.getDefaultVibrator().vibrate(effect);
		} catch (Exception ignored) {
		}
	}

	public static void Sleep() {
		if (instance == null) return;

		try {
			callMethod(instance.mPowerManager, "goToSleep", SystemClock.uptimeMillis());
		} catch (Throwable ignored) {
		}
	}

	public SystemUtils(Context context, boolean enableCameraListener) {
		mContext = context;
		mCameraListenerEnabled = enableCameraListener;

		instance = this;

		//Camera and Flash
		if(mCameraListenerEnabled) {
			try {
				HandlerThread thread = new HandlerThread("", THREAD_PRIORITY_BACKGROUND);
				thread.start();
				mHandler = new Handler(thread.getLooper());
				mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

				mCameraManager.registerTorchCallback(torchCallback, mHandler);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					t.printStackTrace();
				}
			}
		}
		//Connectivity
		try {
			mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error getting connection manager");
				t.printStackTrace();
			}
		}

		//Power
		try {
			mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error getting power manager");
				t.printStackTrace();
			}
		}

		//Telephony
		try {
			mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error getting telephony manager");
				t.printStackTrace();
			}
		}

		//Alarm
		try {
			mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error getting alarm manager");
				t.printStackTrace();
			}
		}

		//Vibrator
		try {
			mVibrationManager = (VibratorManager) mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
			hasVibrator = mVibrationManager.getDefaultVibrator().hasVibrator();
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error getting vibrator");
				t.printStackTrace();
			}
		}
	}

	public static void registerFlashlighLevelListener(FlashlighLevelListener listener)
	{
		instance.flashlighLevelListeners.add(listener);
	}

	public static void setFlashlightLevel(int level)
	{
		for(FlashlighLevelListener listener : instance.flashlighLevelListeners)
		{
			listener.onLevelChanged(level);
		}
	}

	private void setFlashInternal(boolean enabled) {
		if(!mCameraListenerEnabled) return;

		try {
			String flashID = getFlashID(mCameraManager);
			if (flashID.equals("")) {
				return;
			}
			if (enabled
					&& XPrefs.Xprefs.getBoolean("leveledFlashTile", false)
					&& XPrefs.Xprefs.getBoolean("isFlashLevelGlobal", false)
					&& supportsFlashLevelsInternal()) {
				float currentPct = XPrefs.Xprefs.getFloat("flashPCT", 0.5f);
				setFlashInternal(true, currentPct);
				return;
			}

			mCameraManager.setTorchMode(flashID, enabled);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error in setting flashlight");
				t.printStackTrace();
			}
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean supportsFlashLevels() {
		if (instance == null) return false;
		return instance.supportsFlashLevelsInternal();
	}

	private boolean supportsFlashLevelsInternal() {
		if(!mCameraListenerEnabled) return false;

		try {
			String flashID = getFlashID(mCameraManager);
			if (flashID.equals("")) {
				return false;
			}
			if (maxFlashLevel == -1) {
				@SuppressWarnings("unchecked")
				CameraCharacteristics.Key<Integer> FLASH_INFO_STRENGTH_MAXIMUM_LEVEL = (CameraCharacteristics.Key<Integer>) getStaticObjectField(CameraCharacteristics.class, "FLASH_INFO_STRENGTH_MAXIMUM_LEVEL");
				maxFlashLevel = mCameraManager.getCameraCharacteristics(flashID).get(FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
			}
			return maxFlashLevel > 1;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private void setFlashInternal(boolean enabled, float pct) {
		if(!mCameraListenerEnabled) return;

		try {
			String flashID = getFlashID(mCameraManager);
			if (flashID.equals("")) {
				return;
			}
			if (maxFlashLevel == -1) {
				@SuppressWarnings("unchecked")
				CameraCharacteristics.Key<Integer> FLASH_INFO_STRENGTH_MAXIMUM_LEVEL = (CameraCharacteristics.Key<Integer>) getStaticObjectField(CameraCharacteristics.class, "FLASH_INFO_STRENGTH_MAXIMUM_LEVEL");
				maxFlashLevel = mCameraManager.getCameraCharacteristics(flashID).get(FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
			}
			if (enabled) {
				if (maxFlashLevel > 1) //good news. we can set levels
				{
					callMethod(mCameraManager, "turnOnTorchWithStrengthLevel", flashID, Math.max(Math.round(pct * maxFlashLevel), 1));
				} else //flash doesn't support levels: go normal
				{
					setFlashInternal(true);
				}
			} else {
				mCameraManager.setTorchMode(flashID, false);
			}
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("AOSPMods Error in setting flashlight");
				t.printStackTrace();
			}
		}
	}

	private void toggleFlashInternal() {
		setFlashInternal(!TorchCallback.torchOn);
	}

	private String getFlashID(@NonNull CameraManager cameraManager) throws CameraAccessException {
		String[] ids = cameraManager.getCameraIdList();
		for (String id : ids) {
			if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
				if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
					return id;
				}
			}
		}
		return "";
	}

	private DownloadManager getDownloadManager() {
		if (mDownloadManager == null) {
			mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
		}
		return mDownloadManager;
	}

	static class TorchCallback extends CameraManager.TorchCallback {
		static boolean torchOn = false;

		@Override
		public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
			super.onTorchModeChanged(cameraId, enabled);
			torchOn = enabled;
		}
	}

	public static boolean isDarkMode() {
		if (instance == null) return false;
		return instance.getIsDark();
	}

	private boolean getIsDark() {
		return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
	}

	static boolean darkSwitching = false;

	public static void doubleToggleDarkMode() {
		boolean isDark = isDarkMode();
		new Thread(() -> {
			try {
				while (darkSwitching) {
					Thread.currentThread().wait(100);
				}
				darkSwitching = true;

				cmd("cmd uimode night " + (isDark ? "no" : "yes")).exec();
				Thread.sleep(1000);
				cmd("cmd uimode night " + (isDark ? "yes" : "no")).exec();

				Thread.sleep(500);
				darkSwitching = false;
			} catch (Exception ignored) {
			}
		}).start();
	}

	public interface FlashlighLevelListener
	{
		public void onLevelChanged(int level);
	}
}