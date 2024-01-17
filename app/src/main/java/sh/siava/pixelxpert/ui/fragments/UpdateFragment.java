package sh.siava.pixelxpert.ui.fragments;

import static android.content.Context.RECEIVER_EXPORTED;
import static sh.siava.pixelxpert.utils.AppUtils.installDoubleZip;
import static sh.siava.pixelxpert.utils.MiscUtils.getColorFromAttribute;
import static sh.siava.pixelxpert.utils.MiscUtils.intToHex;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonReader;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.callback.Callback;

import br.tiagohm.markdownview.MarkdownView;
import br.tiagohm.markdownview.css.InternalStyleSheet;
import br.tiagohm.markdownview.css.styles.Github;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.UpdateFragmentBinding;
import sh.siava.pixelxpert.modpacks.utils.ModuleFolderOperations;
import sh.siava.pixelxpert.ui.activities.SettingsActivity;
import sh.siava.pixelxpert.utils.AppUtils;
import sh.siava.pixelxpert.utils.PreferenceHelper;
import sh.siava.rangesliderpreference.RangeSliderPreference;


public class UpdateFragment extends Fragment {
	public static final String MOD_NAME = "PixelXpert";
	public static final String MAGISK_UPDATE_DIR = "/data/adb/modules_update";
	public static final String MAGISK_MODULES_DIR = "/data/adb/modules";
	private static final String updateRoot = String.format("%s/%s", MAGISK_UPDATE_DIR, MOD_NAME);

	public static final String UPDATES_CHANNEL_ID = "Updates";
	private static final String stableUpdatesURL = "https://raw.githubusercontent.com/siavash79/PixelXpert/stable/latestStable.json";
	private static final String canaryUpdatesURL = "https://raw.githubusercontent.com/siavash79/PixelXpert/canary/latestCanary.json";
	DownloadManager downloadManager;
	long downloadID = 0; //from download manager
	static boolean canaryUpdate = BuildConfig.VERSION_NAME.toLowerCase().contains("canary");
	HashMap<String, Object> latestVersion = null;
	private String downloadedFilePath;
	private static final String updateDir = String.format("%s/%s", MAGISK_UPDATE_DIR, MOD_NAME);
	private static final String moduleDir = String.format("%s/%s", MAGISK_MODULES_DIR, MOD_NAME);

	BroadcastReceiver downloadCompletionReceiver = new BroadcastReceiver() {
		@SuppressLint("MissingPermission")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (getContext() != null)
				getContext().unregisterReceiver(downloadCompletionReceiver);


			boolean successful = false;
			if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()) && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadID) {
				try (Cursor downloadData = downloadManager.query(
						new DownloadManager.Query()
								.setFilterById(downloadID))) {
					downloadData.moveToFirst();

					int uriColIndex = downloadData.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

					File downloadedFile = new File(URI.create(downloadData.getString(uriColIndex)));

					if (downloadedFile.exists()) {
						downloadedFilePath = new File(URI.create(downloadData.getString(uriColIndex))).getAbsolutePath();

						notifyInstall();
						successful = true;
					}
				} catch (Throwable ignored) {
				}
			}

			if (!successful) {
				NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), UPDATES_CHANNEL_ID)
						.setSmallIcon(R.drawable.ic_notification_foreground)
						.setContentTitle(requireContext().getText(R.string.download_failed))
						.setContentText(requireContext().getText(R.string.try_again_later))
						.setPriority(NotificationCompat.PRIORITY_DEFAULT);

				NotificationManagerCompat.from(requireContext()).notify(2, builder.build());
			}
		}
	};
	private UpdateFragmentBinding binding;
	private int currentVersionCode = -1;
	private int currentVersionType = PreferenceHelper.XPOSED_ONLY;
	private String currentVersionName = "";
	private boolean rebootPending = false;
	//	private boolean downloadStarted = false;
	private boolean installFullVersion = false;

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		//noinspection ConstantConditions
		downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);


		//finally
		binding = UpdateFragmentBinding.inflate(inflater, container, false);

		if (getArguments() != null && getArguments().getBoolean("updateTapped", false)) {
			String downloadPath = getArguments().getString("filePath");

			installDoubleZip(downloadPath);
		} else if (getArguments() != null && "true".equals(getArguments().getString("migratePrefs"))) //received intent from magisk, showing installation is done
		{
			applyPrefsToUpdate();
		}

		return binding.getRoot();
	}

	private void applyPrefsToUpdate() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext().createDeviceProtectedStorageContext());

		int volumeStps = RangeSliderPreference.getSingleIntValue(prefs, "volumeStps", 0);
		boolean customFontsEnabled = prefs.getBoolean("enableCustomFonts", false);
		boolean GSansOverrideEnabled = prefs.getBoolean("gsans_override", false);

		ModuleFolderOperations.applyVolumeSteps(volumeStps, updateRoot, true);
		ModuleFolderOperations.applyFontSettings(customFontsEnabled, GSansOverrideEnabled, updateRoot, true);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		//Android 13 requires notification permission to be granted or it won't allow it
		Shell.cmd(String.format("pm grant %s android.permission.POST_NOTIFICATIONS", BuildConfig.APPLICATION_ID)).exec(); //will ask root if not granted yet

		if (!Shell.getShell().isRoot()) {
			currentVersionName = getString(R.string.root_not_here);
			currentVersionType = -1;
			currentVersionCode = 9999;
		} else {
			getCurrentVersion();
		}

		String pendingRebootString = (rebootPending) ? " - " + getString(R.string.reboot_pending) : "";
		((TextView) view.findViewById(R.id.currentVersionValueID)).setText(String.format("%s (%s)%s", currentVersionName, currentVersionCode, pendingRebootString));

		if (rebootPending) {
			binding.updateBtn.setEnabled(true);
			binding.updateBtn.setText(R.string.reboot_word);
		}

		RadioGroup.OnCheckedChangeListener onCheckChangedListener = (radioGroup, i) -> {
			canaryUpdate = ((RadioButton) radioGroup.findViewById(R.id.canaryID)).isChecked();
			((TextView) view.findViewById(R.id.latestVersionValueID)).setText(R.string.update_checking);
			binding.updateBtn.setEnabled(rebootPending);

			checkUpdates(result -> {
				latestVersion = result;

				if (getActivity() != null) {
					requireActivity().runOnUiThread(() -> {
						try {
							MarkdownView mMarkdownView = view.findViewById(R.id.changelogView);
							InternalStyleSheet css = new Github();
							css.addRule("body, kbd", "background-color: " + intToHex(getColorFromAttribute(requireContext(), R.attr.colorSurfaceContainer)));
							css.addRule("body, p, h1, h2, h3, h4, h5, h6, span, div", "color: " + intToHex(getColorFromAttribute(requireContext(), R.attr.colorOnSurface)));
							css.addRule("kbd", "border-color: " + intToHex(getColorFromAttribute(requireContext(), R.attr.colorSurfaceVariant)));
							css.addRule("kbd", "color: " + intToHex(getColorFromAttribute(requireContext(), R.attr.colorOnSurface)));
							css.addRule("a", "color: " + intToHex(getColorFromAttribute(requireContext(), R.attr.colorPrimary)));
							mMarkdownView.addStyleSheet(css);
							mMarkdownView.loadMarkdownFromUrl((String) result.get("changelog"));
						} catch (Throwable ignored) {
						}
					});
				}

				if (getActivity() != null) {
					requireActivity().runOnUiThread(() -> {
						((TextView) view.findViewById(R.id.latestVersionValueID)).setText(
								String.format("%s (%s)", result.get("version"),
										result.get("versionCode")));
						int latestCode;
						int BtnText = R.string.update_word;

						boolean enable = false;
						try {
							//noinspection ConstantConditions
							latestCode = (int) result.get("versionCode");

							if (rebootPending) {
								enable = true;
								BtnText = R.string.reboot_word;
							} else if (!canaryUpdate) //stable selected
							{
								if (currentVersionName.contains("-")) //currently canary installed
								{
									BtnText = R.string.switch_branches;
								} else if (latestCode == currentVersionCode) //already up to date
								{
									BtnText = R.string.reinstall_word;
								}
								enable = true; //stable version is ALWAYS flashable, so that user can revert from canary or repair installation
							} else {
								if (latestCode > currentVersionCode || (currentVersionType == PreferenceHelper.FULL_VERSION) != installFullVersion) {
									enable = true;
								}
							}
						} catch (Exception ignored) {
						}
						view.findViewById(R.id.updateBtn).setEnabled(enable);
						((Button) view.findViewById(R.id.updateBtn)).setText(BtnText);
					});
				}
			});
		};

		binding.updateChannelRadioGroup.setOnCheckedChangeListener(onCheckChangedListener);

		binding.packageTypeRadioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
			onCheckChangedListener.onCheckedChanged(view.findViewById(R.id.updateChannelRadioGroup), 0);
		});

		binding.updateBtn.setOnClickListener(view1 -> {
			if (rebootPending) {
				AppUtils.Restart("system");
			} else {
				String zipURL = (String) latestVersion.get("zipUrl_Xposed");
				if (zipURL == null) zipURL = (String) latestVersion.get("zipUrl");

				//noinspection ConstantConditions
				startDownload(zipURL, (int) latestVersion.get("versionCode"));
				binding.updateBtn.setEnabled(false);
//				downloadStarted = true;
				binding.updateBtn.setText(R.string.update_download_started);
			}
		});

		if (currentVersionType == PreferenceHelper.FULL_VERSION) {
			((RadioButton) view.findViewById(R.id.fullTypeID)).setChecked(true);
		} else {
			((RadioButton) view.findViewById(R.id.XposedTypeID)).setChecked(true);
		}

		if (currentVersionName.toLowerCase().contains("canary")) {
			((RadioButton) view.findViewById(R.id.canaryID)).setChecked(true);
		} else {
			((RadioButton) view.findViewById(R.id.stableID)).setChecked(true);
		}
	}

/*    private void getChangelog(String URL, TaskDoneCallback callback) {
        new ChangelogReceiver(URL, callback).start();
    }*/

	private void getCurrentVersion() {
		rebootPending = false;

		try {
			List<String> updateLines = Shell.cmd(String.format("cat %s/module.prop | grep version", updateDir)).exec().getOut();
			if (updateLines.size() >= 2) {
				for (String line : updateLines) {
					if (line.toLowerCase().contains("code")) {
						currentVersionCode = Integer.parseInt(line.substring(line.indexOf("=") + 1));
					} else {
						currentVersionName = line.substring(line.indexOf("=") + 1);
					}
				}
				rebootPending = true;
			} else {
				throw new Exception();
			}
		} catch (Exception ignored) {
			rebootPending = false;
			currentVersionName = BuildConfig.VERSION_NAME;
			currentVersionCode = BuildConfig.VERSION_CODE;
		}
	}

	public void checkUpdates(TaskDoneCallback callback) {
		new updateChecker(callback).start();
	}

	public void startDownload(String zipURL, int versionNumber) {
		IntentFilter filters = new IntentFilter();
		filters.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		filters.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);

		downloadID = downloadManager.enqueue(new DownloadManager.Request(Uri.parse(zipURL))
				.setTitle("PixelXpert Update Package")
				.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, String.format("PixelXpert-%s.zip", versionNumber))
				.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE));

		//noinspection ConstantConditions
		if (getContext() != null) {
			getContext().registerReceiver(downloadCompletionReceiver, filters, RECEIVER_EXPORTED);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@SuppressLint("MissingPermission")
	public void notifyInstall() {
		if (getContext() == null) {
			Log.w("UpdateFragment", "notifyInstall: context is null");
			return;
		}

		Intent notificationIntent = new Intent(getContext(), SettingsActivity.class);
		notificationIntent.setAction(Intent.ACTION_RUN);
		notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
		notificationIntent.putExtra("updateTapped", true);
		notificationIntent.putExtra("filePath", downloadedFilePath);

		PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

		//noinspection ConstantConditions
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), UPDATES_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_notification_foreground)
				.setContentTitle(requireContext().getString(R.string.update_notification_title))
				.setContentText(requireContext().getString(R.string.update_notification_text))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setContentIntent(pendingIntent)
				.setAutoCancel(true);

		NotificationManagerCompat.from(getContext()).notify(1, builder.build());
	}

	public interface TaskDoneCallback extends Callback {
		void onFinished(HashMap<String, Object> result);
	}
/*    private static class ChangelogReceiver extends Thread {
        private final TaskDoneCallback mCallback;
        private final String mURL;

        private ChangelogReceiver(String URL, TaskDoneCallback callback) {
            mURL = URL;
            mCallback = callback;
        }

        @Override
        public void run()
        {
            try {
                URL changelogData = new URL(mURL);
                InputStream s = changelogData.openStream();

                BufferedReader in = new BufferedReader(new InputStreamReader(s));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line).append("\n");
                }
                HashMap<String, Object> returnVal = new HashMap<>();
                returnVal.put("changelog", result.toString());

                mCallback.onFinished(returnVal);
            } catch (Exception ignored){}
        }
    }
*/

	public static class updateChecker extends Thread {
		private final TaskDoneCallback mCallback;

		public updateChecker(TaskDoneCallback callback) {
			mCallback = callback;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(200); //waiting for canaryUpdate variable to initialize
				URL updateData = new URL((canaryUpdate) ? canaryUpdatesURL : stableUpdatesURL);
				InputStream s = updateData.openStream();
				InputStreamReader r = new InputStreamReader(s);
				JsonReader jsonReader = new JsonReader(r);

				HashMap<String, Object> versionInfo = new HashMap<>();
				jsonReader.beginObject();
				while (jsonReader.hasNext()) {
					String name = jsonReader.nextName();
					switch (name) {
						case "versionCode":
							versionInfo.put(name, jsonReader.nextInt());
							break;
						case "zipUrl":
						case "zipUrl_Xposed":
						case "zipUrl_Full":
						case "version":
						case "changelog":
						default:
							versionInfo.put(name, jsonReader.nextString());
							break;
					}
				}
				mCallback.onFinished(versionInfo);
			} catch (Exception e) {
				HashMap<String, Object> error = new HashMap<>();
				error.put("version", "Connection Error");
				error.put("versionCode", -1);
				mCallback.onFinished(error);
				e.printStackTrace();
			}
		}
	}
}