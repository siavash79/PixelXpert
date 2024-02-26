package sh.siava.pixelxpert.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import sh.siava.pixelxpert.BuildConfig;

public class TimeSyncScheduler {
	private static final String TIME_SYNC_WORK_NAME = BuildConfig.APPLICATION_ID + ".TimeSyncSchedule";

	public static void scheduleTimeSync(Context context)
	{
		if(BuildConfig.DEBUG)
			Log.d("Time Scheduler", "Updating time sync schedule...");

		if(!WorkManager.isInitialized())
		{
			WorkManager.initialize(context, new Configuration.Builder().build());
		}

		WorkManager workManager = WorkManager.getInstance(context);

		SharedPreferences prefs = getDefaultSharedPreferences(context.createDeviceProtectedStorageContext());

		boolean SyncNTPTime = prefs.getBoolean("SyncNTPTime", false);
		int TimeSyncInterval = prefs.getInt("TimeSyncInterval", 24);

		if(SyncNTPTime)
		{
			PeriodicWorkRequest.Builder builder = new PeriodicWorkRequest.Builder(TimeSyncWorker.class, TimeSyncInterval, TimeUnit.HOURS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS);

			workManager.enqueueUniquePeriodicWork(
					TIME_SYNC_WORK_NAME,
					ExistingPeriodicWorkPolicy.UPDATE,
					builder.build());
		}
		else
		{
			workManager.cancelUniqueWork(TIME_SYNC_WORK_NAME);
		}
	}
}