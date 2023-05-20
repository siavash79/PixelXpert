package sh.siava.AOSPMods.modpacks.utils;

import static de.robv.android.xposed.XposedBridge.log;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import javax.security.auth.callback.Callback;

import sh.siava.AOSPMods.BuildConfig;

public class NetworkStats {

	private static final long MB = 1024 * 1024;
	private static final long MINUTE = 60 * 1000L;
	private final Context mContext;
	private long lastUpdateTime = 0;
	private static final long refreshInterval = 10; //seconds
	private static final long saveThreshold = 10 * MB;
	private final String statDataPath;
	private boolean enabled = false;
	private Calendar operationDate;

	private final ArrayList<networkStatCallback> callbacks = new ArrayList<>();
	private long totalRxBytes = 0;
	private long totalTxBytes = 0;
	private long todayCellRxBytes = 0, todayCellTxBytes = 0;
	private long cellRx = 0, cellTx = 0;
	private long totalCellRxBytes = 0, totalCellTxBytes = 0;
	private int saveInterval = 5; //minutes

	private long todayRxBytes = 0, todayTxBytes = 0;

	private long rxData, txData;
	private long lastSaveTime;

	private String wifiSsidName = "";

	@SuppressWarnings("unused")
	public void registerCallback(networkStatCallback callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	@SuppressWarnings("unused")
	public void unRegisterCallback(networkStatCallback callback) {
		try {
			callbacks.remove(callback);
		} catch (Exception ignored) {
		}
	}

	@SuppressWarnings("unused")
	public void resetCallbacks() {
		callbacks.clear();
	}

	private void resetStats() {
		operationDate = Calendar.getInstance();

		resetData();

		try {
			//noinspection ResultOfMethodCallIgnored
			new File(statDataPath).delete();
			//noinspection ResultOfMethodCallIgnored
			new File(statDataPath).mkdirs();
		} catch (Exception ignored) {
		}
	}

	public void setSaveInterval(int intrval) {
		saveInterval = intrval;
	}

	private void resetData() {
		todayCellRxBytes
				= todayCellTxBytes
				= todayRxBytes
				= todayTxBytes
				= rxData
				= txData
				= 0;
		wifiSsidName = "";
	}


	private final Handler mTrafficHandler = new Handler(Looper.myLooper()) {
		@Override
		public void handleMessage(Message msg) {
			long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

			if (timeDelta < refreshInterval * 1000L) {
				return;
			}
			lastUpdateTime = SystemClock.elapsedRealtime();

			// Calculate the data rate from the change in total bytes and time
			long newTotalRxBytes = TrafficStats.getTotalRxBytes();
			long newTotalTxBytes = TrafficStats.getTotalTxBytes();
			long newCellTotalRxBytes = TrafficStats.getMobileRxBytes();
			long newCellTotalTxBytes = TrafficStats.getMobileTxBytes();

			rxData += newTotalRxBytes - totalRxBytes;
			txData += newTotalTxBytes - totalTxBytes;

			cellRx = newCellTotalRxBytes - totalCellRxBytes;
			cellTx = newCellTotalTxBytes - totalCellTxBytes;

			String curSsid = fetchCurrentWifiSSID();
			boolean ssidChanged = !Objects.equals(wifiSsidName, curSsid);
			if (ssidChanged) {
				wifiSsidName = curSsid;
			}
			boolean savedTraffic = false;
			if (rxData + txData > saveThreshold || (SystemClock.elapsedRealtime() - lastSaveTime) > (saveInterval * MINUTE)) {
				saveTrafficData();
				savedTraffic = true;
			}

			// Make sure we notify on SSID change where we didn't write out traffic data
			if (!savedTraffic && ssidChanged) {
				informCallbacks();
			}

			// Post delayed message to refresh in ~1000ms
			totalRxBytes = newTotalRxBytes;
			totalTxBytes = newTotalTxBytes;
			totalCellRxBytes = newCellTotalRxBytes;
			totalCellTxBytes = newCellTotalTxBytes;
			clearHandlerCallbacks();
			mTrafficHandler.postDelayed(mRunnable, refreshInterval * 1000L);
		}
	};

	BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) return;
			//noinspection deprecation
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				mTrafficHandler.sendEmptyMessage(0);
			}
		}
	};

	public void setStatus(boolean enabled) {
		if (enabled) {
			setEnabled();
		} else {
			setDisabled();
		}
		informCallbacks();
	}


	private void setEnabled() {
		if (enabled) return;

		try {
			//noinspection ResultOfMethodCallIgnored
			new File(statDataPath).mkdirs();
		} catch (Exception ignored) {
			return;
		}

		resetData();
		lastSaveTime = SystemClock.elapsedRealtime();

		totalRxBytes = TrafficStats.getTotalRxBytes(); //if we're at startup so it's almost zero
		totalTxBytes = TrafficStats.getTotalTxBytes(); //if we're midway, then previous stats since boot worth nothing
		totalCellRxBytes = TrafficStats.getMobileRxBytes(); //because we don't know those data are since when
		totalCellTxBytes = TrafficStats.getMobileTxBytes();

		tryLoadData();

		IntentFilter filter = new IntentFilter();
		//noinspection deprecation
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		mContext.registerReceiver(mIntentReceiver, filter, null, null);
		operationDate = Calendar.getInstance();
		scheduleDateChange();
		enabled = true;
	}

	private void setDisabled() {
		if (!enabled) return;
		enabled = false;
		try {
			mContext.unregisterReceiver(mIntentReceiver);
			clearHandlerCallbacks();
		} catch (Exception ignored) {
		}
	}

	private void scheduleDateChange() {
		try {
			Calendar nextDay = Calendar.getInstance();
			nextDay.set(Calendar.HOUR, 0);
			nextDay.set(Calendar.MINUTE, 0);
			nextDay.add(Calendar.DATE, 1);

			//noinspection ConstantConditions
			SystemUtils.AlarmManager().set(AlarmManager.RTC,
					nextDay.getTimeInMillis(),
					"",
					() -> {
						resetStats();
						scheduleDateChange();
					},
					null);
		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("Error setting network reset schedule");
				t.printStackTrace();
			}
		}
	}

	private void informCallbacks() {
		callbacks.forEach(callback -> callback.onStatChanged(this));
	}

	private void saveTrafficData() {
		lastSaveTime = SystemClock.elapsedRealtime();
		todayRxBytes += rxData;
		todayTxBytes += txData;
		todayCellRxBytes += cellRx;
		todayCellTxBytes += cellTx;
		rxData = txData = 0;

		try {
			if (Calendar.getInstance().get(Calendar.DATE) != operationDate.get(Calendar.DATE)) //in a rare case that we didn't understand the date has changed, this will ensure
			{
				resetStats();
				return;
			}

			informCallbacks();

			File dataFile = getDataFile();

			FileWriter writer = new FileWriter(dataFile);

			JsonWriter jasonWriter = new JsonWriter(writer);

			jasonWriter.beginObject()

					.name("RXTotal")
					.value(todayRxBytes)

					.name("TXTotal")
					.value(todayTxBytes)

					.name("CellRX")
					.value(todayCellRxBytes)

					.name("CellTX")
					.value(todayCellTxBytes)

					.endObject();

			jasonWriter.close();
			writer.close();
		} catch (Exception ignored) {
		}
	}

	private File getDataFile() {
		@SuppressLint("SimpleDateFormat")
		SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd");
		String filename = String.format("TrafficStats-%s.txt", nameFormat.format(Calendar.getInstance().getTime()));
		return new File(String.format("%s/%s", statDataPath, filename));
	}

	private final Runnable mRunnable = () -> mTrafficHandler.sendEmptyMessage(0);

	public NetworkStats(Context context) {
		mContext = context;

		statDataPath = mContext.getDataDir().getPath() + "/netStats";
	}

	public long getTodayDownloadBytes(boolean cellDataOnly) {
		return (cellDataOnly) ? todayCellRxBytes : todayRxBytes;
	}

	public long getTodayUploadBytes(boolean cellDataOnly) {
		return (cellDataOnly) ? todayCellTxBytes : todayTxBytes;
	}

	public String getSSIDName() {
		return wifiSsidName;
	}

	private void tryLoadData() {
		try {
			JsonReader jsonReader = new JsonReader(
					new FileReader(getDataFile()));

			jsonReader.beginObject();
			while (jsonReader.hasNext()) {
				String name = jsonReader.nextName();
				long value = jsonReader.nextLong();
				switch (name) {
					case "RXTotal":
						todayRxBytes = value;
						break;
					case "TXTotal":
						todayTxBytes = value;
						break;
					case "CellRX":
						todayCellRxBytes = value;
						break;
					case "CellTX":
						todayCellTxBytes = value;
						break;
				}

			}
		} catch (Exception ignored) {
		}
	}

	private String fetchCurrentWifiSSID() {
		WifiInfo info = mContext.getSystemService(WifiManager.class).getConnectionInfo();
		String ssid = "";
		String theSsid = info.getSSID();
		if (theSsid.startsWith("\"") && theSsid.endsWith("\"")) {
			theSsid = theSsid.substring(1, theSsid.length() - 1);
		}
		if (!WifiManager.UNKNOWN_SSID.equals(theSsid)) {
			ssid = theSsid;
		}
		return ssid;
	}

	private void clearHandlerCallbacks() {
		mTrafficHandler.removeCallbacks(mRunnable);
		mTrafficHandler.removeMessages(0);
		mTrafficHandler.removeMessages(1);
	}

	public interface networkStatCallback extends Callback {
		void onStatChanged(NetworkStats stats);
	}
}