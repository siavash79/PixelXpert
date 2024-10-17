package sh.siava.pixelxpert.utils;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static sh.siava.pixelxpert.R.string.download_channel_name;
import static sh.siava.pixelxpert.ui.Constants.ASSETS_DOWNLOADING_ID;
import static sh.siava.pixelxpert.ui.Constants.DOWNLOAD_CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;

import java.io.File;

import sh.siava.pixelxpert.R;

public class PyTorchSegmentor {

	private static final String TAG = "PyTorchSegmentor";
	private static final String PYTORCH_LIB = "libpytorch_jni_lite.so";
	private static final String LIB_BASE_URL = "https://github.com/siavash79/PixelXpert/raw/refs/heads/canary/app/lib/";
	private static final String MODEL_FILENAME = "u2net.ptl";
	private static final String MODEL_BASE_URL = "https://github.com/siavash79/PixelXpert/raw/refs/heads/canary/app/pytorchModel/";
	public static Bitmap extractSubject(Context context, Bitmap input)
	{
		try {
			if (!loadAssets(context)) return null;

			String modelPath = String.format("%s/%s", context.getCacheDir().getAbsolutePath(), MODEL_FILENAME);
			return new PyTorchBackgroundRemover(context, modelPath).removeBackground(input);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static void createNotificationChannel(Context context) {
		NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

		notificationManager.createNotificationChannel(new NotificationChannel(DOWNLOAD_CHANNEL_ID, context.getString(download_channel_name), IMPORTANCE_DEFAULT));
	}


	/**Loads assets from web. returns true if everything is pre-loaded and false if anything is still ongoing*/
	public static boolean loadAssets(Context context) {
		createNotificationChannel(context);

		boolean libLoaded = loadPyTorchLibrary(context);
		boolean modelLoaded =  downloadAIModel(context);

		return libLoaded && modelLoaded;
	}

	private static boolean downloadAIModel(Context context) {
		String AIPath = String.format("%s/%s", context.getCacheDir(), MODEL_FILENAME);
		if(new File(AIPath).exists()) return true;

		String downloadURL = String.format("%s/%s", MODEL_BASE_URL, MODEL_FILENAME);

		String tag = "ai_model";

		postNotification(context, tag);
		PRDownloader.download(downloadURL, context.getCacheDir().getPath(), MODEL_FILENAME).build().start(new OnDownloadListener(){
			@Override
			public void onDownloadComplete() {
				removeNotification(context, tag);
			}

			@Override
			public void onError(Error error) {
				removeNotification(context, tag);
			}
		});

		return false;
	}

	private static void removeNotification(Context context, String tag) {
		NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

		notificationManager.cancel(tag, ASSETS_DOWNLOADING_ID);
	}

	private static void postNotification(Context context, String tag) {
		NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
		Notification notification = new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_notification_foreground)
				.setContentTitle(context.getText(R.string.assets_download_title))
				.setContentText(context.getText(R.string.assets_download_body))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setOngoing(true)
				.setProgress(0, 0, true)
				.setAutoCancel(false)
				.build();

		notificationManager.notify(tag, ASSETS_DOWNLOADING_ID, notification);
	}

	@SuppressLint("UnsafeDynamicallyLoadedCode")
	private static boolean loadPyTorchLibrary(Context context) {
		String libPath = String.format("%s/%s", context.getCacheDir(), PYTORCH_LIB);
		if(new File(libPath).exists())
		{
			try {
				System.load(libPath);
				return true;
			} catch (Throwable ignored) {}
		}

		downloadLibrary(context);
		return false;
	}

	private static void downloadLibrary(Context context) {
		String architecture = Build.SUPPORTED_ABIS[0];
		String downloadURL = String.format("%s%s/%s", LIB_BASE_URL, architecture, PYTORCH_LIB);

		String tag = "ai_lib";

		postNotification(context, tag);

		PRDownloader.download(downloadURL, context.getCacheDir().getPath(), PYTORCH_LIB).build().start(new OnDownloadListener() {
			@Override
			public void onDownloadComplete() {
				removeNotification(context, tag);
			}

			@Override
			public void onError(Error error) {
				removeNotification(context, tag);
			}
		});
	}
}