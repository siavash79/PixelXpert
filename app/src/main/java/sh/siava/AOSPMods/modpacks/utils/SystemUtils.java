package sh.siava.AOSPMods.modpacks.utils;

import static android.content.res.Configuration.UI_MODE_NIGHT_YES;
import static com.topjohnwu.superuser.Shell.cmd;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.TorchCallback;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.modpacks.XPrefs;

@SuppressWarnings("unused")
public class SystemUtils {
	private static final int THREAD_PRIORITY_BACKGROUND = 10;

	@SuppressLint("StaticFieldLeak")
	static SystemUtils instance;
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
	boolean mHasVibrator = false;
	int maxFlashLevel = -1;
	static boolean isTorchOn = false;

	ArrayList<ChangeListener> mFlashlightLevelListeners = new ArrayList<>();
	ArrayList<ChangeListener> mVolumeChangeListeners = new ArrayList<>();

	public static void restartSystemUI() {
		cmd("killall com.android.systemui").submit();
	}

	public static void restart() {
		cmd("am start -a android.intent.action.REBOOT").submit();
	}

	public static boolean isFlashOn() {
		return isTorchOn;
	}

	public static void toggleFlash() {
		if (instance != null)
			instance.toggleFlashInternal();
	}

	public static void setFlash(boolean enabled, float pct) {
		if (instance != null)
			instance.setFlashInternal(enabled, pct);
	}

	public static void setFlash(boolean enabled) {
		if (instance != null)
			instance.setFlashInternal(enabled);
	}

	public static NetworkStats NetworkStats() {
		return instance == null
				? null
				: instance.getNetworkStats();
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

	private void registerVolumeChangeReceiver() {
		BroadcastReceiver volChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				for(ChangeListener listener : mVolumeChangeListeners)
				{
					listener.onChanged(0);
				}
			}
		};

		IntentFilter volumeFilter = new IntentFilter();
		volumeFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
		mContext.registerReceiver(volChangeReceiver, volumeFilter);
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
		return instance != null
				&& instance.supportsFlashLevelsInternal();
	}

	private boolean supportsFlashLevelsInternal() {
		if(getCameraManager() == null)
		{
			return false;
		}

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
		if(getCameraManager() == null)
		{
			return;
		}

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

	public static boolean isDarkMode() {
		return instance != null
				&& instance.getIsDark();
	}

	private boolean getIsDark() {
		return (mContext.getResources().getConfiguration().uiMode & UI_MODE_NIGHT_YES) == UI_MODE_NIGHT_YES;
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

	public static void killSelf()
	{
		BootLoopProtector.resetCounter(android.os.Process.myProcessName());

		android.os.Process.killProcess(android.os.Process.myPid());
	}
	public static void killForegroundApp()
	{
		Shell.cmd("am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)").submit();
	}

	private NetworkStats getNetworkStats() {
		if (mNetworkStats == null) {
			mNetworkStats = new NetworkStats(mContext);
		}
		return mNetworkStats;
	}

	private AudioManager getAudioManager() {
		if (mAudioManager == null) {
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

	private ConnectivityManager getConnectivityManager() {
		if(mConnectivityManager == null)
		{
			try
			{
				mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			} catch (Throwable t) {
				if (BuildConfig.DEBUG) {
					log("AOSPMods Error getting connection manager");
					t.printStackTrace();
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
					log("AOSPMods Error getting power manager");
					t.printStackTrace();
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
					log("AOSPMods Error getting alarm manager");
					t.printStackTrace();
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
					log("AOSPMods Error getting telephony manager");
					t.printStackTrace();
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
					log("AOSPMods Error getting vibrator");
					t.printStackTrace();
				}
			}
		}
		return mVibrationManager;
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
					t.printStackTrace();
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

	public interface ChangeListener
	{
		void onChanged(int newVal);
	}
}