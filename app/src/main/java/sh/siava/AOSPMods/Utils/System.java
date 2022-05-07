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
import android.os.Vibrator;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XposedHelpers;

public class System {
	static System instance;
	
	Context mContext;
	CameraManager mCameraManager;
	Vibrator mVibrator;
	SensorManager mSensorManager;
	boolean torchOn = false;
	boolean hasVibrator = false;
	AudioManager mAudioManager;
	PowerManager mPowerManager;
	Sensor mProximitySensor;
	TelephonyManager mTelephonyManager;
	
	CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
		@Override
		public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
			super.onTorchModeChanged(cameraId, enabled);
			torchOn = enabled;
		}
	};
	
	ProximityListener proximityListener = new ProximityListener();
	
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
	public static void vibrate(long duration)
	{
		if(instance == null || !instance.hasVibrator) return;
		instance.mVibrator.vibrate(duration);
	}
	
	public static void Sleep()
	{
		if(instance == null) return;
		
		XposedHelpers.callMethod(instance.mPowerManager, "goToSleep", SystemClock.uptimeMillis());
	}
	
	public System(Context context)
	{
		mContext = context;
		instance = this;
		
		mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		
		
		mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		
		mSensorManager.registerListener(proximityListener, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		mCameraManager.registerTorchCallback(torchCallback, new Handler());
		
		hasVibrator = mVibrator.hasVibrator();
	}
	
	
	private void toggleFlashInternal() {
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
			
			mCameraManager.setTorchMode(flashID, !torchOn);
		}
		catch(Throwable T)
		{
			T.printStackTrace();
		}
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
		public static boolean near = false;
		@Override
		public void onSensorChanged(SensorEvent sensorEvent) {
			near = sensorEvent.values[0] == 0;
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int i) {
		}
	}
	
}
