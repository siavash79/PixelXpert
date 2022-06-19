package sh.siava.AOSPMods.Utils;

import static com.topjohnwu.superuser.Shell.cmd;
import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

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
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import org.jetbrains.annotations.Contract;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import sh.siava.AOSPMods.BuildConfig;

public class SystemUtils{
	@SuppressLint("StaticFieldLeak")
	static SystemUtils instance;

	Context mContext;
	CameraManager mCameraManager;
	VibratorManager mVibrationManager;
	AudioManager mAudioManager;
	PowerManager mPowerManager;
	ConnectivityManager mConnectivityManager;
	TelephonyManager mTelephonyManager;
	AlarmManager mAlaramManager;
	NetworkStats mNetworkStats;
	DownloadManager mDownloadManager = null;
	boolean hasVibrator;

	TorchCallback torchCallback = new TorchCallback();

	public static void RestartSystemUI()
	{
		cmd("killall com.android.systemui").submit();
	}

	public static void Restart()
	{
		cmd("am start -a android.intent.action.REBOOT").submit();
	}

	public static boolean isFlashOn() {
		if(instance == null) return false;
		return TorchCallback.torchOn;
	}
	
	public static void ToggleFlash() {
		if(instance == null) return;
		instance.toggleFlashInternal();
	}

	public static NetworkStats NetworkStats()
	{
		if(instance == null) return null;
		instance.initiateNetworkStats();
		return instance.mNetworkStats;
	}

	private void initiateNetworkStats() {
		if(mNetworkStats == null) {
			mNetworkStats = new NetworkStats(mContext);
		}
	}

	public static void setFlash(boolean enabled) {
		if(instance == null) return;
		instance.setFlashInternal(enabled);
	}
	
	@Nullable
	@Contract(pure = true)
	public static AudioManager AudioManager() {
		if(instance == null) return null;
		return instance.mAudioManager;
	}

	@Nullable
	@Contract(pure = true)
	public static ConnectivityManager ConnectivityManager()
	{
		if(instance == null) return null;
		return instance.mConnectivityManager;
	}

	@Nullable
	@Contract(pure = true)
	public static PowerManager PowerManager() {
		if(instance == null) return null;
		return instance.mPowerManager;
	}

	@Nullable
	@Contract(pure = true)
	public static AlarmManager AlarmManager() {
		if(instance == null) return null;
		return instance.mAlaramManager;
	}


	@Nullable
	@Contract(pure = true)
	public static TelephonyManager TelephonyManager() {
		if(instance == null) return null;
		return instance.mTelephonyManager;
	}

	public static DownloadManager DownloadManager() {
		if(instance == null) return null;
		return instance.getmDownloadManager();
	}
	
	public static void vibrate(int effect) {
		vibrate(VibrationEffect.createPredefined(effect));
	}

	@SuppressLint("MissingPermission")
	public static void vibrate(VibrationEffect effect) {
		if(instance == null || !instance.hasVibrator) return;
		try {
			instance.mVibrationManager.getDefaultVibrator().vibrate(effect);
		}catch (Exception ignored){}
	}

	public static void Sleep() {
		if(instance == null) return;

		try {
			callMethod(instance.mPowerManager, "goToSleep", SystemClock.uptimeMillis());
		} catch (Throwable ignored){}
	}

	public SystemUtils(Context context) {
		mContext = context;
		
		instance = this;

		//Camera and Flash
		try {
			mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
			mCameraManager.registerTorchCallback(torchCallback, null);
		}
		catch(Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods: Failed to Register flash callback");
				t.printStackTrace();
			}
		}

		//Audio
		try {
			mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods Error getting audio manager");
				t.printStackTrace();
			}
		}

		//Connectivity
		try
		{
			mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods Error getting connection manager");
				t.printStackTrace();
			}
		}

		//Power
		try {
			mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods Error getting power manager");
				t.printStackTrace();
			}
		}

		//Telephony
		try {
			mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods Error getting telephoney manager");
				t.printStackTrace();
			}
		}

		//Alarm
		try {
			mAlaramManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods Error getting alarm manager");
				t.printStackTrace();
			}
		}

		//Vibrator
		try {
			mVibrationManager = (VibratorManager) mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
			hasVibrator = mVibrationManager.getDefaultVibrator().hasVibrator();
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
				log("AOSPMods Error getting vibrator");
				t.printStackTrace();
			}
		}
	}
	
	private void setFlashInternal(boolean enabled) {
		try {
			String flashID = getFlashID(mCameraManager);
			if(flashID.equals(""))
			{
				return;
			}
			mCameraManager.setTorchMode(flashID, enabled);
		}
		catch (Throwable t)
		{
			if(BuildConfig.DEBUG)
			{
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

	private DownloadManager getmDownloadManager()
	{
		if(mDownloadManager == null) {
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
	public static boolean isDarkMode()
	{
		if(instance == null) return false;
		return instance.getIsDark();
	}

	private boolean getIsDark() {
		return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
	}

	public static void doubleToggleDarkMode() {
		boolean isDark = isDarkMode();
		new Thread(() -> {
			cmd("cmd uimode night " + (isDark ? "no" : "yes")).exec();
			cmd("cmd uimode night " + (isDark ? "yes" : "no")).exec();
		}).start();
	}
}