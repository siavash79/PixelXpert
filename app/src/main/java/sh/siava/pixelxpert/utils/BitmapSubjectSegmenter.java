package sh.siava.pixelxpert.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.nio.FloatBuffer;

public class BitmapSubjectSegmenter {
	final SubjectSegmenter mSegmenter;
	final Context mContext;
	final Segmenter mSelfieSegmenter;
	public BitmapSubjectSegmenter(Context context)
	{
		mContext = context;
		mSegmenter = SubjectSegmentation.getClient(
				new SubjectSegmenterOptions.Builder()
						.enableForegroundConfidenceMask()
						.build());

		mSelfieSegmenter = Segmentation.getClient(
				new SelfieSegmenterOptions.Builder()
						.setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
						.build());

		downloadModelIfNeeded();
	}

	public void downloadModelIfNeeded()
	{
		ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(mContext);

		moduleInstallClient
				.areModulesAvailable(mSegmenter)
				.addOnSuccessListener(
						response -> {
							if (!response.areModulesAvailable()) {
								moduleInstallClient
										.installModules(
												ModuleInstallRequest.newBuilder()
														.addApi(mSegmenter)
														.build());
							}
						});
	}

	public void checkModelAvailability(OnSuccessListener<ModuleAvailabilityResponse> resultListener)
	{
		ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(mContext);

		moduleInstallClient.areModulesAvailable(mSegmenter).addOnSuccessListener(resultListener);
	}

	public void segmentSubject(Bitmap inputBitmap, SegmentResultListener listener)
	{
		int transparentColor = Color.alpha(Color.TRANSPARENT);

		Bitmap resultBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
		InputImage inputImage = InputImage.fromBitmap(inputBitmap, 0);
		inputBitmap.recycle();
		mSegmenter.process(inputImage)
				.addOnSuccessListener(subjectSegmentationResult -> {
					FloatBuffer mSubjectMask = subjectSegmentationResult.getForegroundConfidenceMask();

					resultBitmap.setHasAlpha(true);
					for(int y = 0; y < inputBitmap.getHeight(); y++)
					{
						for(int x = 0; x < inputBitmap.getWidth(); x++)
						{
							//noinspection DataFlowIssue
							if(mSubjectMask.get() < .5f)
							{
								resultBitmap.setPixel(x, y, transparentColor);
							}
						}
					}

					listener.onSuccess(resultBitmap);
				})
				.addOnFailureListener(e -> {
					mSelfieSegmenter.process(inputImage)
							.addOnSuccessListener(segmentationMask -> {
								Log.d("BITMAPP", "segmentSubject: result");
								resultBitmap.setHasAlpha(true);
								for(int y = 0; y < inputBitmap.getHeight(); y++)
								{
									for(int x = 0; x < inputBitmap.getWidth(); x++)
									{
										//noinspection DataFlowIssue
										if(segmentationMask.getBuffer().get() < .5f)
										{
											resultBitmap.setPixel(x, y, transparentColor);
										}
									}
								}

								inputBitmap.recycle();
								listener.onSuccess(resultBitmap);
							})
							.addOnFailureListener(e1 -> {
								Log.d("BITMAPP", "segmentSubject: fail");
								inputBitmap.recycle();
								listener.onFail();
							});
				});

	}

	public interface SegmentResultListener{
		void onSuccess(Bitmap result);
		void onFail();
	}
}