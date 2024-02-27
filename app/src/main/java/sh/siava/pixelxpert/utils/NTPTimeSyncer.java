package sh.siava.pixelxpert.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.lyft.kronos.AndroidClockFactory;
import com.lyft.kronos.KronosClock;
import com.lyft.kronos.SyncListener;
import com.topjohnwu.superuser.Shell;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class NTPTimeSyncer {
	static final int TIME_SYNC_TIMEOUT = 10000;
	final Object lock = new Object();
	Context mContext;
	KronosClock timeChecker;
	boolean success = false;

	SyncListener resultListener = new SyncListener() {
		@Override
		public void onStartSync(@NonNull String s) {}

		@Override
		public void onSuccess(long l, long l1) {
			try
			{
				//noinspection DataFlowIssue
				long currentNTPTime = timeChecker.getCurrentNtpTimeMs();
				Shell.cmd("date -s @"
						+ new DecimalFormat("#0.000")
						.format(
								currentNTPTime / 1000d)
				).exec();
				success = true;
			}
			finally {
				synchronized (lock) {
					lock.notify();
				}
			}
		}

		@Override
		public void onError(@NonNull String s, @NonNull Throwable throwable) {
			synchronized (lock) {
				lock.notify();
			}
		}
	};

	public NTPTimeSyncer(Context context)
	{
		mContext = context;
		timeChecker = AndroidClockFactory.createKronosClock(mContext, resultListener, getNTPServers());
	}
	public boolean syncTimeNow()
	{

		synchronized (lock) {
			success = false;
			timeChecker.syncInBackground();

			try {
				lock.wait(TIME_SYNC_TIMEOUT);
			} catch (Throwable ignored) {}

			return success;
		}
	}

	private List<String> getNTPServers() {
		SharedPreferences prefs = getDefaultSharedPreferences(mContext.createDeviceProtectedStorageContext());

		String NTPServerString =  prefs.getString("NTPServers", "");

		String[] NTPServers = NTPServerString.split(";");

		ArrayList<String> servers = new ArrayList<>();

		for(String NTPServer : NTPServers)
		{
			NTPServer = NTPServer.trim();
			if(NTPServer.length() > 0)
				servers.add(NTPServer);
		}

		if(servers.isEmpty())
			servers.add("time.nist.gov");

		return servers;
	}

}
