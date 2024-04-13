package sh.siava.pixelxpert.modpacks.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.nio.FloatBuffer;

public class BitmapSubjectSegmenter {
	SubjectSegmenter mSegmenter;
	public BitmapSubjectSegmenter()
	{
		mSegmenter = SubjectSegmentation.getClient(
				new SubjectSegmenterOptions.Builder()
						.enableForegroundConfidenceMask()
						.build());
	}

	public void segmentSubject(Bitmap inputBitmap, SegmentResultListener listener)
	{
		int transparentColor = Color.alpha(Color.TRANSPARENT);

		Bitmap resultBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);

		mSegmenter.process(InputImage.fromBitmap(inputBitmap, 0))
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

					inputBitmap.recycle();
					listener.onSuccess(resultBitmap);
				})
				.addOnFailureListener(e -> {
					inputBitmap.recycle();
					listener.onFail();
				});

	}

	public interface SegmentResultListener{
		void onSuccess(Bitmap result);
		void onFail();
	}
}