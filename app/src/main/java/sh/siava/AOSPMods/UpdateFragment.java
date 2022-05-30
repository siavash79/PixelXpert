package sh.siava.AOSPMods;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.solver.widgets.Helper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.security.auth.callback.Callback;

import sh.siava.AOSPMods.Utils.Helpers;
import sh.siava.AOSPMods.databinding.UpdateFragmentBinding;

public class UpdateFragment extends Fragment {
    private static final String stableUpdatesURL = "https://raw.githubusercontent.com/siavash79/AOSPMods/stable/MagiskModuleUpdate.json";
    private static final String canaryUpdatesURL = "https://raw.githubusercontent.com/siavash79/AOSPMods/canary/latestVersion.json";
    DownloadManager downloadManager;
    long downloadID = 0; //from download manager
    boolean canaryUpdate = false;
    HashMap<String, Object> latestVersion = null;
    private String downloadedFilePath;
    BroadcastReceiver downloadCompletionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (Objects.equals(intent.getAction(), DownloadManager.ACTION_DOWNLOAD_COMPLETE) && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadID) {
                    Cursor downloadData = downloadManager.query(
                            new DownloadManager.Query()
                                    .setFilterById(downloadID)
                    );

                    downloadData.moveToFirst();

                    int uriColIndex = downloadData.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                    if (uriColIndex > 0) {
                        downloadedFilePath = new File(URI.create(downloadData.getString(uriColIndex))).getAbsolutePath();

                        notifyInstall();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private UpdateFragmentBinding binding;
    private int currentVersionCode = -1;
    private String currentVersionName = "";
    private boolean rebootPending = false;
    private boolean downloadStarted = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        //noinspection ConstantConditions
        downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);


        //finally
        binding = UpdateFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Shell.cmd("echo").exec(); //will ask root if not granted yet

        if (!Shell.getShell().isRoot()) {
            currentVersionName = getString(R.string.root_not_here);
            currentVersionCode = 9999;
        } else {
            getCurrentVersion();
        }
        String pendingRebootString = (rebootPending) ? " - " + getString(R.string.reboot_pending) : "";
        ((TextView) view.findViewById(R.id.currentVersionValueID)).setText(String.format("%s (%s)%s", currentVersionName, currentVersionCode, pendingRebootString));

        if(rebootPending)
        {
            binding.updateBtn.setEnabled(true);
            binding.updateBtn.setText(R.string.reboot_word);
        }

        binding.radioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            canaryUpdate = ((RadioButton) radioGroup.findViewById(R.id.canaryID)).isChecked();
            ((TextView) view.findViewById(R.id.latestVersionValueID)).setText(R.string.update_checking);
            binding.updateBtn.setEnabled(rebootPending || false);

            checkUpdates(result -> {
                latestVersion = result;
                //noinspection ConstantConditions
                getActivity().runOnUiThread(() -> {
                    ((TextView) view.findViewById(R.id.latestVersionValueID)).setText(
                            String.format("%s (%s)", result.get("version"),
                                    result.get("versionCode")));
                    int latestCode = 0;
                    boolean enable = false;
                    try {
                        //noinspection ConstantConditions
                        latestCode = (int) result.get("versionCode");
                        if(!canaryUpdate) enable = true; //stable version is ALWAYS flashable, so that user can revert from canary or repair installation
                    } catch (Exception ignored) {}
                    enable = rebootPending || ((enable || latestCode > currentVersionCode) && !downloadStarted);
                    view.findViewById(R.id.updateBtn).setEnabled(enable);
                });
            });
        });
        binding.updateBtn.setOnClickListener(view1 -> {
            if(rebootPending)
            {
                Shell.cmd("reboot").exec();
            }
            else {
                //noinspection ConstantConditions
                startDownload((String) latestVersion.get("zipUrl"), (int) latestVersion.get("versionCode"));
                binding.updateBtn.setEnabled(false);
                downloadStarted = true;
                binding.updateBtn.setText(R.string.update_download_started);
            }
        });
    }

    private void getCurrentVersion() {
        rebootPending = false;
        try {
            List<String> updateLines = Shell.cmd("cat /data/adb/modules_update/AOSPMods/module.prop | grep version").exec().getOut();
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
        } catch (Exception e) {
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
                .setTitle("AOSPMods Update Package")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, String.format("AOSPMods-%s.zip", versionNumber))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE));

        //noinspection ConstantConditions
        getContext().registerReceiver(downloadCompletionReceiver, filters);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void notifyInstall() {
        Intent notificationIntent = new Intent(getContext(), UpdateActivity.class);
        notificationIntent.setAction(Intent.ACTION_RUN);
        notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
        notificationIntent.putExtra("updateTapped", true);
        notificationIntent.putExtra("filePath", downloadedFilePath);

        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        //noinspection ConstantConditions
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "updates")
                .setSmallIcon(R.drawable.ic_notification_foreground)
                .setContentTitle(getContext().getString(R.string.update_notification_title))
                .setContentText(getContext().getString(R.string.update_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(getContext()).notify(1, builder.build());
    }

    public interface TaskDoneCallback extends Callback {
        void onFinished(HashMap<String, Object> result);
    }

    private class updateChecker extends Thread {
        private final TaskDoneCallback mCallback;

        private updateChecker(TaskDoneCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            try {
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
                        case "version":
                        case "changelog":
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