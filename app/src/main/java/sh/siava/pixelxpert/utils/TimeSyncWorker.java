package sh.siava.pixelxpert.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

public class TimeSyncWorker extends ListenableWorker {
	/**
	 * @noinspection unused
	 */
	private static final String TAG = "Time Sync Worker";
	Context mContext;

	/**
	 * @param appContext   The application {@link Context}
	 * @param workerParams Parameters to setup the internal state of this worker
	 */
	public TimeSyncWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
		super(appContext, workerParams);
		mContext = appContext;

	}

	@NonNull
	@Override
	public ListenableFuture<Result> startWork() {
		return CallbackToFutureAdapter.getFuture(completer -> {
			completer.set(new NTPTimeSyncer(mContext).syncTimeNow() ? Result.success() : Result.retry());
			return completer;
		});
	}
}

