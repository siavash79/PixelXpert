package sh.siava.AOSPMods.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.concurrent.CountDownLatch;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.SettingsActivity;
import sh.siava.AOSPMods.databinding.ActivitySplashScreenBinding;
import sh.siava.AOSPMods.service.RootProvider;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

/*	static {
		Shell.enableVerboseLogging = BuildConfig.DEBUG;
		Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
	}*/

	String TAG = getClass().getSimpleName();
	private ActivitySplashScreenBinding mBinding;
	private final CountDownLatch mRootCheckPassed = new CountDownLatch(1);
	private final CountDownLatch mRootServiceConnected = new CountDownLatch(1);
	private boolean mCoreRootServiceBound = false;
	private ServiceConnection mCoreRootServiceConnection;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());

		if (getSupportActionBar() != null) {
			getSupportActionBar().hide();
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		getWindow().setStatusBarColor(getColor(R.color.ui_bg));

		// Start RootService connection
		Intent intent = new Intent(this, RootProvider.class);
		mCoreRootServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(TAG, "onServiceConnected: ");
				mCoreRootServiceBound = true;
				mRootServiceConnected.countDown();

				// Update the UI
				setCheckUIDone(mBinding.circularRootService.getId(), mBinding.doneRootService.getId(), mRootServiceConnected.getCount() == 0);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.d(TAG, "onServiceDisconnected: ");
				mCoreRootServiceBound = false;
				mRootServiceConnected.countDown();
			}
		};
		RootService.bind(intent, mCoreRootServiceConnection);

		// Root permission check
		new Thread(() -> {
			if (Shell.getShell().isRoot()) {
				mRootCheckPassed.countDown();
			} else {
				runOnUiThread(() ->
						new AlertDialog.Builder(SplashScreenActivity.this)
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
				mRootServiceConnected.await();

				// This is just for aesthetics: I don't want the splashscreen to be too fast
				Thread.sleep(1000);

				// Start the main activity
				Intent intent1 = new Intent(SplashScreenActivity.this, SettingsActivity.class);
				intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent1);
			} catch (InterruptedException e) {
				Log.e(TAG, e.toString());
			}
		}).start();
	}

	private void setCheckUIDone(int circularID, int doneImageID, boolean success) {
		CircularProgressIndicator circular = findViewById(circularID);
		ImageView doneImage = findViewById(doneImageID);
		runOnUiThread(() -> {
			circular.setVisibility(View.GONE);
			doneImage.setImageResource(success ? R.drawable.ic_success : R.drawable.ic_fail);
			doneImage.setVisibility(View.VISIBLE);
		});
	}

	@Override
	protected void onDestroy() {
		if (mCoreRootServiceBound)
			RootService.unbind(mCoreRootServiceConnection);
		super.onDestroy();
	}
}