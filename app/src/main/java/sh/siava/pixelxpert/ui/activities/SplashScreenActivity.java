package sh.siava.pixelxpert.ui.activities;

import static sh.siava.pixelxpert.utils.MiscUtils.getColorFromAttribute;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.CountDownLatch;

import sh.siava.pixelxpert.PixelXpert;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.ActivitySplashScreenBinding;
import sh.siava.pixelxpert.utils.AppUtils;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {
	/**
	 * @noinspection unused
	 */
	String TAG = getClass().getSimpleName();
	private ActivitySplashScreenBinding mBinding;
	private final CountDownLatch mRootCheckPassed = new CountDownLatch(1);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		getWindow().setStatusBarColor(getColorFromAttribute(this, R.attr.colorSurfaceContainer));

		// Root permission check
		new Thread(() -> {
			if (PixelXpert.get().hasRootAccess()) {
				mRootCheckPassed.countDown();
			} else {
				if (!getIntent().hasExtra("FromKSU")) {
					AppUtils.runKSURootActivity(this, true);
				}

				runOnUiThread(() ->
						new MaterialAlertDialogBuilder(SplashScreenActivity.this, R.style.MaterialComponents_MaterialAlertDialog)
								.setCancelable(false)
								.setMessage(getText(R.string.root_access_denied))
								.setPositiveButton(getText(R.string.exit), (dialog, i) -> System.exit(0))
								.show());
			}

			// Update the UI
			setCheckUIDone(mBinding.circularRoot.getId(), mBinding.doneRoot.getId(), mRootCheckPassed.getCount() == 0);
		}).start();

		// End splash screen and go to the main activity
		new Thread(() -> {
			try {
				// Wait for all checks to pass and for all operations to finish
				mRootCheckPassed.await();

				PixelXpert app = PixelXpert.get();
				if (!PixelXpert.get().isCoreRootServiceBound()) {
					app.tryConnectRootService();
				}

				app.mRootServiceConnected.await();

				// Update the UI
				setCheckUIDone(mBinding.circularRootService.getId(), mBinding.doneRootService.getId(), app.mRootServiceConnected.getCount() == 0);

				// This is just for aesthetics: I don't want the splashscreen to be too fast
				Thread.sleep(1000);

				if (app.mRootServiceConnected.getCount() == 0) {
					// Start the main activity
					Intent intent1 = new Intent(SplashScreenActivity.this, SettingsActivity.class);
					intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent1);
				} else {
					runOnUiThread(() ->
							new MaterialAlertDialogBuilder(SplashScreenActivity.this, R.style.MaterialComponents_MaterialAlertDialog)
									.setCancelable(false)
									.setMessage(getText(R.string.root_service_failed))
									.setPositiveButton(getText(R.string.exit), (dialog, i) -> System.exit(0))
									.show());
				}
			} catch (InterruptedException ignored) {
			}
		}).start();
	}

	private void setCheckUIDone(int circularID, int doneImageID, boolean success) {
		View circular = findViewById(circularID);
		ImageView doneImage = findViewById(doneImageID);
		runOnUiThread(() -> {
			circular.setVisibility(View.GONE);
			doneImage.setImageResource(success ? R.drawable.ic_success : R.drawable.ic_fail);
			doneImage.setVisibility(View.VISIBLE);
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}