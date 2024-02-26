package sh.siava.pixelxpert.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import sh.siava.pixelxpert.BuildConfig;

public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
		{
			if(BuildConfig.DEBUG)
				Log.d("BootReceiver", "Broadcast received: " + intent.getAction());

			UpdateScheduler.scheduleUpdates(context);
			TimeSyncScheduler.scheduleTimeSync(context);
		}
	}
}