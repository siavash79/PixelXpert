package sh.siava.pixelxpert.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;

import java.io.File;

public class PyTorchSegmentor {

	private static final String TAG = "PyTorchSegmentor";
	private static final String PYTORCH_LIB = "libpytorch_jni_lite.so";
	private static final String LIB_BASE_URL = "https://github.com/siavash79/PixelXpert/raw/refs/heads/canary/app/lib/";
	public static final String MODEL_FILENAME = "u2net.ptl";
	private static final String MODEL_BASE_URL = "https://github.com/siavash79/PixelXpert/raw/refs/heads/canary/app/pytorchModel/";
	public static Bitmap extractSubject(Context context, Bitmap input)
	{
		try {
			if (!loadAssets(context)) return null;

			return new PyTorchBackgroundRemover(context).removeBackground(input);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**Loads assets from web. returns true if everything is pre-loaded and false if anything is still ongoing*/
	public static boolean loadAssets(Context context) {
		boolean libLoaded = loadPyTorchLibrary(context);
		boolean modelLoaded =  downloadAIModel(context);

		return libLoaded && modelLoaded;
	}

	private static boolean downloadAIModel(Context context) {
		String AIPath = String.format("%s/%s", context.getCacheDir(), MODEL_FILENAME);
		if(new File(AIPath).exists()) return true;

		String downloadURL = String.format("%s/%s", MODEL_BASE_URL, MODEL_FILENAME);

		PRDownloader.download(downloadURL, context.getCacheDir().getPath(), MODEL_FILENAME).build().start(new OnDownloadListener(){
			@Override
			public void onDownloadComplete() {

			}

			@Override
			public void onError(Error error) {

			}
		});

		return false;
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

		PRDownloader.download(downloadURL, context.getCacheDir().getPath(), PYTORCH_LIB).build().start(new OnDownloadListener() {
			@Override
			public void onDownloadComplete() {}

			@Override
			public void onError(Error error) {}
		});
	}
}