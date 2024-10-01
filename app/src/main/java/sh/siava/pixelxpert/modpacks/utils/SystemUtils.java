package sh.siava.pixelxpert.modpacks.utils;

import static android.content.Context.RECEIVER_EXPORTED;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;
import static java.lang.Math.round;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.TorchCallback;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;

import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.modpacks.XPLauncher;

/** @noinspection UnusedReturnValue*/
@SuppressWarnings("unused")
public class SystemUtils {
	private static final int THREAD_PRIORITY_BACKGROUND = 10;
	public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
	public static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";

	@SuppressLint("StaticFieldLeak")
	static SystemUtils instance;
	Context mContext;
	PackageManager mPackageManager;
	CameraManager mCameraManager;
	VibratorManager mVibrationManager;
	AudioManager mAudioManager;
	PowerManager mPowerManager;
	ConnectivityManager mConnectivityManager;
	TelephonyManager mTelephonyManager;
	AlarmManager mAlarmManager;
	DownloadManager mDownloadManager = null;
	NetworkStatsManager mNetworkStatsManager = null;
	boolean mHasVibrator = false;
	int maxFlashLevel = -1;
	static boolean isTorchOn = false;

	ArrayList<ChangeListener> mFlashlightLevelListeners = new ArrayList<>();
	ArrayList<ChangeListener> mVolumeChangeListeners = new ArrayList<>();
	private WifiManager mWifiManager;
	private WindowManager mWindowManager;
	private UserManager mUserManager;

	public static void restartSystemUI() {
		BootLoopProtector.resetCounter("com.android.systemui");

		XPLauncher.enqueueProxyCommand(proxy -> {
			try {
				proxy.runCommand("killall com.android.systemui");
			} catch (Throwable ignored) {}
		});
	}

	public static void restart() {
		XPLauncher.enqueueProxyCommand(proxy -> {
			try {
				proxy.runCommand("am start -a android.intent.action.REBOOT");
			} catch (Throwable ignored) {}
		});
	}

	public static boolean isFlashOn() {
		return isTorchOn;
	}

	public static void toggleFlash() {
		if (instance != null)
			instance.toggleFlashInternal();
	}

	public static void setFlash(boolean enabled, int level) {
		if (instance != null)
			instance.setFlashInternal(enabled, level);
	}

	public static void setFlash(boolean enabled) {
		if (instance != null)
			instance.setFlashInternal(enabled);
	}

	@Nullable
	@Contract(pure = true)
	public static WifiManager WifiManager()
	{
		return instance == null
				? null
				: instance.getWifiManager();
	}
	@Nullable
	@Contract(pure = true)
	public static NetworkStatsManager NetworkStatsManager() {
		return instance == null
				? null
				: instance.getNetworkStatsManager();
	}

	@Nullable
	@Contract(pure = true)
	public static CameraManager CameraManager() {
		return instance == null
				? null
				: instance.getCameraManager();
	}

	@Nullable
	@Contract(pure = true)
	public static PackageManager PackageManager() {
		return instance == null
				? null
				: instance.getPackageManager();
	}

	@Nullable
	@Contract(pure = true)
	public static AudioManager AudioManager() {
		return instance == null
				? null
				: instance.getAudioManager();
	}

	@Nullable
	@Contract(pure = true)
	public static WindowManager WindowManager() {
		return instance == null
				? null
				: instance.getWindowManager();
	}

	@Nullable
	@Contract(pure = true)
	public static UserManager UserManager() {
		return instance == null
				? null
				: instance.getUserManager();
	}

	@Nullable
	@Contract(pure = true)
	public static ConnectivityManager ConnectivityManager() {
		return instance == null
				? null
				: instance.getConnectivityManager();
	}

	@Nullable
	@Contract(pure = true)
	public static PowerManager PowerManager() {
		return instance == null
				? null
				: instance.getPowerManager();
	}

	@Nullable
	@Contract(pure = true)
	public static AlarmManager AlarmManager() {
		return instance == null
				? null
				: instance.getAlarmManager();
	}

	@Nullable
	@Contract(pure = true)
	public static TelephonyManager TelephonyManager() {
		return instance == null
				? null
				: instance.getTelephonyManager();
	}

	public static DownloadManager DownloadManager() {
		return instance == null
				? null
				: instance.getDownloadManager();
	}

	public static void vibrate(int effect, @Nullable Integer vibrationUsage) {
		vibrate(VibrationEffect.createPredefined(effect), vibrationUsage);
	}

	@SuppressLint("MissingPermission")
	public static void vibrate(VibrationEffect effect, @Nullable Integer vibrationUsage) {
		if (instance == null || !instance.hasVibrator()) return;
		try {
			if(vibrationUsage != null) {
				instance.getVibrationManager().getDefaultVibrator().vibrate(effect, VibrationAttributes.createForUsage(vibrationUsage));
			}
			else
			{
				instance.getVibrationManager().getDefaultVibrator().vibrate(effect);
			}
		} catch (Exception ignored) {
		}
	}

	private boolean hasVibrator() {
		return getVibrationManager() != null && mHasVibrator;
	}

	public static void sleep() {
		if (instance != null)
		{
			try {
				callMethod(PowerManager(), "goToSleep", SystemClock.uptimeMillis());
			} catch (Throwable ignored) {}
		}
	}

	public SystemUtils(Context context) {
		mContext = context;

		instance = this;

		registerVolumeChangeReceiver();
	}
	public static void threadSleep(int millis)
	{
		try {
			Thread.sleep(millis);
		} catch (Throwable ignored) {}
	}

	private void registerVolumeChangeReceiver() {
		BroadcastReceiver volChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC)
				{
					int newLevel = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, 0);
					for(ChangeListener listener : mVolumeChangeListeners)
					{
						listener.onChanged(newLevel);
					}
				}
			}
		};

		IntentFilter volumeFilter = new IntentFilter();
		volumeFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
		mContext.registerReceiver(volChangeReceiver, volumeFilter, RECEIVER_EXPORTED);
	}

	public static void registerFlashlightLevelListener(ChangeListener listener)
	{
		instance.mFlashlightLevelListeners.add(listener);
	}

	public static void registerVolumeChangeListener(ChangeListener listener)
	{
		if(instance != null)
			instance.mVolumeChangeListeners.add(listener);
	}

	public static void unregisterVolumeChangeListener(ChangeListener listener)
	{
		if(instance != null)
			instance.mVolumeChangeListeners.remove(listener);
	}

	public static void setFlashlightLevel(int level)
	{
		for(ChangeListener listener : instance.mFlashlightLevelListeners)
		{
			listener.onChanged(level);
		}
	}

	private void setFlashInternal(boolean enabled) {
		if(getCameraManager() == null)
			return;

		try {
			String flashID = getFlashID(mCameraManager);
			if (flashID.isEmpty()) {
				return;
			}
			if (enabled
					&& Xprefs.getBoolean("leveledFlashTile", false)
					&& Xprefs.getBoolean("isFlashLevelGlobal", false)
					&& supportsFlashLevelsInternal()) {
				float currentPct = Xprefs.getFloat("flashPCT", 0.5f);
				setFlashInternal(true, getFlashlightLevelInternal(currentPct));
				return;
			}

			mCameraManager.setTorchMode(flashID, enabled);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("PixelXpert Error in setting flashlight");
				log(t);
			}
		}
	}

	public static int getFlashlightLevel(float flashPct)
	{
		if(instance == null) return 1;
		return instance.getFlashlightLevelInternal(flashPct);
	}

	private int getFlashlightLevelInternal(float flashPct) {
		return
				Math.max(
						Math.min(
								round(SystemUtils.getMaxFlashLevel() * flashPct),
								SystemUtils.getMaxFlashLevel())
						, 1);
	}


	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean supportsFlashLevels() {
		return instance != null
				&& instance.supportsFlashLevelsInternal();
	}

	private boolean supportsFlashLevelsInternal() {
		if(maxFlashLevel == -1)
		{
			refreshFlashLevel();
		}
		return maxFlashLevel > 0;
	}

	private void refreshFlashLevel()
	{
		try {
			String flashID = getFlashID(getCameraManager());
			if (flashID.isEmpty()) {
				maxFlashLevel = -1;
				return;
			}
			if (maxFlashLevel == -1) {
				@SuppressWarnings("unchecked")
				CameraCharacteristics.Key<Integer> FLASH_INFO_STRENGTH_MAXIMUM_LEVEL = (CameraCharacteristics.Key<Integer>) getStaticObjectField(CameraCharacteristics.class, "FLASH_INFO_STRENGTH_MAXIMUM_LEVEL");
				maxFlashLevel = mCameraManager.getCameraCharacteristics(flashID).get(FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
			}
		} catch (Throwable ignored) {}
	}

	public static int getMaxFlashLevel()
	{
		return instance == null
				? -1
				: instance.getMaxFlashLevelInternal();
	}

	private int getMaxFlashLevelInternal() {
		if(maxFlashLevel == -1)
		{
			refreshFlashLevel();
		}
		return maxFlashLevel;
	}

	private void setFlashInternal(boolean enabled, int level) {
		if(getCameraManager() == null)
		{
			return;
		}

		try {
			String flashID = getFlashID(mCameraManager);
			if (enabled) {
				if (supportsFlashLevels()) //good news. we can set levels
				{
					callMethod(mCameraManager, "turnOnTorchWithStrengthLevel", flashID, Math.max(level, 1));
				} else //flash doesn't support levels: go normal
				{
					setFlashInternal(true);
				}
			} else {
				mCameraManager.setTorchMode(flashID, false);
			}
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("PixelXpert Error in setting flashlight");
				log(t);
			}
		}
	}

	private void toggleFlashInternal() {
		setFlashInternal(!isTorchOn);
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

	public static int getFlashStrength()
	{
		return instance.getFlashStrengthInternal();
	}

	public static float getFlashStrengthPCT()
	{
		return instance.getFlashStrengthInternal() / (float) SystemUtils.getMaxFlashLevel();
	}

	private int getFlashStrengthInternal()
	{
		try {
			return mCameraManager.getTorchStrengthLevel(getFlashID(mCameraManager));
		} catch (CameraAccessException e) {
			return 0;
		}
	}

	public static boolean isDarkMode() {
		return instance != null
				&& instance.getIsDark();
	}

	private boolean getIsDark() {
		return (mContext.getResources().getConfiguration().uiMode & UI_MODE_NIGHT_YES) == UI_MODE_NIGHT_YES;
	}

	static boolean darkSwitching = false;

	public static void doubleToggleDarkMode() {
		XPLauncher.enqueueProxyCommand(proxy -> {
			boolean isDark = isDarkMode();
			new Thread(() -> {
				try {
					while (darkSwitching) {
						Thread.currentThread().wait(100);
					}
					darkSwitching = true;

					proxy.runCommand("cmd uimode night " + (isDark ? "no" : "yes"));
					threadSleep(1000);
					proxy.runCommand("cmd uimode night " + (isDark ? "yes" : "no"));

					threadSleep(500);
					darkSwitching = false;
				} catch (Exception ignored) {
				}
			}).start();
		});
	}

	public static void killSelf()
	{
		BootLoopProtector.resetCounter(android.os.Process.myProcessName());

		android.os.Process.killProcess(android.os.Process.myPid());
	}

	private AudioManager getAudioManager() {
		if (mAudioManager == null) {
			try {
				mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log(t);
				}
			}
		}
		return mAudioManager;
	}

	private PackageManager getPackageManager() {
		if (mPackageManager == null) {
			try {
				mPackageManager = mContext.getPackageManager();
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log(t);
				}
			}
		}
		return mPackageManager;
	}

	private WifiManager getWifiManager() {
		if(mWifiManager == null)
		{
			try
			{
				mWifiManager = mContext.getSystemService(WifiManager.class);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting wifi manager");
					log(t);
				}
			}
		}
		return mWifiManager;
	}

	private ConnectivityManager getConnectivityManager() {
		if(mConnectivityManager == null)
		{
			try
			{
				mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting connection manager");
					log(t);
				}
			}
		}
		return mConnectivityManager;
	}


	private PowerManager getPowerManager() {
		if(mPowerManager == null)
		{
			try {
				mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting power manager");
					log(t);
				}
			}
		}
		return mPowerManager;
	}

	private AlarmManager getAlarmManager() {
		if(mAlarmManager == null)
		{
			try {
				mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting alarm manager");
					log(t);
				}
			}
		}
		return mAlarmManager;
	}

	private TelephonyManager getTelephonyManager() {
		if(mTelephonyManager == null)
		{
			try {
				mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting telephony manager");
					log(t);
				}
			}
		}
		return mTelephonyManager;
	}

	private VibratorManager getVibrationManager()
	{
		if(mVibrationManager == null)
		{
			try {
				mVibrationManager = (VibratorManager) mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
				mHasVibrator = mVibrationManager.getDefaultVibrator().hasVibrator();
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting vibrator");
					log(t);
				}
			}
		}
		return mVibrationManager;
	}

	private NetworkStatsManager getNetworkStatsManager()
	{
		if(mNetworkStatsManager == null)
		{
			try
			{
				mNetworkStatsManager = (NetworkStatsManager) mContext.getSystemService(Context.NETWORK_STATS_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("PixelXpert Error getting network stats");
					log(t);
				}
			}
		}
		return mNetworkStatsManager;
	}

	private CameraManager getCameraManager() {
		if(mCameraManager == null)
		{
			try {
				HandlerThread thread = new HandlerThread("", THREAD_PRIORITY_BACKGROUND);
				thread.start();
				Handler mHandler = new Handler(thread.getLooper());
				mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

				mCameraManager.registerTorchCallback(new TorchCallback() {
					@Override
					public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
						super.onTorchModeChanged(cameraId, enabled);
						isTorchOn = enabled;
					}
				}, mHandler);
			} catch (Throwable t) {
				mCameraManager = null;
				if (BuildConfig.DEBUG) {
					log(t);
				}
			}
		}
		return mCameraManager;
	}

	private DownloadManager getDownloadManager() {
		if (mDownloadManager == null) {
			mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
		}
		return mDownloadManager;
	}

	private WindowManager getWindowManager() {
		if (mWindowManager == null) {
			try {
				mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log(t);
				}
			}
		}
		return mWindowManager;
	}

	private UserManager getUserManager() {
		if (mUserManager == null) {
			try {
				mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log(t);
				}
			}
		}
		return mUserManager;
	}
	
	public interface ChangeListener
	{
		void onChanged(int newVal);
	}
}