package sh.siava.AOSPMods.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XposedHelpers;

public class SystemUtils {
	static SystemUtils instance;
	
	Context mContext;
	CameraManager mCameraManager;
	VibratorManager mVibrationManager;
	SensorManager mSensorManager;
	boolean torchOn = false;
	boolean hasVibrator;
	AudioManager mAudioManager;
	PowerManager mPowerManager;
	Sensor mProximitySensor;
	TelephonyManager mTelephonyManager;
	ProximityListener proximityListener = new ProximityListener();

	CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
		@Override
		public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
			super.onTorchModeChanged(cameraId, enabled);
			torchOn = enabled;
		}
	};

	public static boolean isFlashOn()
	{
		if(instance == null) return false;
		return instance.torchOn;
	}
	
	public static boolean isScreenCovered()
	{
		return ProximityListener.near;
	}
	
	public static void ToggleFlash()
	{
		if(instance == null) return;
		instance.toggleFlashInternal();
	}
	
	public static void setFlash(boolean enabled)
	{
		if(instance == null) return;
		instance.setFlashInternal(enabled);
	}
	
	public static AudioManager AudioManager()
	{
		if(instance == null) return null;
		return instance.mAudioManager;
	}
	
	public static PowerManager PowerManager()
	{
		if(instance == null) return null;
		return instance.mPowerManager;
	}
	
	
	public static TelephonyManager TelephonyManager()
	{
		if(instance == null) return null;
		return instance.mTelephonyManager;
	}
	
	@SuppressLint("MissingPermission")
	public static void vibrate(VibrationEffect effect)
	{
		if(instance == null || !instance.hasVibrator) return;
		instance.mVibrationManager.getDefaultVibrator().vibrate(effect);
	}
	
	public static void Sleep()
	{
		if(instance == null) return;
		
		XposedHelpers.callMethod(instance.mPowerManager, "goToSleep", SystemClock.uptimeMillis());
	}

	public static void startProximity()
	{
		if(instance != null)
		{
			instance.startProximityInternal();
		}
	}

	private void startProximityInternal()
	{
		try {
			//Proximity
			mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
			mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			mSensorManager.registerListener(proximityListener, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		}catch (Exception ignored){}
	}

	public static void stopProximity()
	{
		if(instance != null)
		{
			instance.stopProximityInternal();
		}
	}

	private void stopProximityInternal()
	{
		try {
			mSensorManager.unregisterListener(proximityListener);
			mProximitySensor = null;
			mSensorManager = null;
		} catch(Exception ignored){}
		ProximityListener.near = false;
	}

	public SystemUtils(Context context)
	{
		mContext = context;
		instance = this;

		//Camera and Flash
		mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
		mCameraManager.registerTorchCallback(torchCallback, new Handler());

		//Audio
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

		//Telephony
		mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

		//Vibrator
		mVibrationManager = (VibratorManager) mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
		hasVibrator = mVibrationManager.getDefaultVibrator().hasVibrator();
	}
	
	private void setFlashInternal(boolean enabled)
	{
		try {
			if(mCameraManager == null)
			{
				return;
			}
			
			String flashID = getFlashID(mCameraManager);
			if(flashID.equals(""))
			{
				return;
			}
			mCameraManager.setTorchMode(flashID, enabled);
		}catch (Exception ignored){}
	}
	
	private void toggleFlashInternal() {
		setFlashInternal(!torchOn);
	}
	
	private String getFlashID(CameraManager c) throws CameraAccessException {
		String[] ids = c.getCameraIdList();
		try {
			for (String id : ids) {
				if (c.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
					if (c.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
						return id;
					}
				}
			}
		}catch (Throwable e) {e.printStackTrace();}
		return "";
	}
	
	static class ProximityListener implements SensorEventListener {
		private static boolean near = false;
		@Override
		public void onSensorChanged(SensorEvent sensorEvent) {
			near = sensorEvent.values[0] == 0;
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int i) {
		}
	}
}
