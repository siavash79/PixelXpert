package sh.siava.pixelxpert.ui.fragments;

import static android.content.Context.RECEIVER_EXPORTED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.topjohnwu.superuser.ipc.RootService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import sh.siava.pixelxpert.IRootProviderService;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.FragmentHooksBinding;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.service.RootProvider;
import sh.siava.pixelxpert.ui.activities.BaseActivity;
import sh.siava.pixelxpert.utils.AppUtils;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class HooksFragment extends Fragment {

	private FragmentHooksBinding binding;
	private final String TAG = getClass().getSimpleName();
	IntentFilter intentFilterHookedPackages = new IntentFilter();
	private final List<String> hookedPackageList = new ArrayList<>();
	private List<String> monitorPackageList;
	private final String SQLite3 = "/data/adb/modules/PixelXpert/sqlite3";
	private final String LSPosedDB = "/data/adb/lspd/config/modules_config.db";
	private int dotCount = 0;
	private ServiceConnection mCoreRootServiceConnection;
	private IRootProviderService mRootServiceIPC = null;
	private boolean rebootPending = false;
	private final String reboot_key = "reboot_pending";

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentHooksBinding.inflate(inflater, container, false);

		if (savedInstanceState != null) {
			rebootPending = savedInstanceState.getBoolean(reboot_key);
		}

		binding.rebootButton.setOnClickListener(view -> AppUtils.Restart("system"));

		if (!rebootPending) {
			binding.rebootButton.hide();
		}

		startRootService();

		return binding.getRoot();
	}

	private void startRootService() {
		// Start RootService connection
		Intent intent = new Intent(requireContext(), RootProvider.class);
		mCoreRootServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				binding.loadingIndicator.setVisibility(GONE);
				mRootServiceIPC = IRootProviderService.Stub.asInterface(service);
				onRootServiceStarted();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mRootServiceIPC = null;
			}
		};
		RootService.bind(intent, mCoreRootServiceConnection);
	}

	private void onRootServiceStarted() {
		if (getContext() == null) {
			return;
		}

		intentFilterHookedPackages.addAction(Constants.ACTION_XPOSED_CONFIRMED);
		requireContext().registerReceiver(receiverHookedPackages, intentFilterHookedPackages, RECEIVER_EXPORTED);

		monitorPackageList = Arrays.asList(getResources().getStringArray(R.array.module_scope));
		checkHookedPackages();

		binding.swipeRefreshLayout.setOnRefreshListener(() -> {
			checkHookedPackages();
			binding.swipeRefreshLayout.setRefreshing(false);
		});
	}

	private final BroadcastReceiver receiverHookedPackages = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), Constants.ACTION_XPOSED_CONFIRMED)) {
				String broadcastPackageName = intent.getStringExtra("packageName");

				for (int i = 0; i < binding.content.getChildCount(); i++) {
					View list = binding.content.getChildAt(i);
					TextView desc = list.findViewById(R.id.desc);
					String pkgName = ((TextView) list.findViewById(R.id.title)).getText().toString();

					if (pkgName.equals(broadcastPackageName)) {
						binding.content.post(() -> {
							desc.setText(getText(R.string.package_hooked_successful));
							desc.setTextColor(requireContext().getColor(R.color.success));
						});
					}
				}

				if (!hookedPackageList.contains(broadcastPackageName)) {
					hookedPackageList.add(broadcastPackageName);
				}
			}
		}
	};

	private final CountDownTimer countDownTimer = new CountDownTimer(5000, 500) {
		@Override
		public void onTick(long millisUntilFinished) {
			dotCount = (dotCount + 1) % 4;
			String dots = new String(new char[dotCount]).replace('\0', '.');

			for (int i = 0; i < binding.content.getChildCount(); i++) {
				View list = binding.content.getChildAt(i);
				TextView desc = list.findViewById(R.id.desc);

				if (((String) desc.getText()).contains(getString(R.string.package_checking, ""))) {
					desc.setText(getString(R.string.package_checking, dots));
				}
			}
		}

		@Override
		public void onFinish() {
			dotCount = 0;
			refreshListItem();
		}
	};

	private void checkHookedPackages() {
		hookedPackageList.clear();

		initListItem(monitorPackageList);
		new Thread(() -> requireContext().sendBroadcast(new Intent().setAction(Constants.ACTION_CHECK_XPOSED_ENABLED))).start();
		waitAndRefresh();
	}

	private void waitAndRefresh() {
		countDownTimer.start();
	}

	private void initListItem(List<String> pack) {
		dotCount = 0;
		countDownTimer.cancel();

		if (binding.content.getChildCount() > 0) {
			binding.content.removeAllViews();
		}

		for (int i = 0; i < pack.size(); i++) {
			View list = LayoutInflater.from(requireContext()).inflate(R.layout.view_hooked_package_list, binding.content, false);
			int margin = getResources().getDimensionPixelSize(R.dimen.ui_container_margin_side);
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) list.getLayoutParams();
			if (i == 0) {
				params.setMargins(margin, dp2px(requireContext(), 12), margin, dp2px(requireContext(), 6));
			} else if (i == pack.size() - 1) {
				params.setMargins(margin, dp2px(requireContext(), 6), margin, dp2px(requireContext(), 12));
			}

			TextView title = list.findViewById(R.id.title);
			title.setText(pack.get(i));

			TextView desc = list.findViewById(R.id.desc);
			if (isAppInstalled(pack.get(i))) {
				desc.setText(getString(R.string.package_checking, ""));
			} else {
				desc.setText(getText(R.string.package_not_found));
				desc.setTextColor(requireContext().getColor(R.color.error));
			}

			ImageView preview = list.findViewById(R.id.icon);
			preview.setImageDrawable(getAppIcon(pack.get(i)));

			int finalI = i;

			Button activateInLSPosed = list.findViewById(R.id.activate_in_lsposed);
			activateInLSPosed.setOnClickListener(view -> {
				activateInLSPosed.setEnabled(false);
				try {
					if (mRootServiceIPC.activateInLSPosed(pack.get(finalI))) {
						activateInLSPosed.animate().setDuration(300).withEndAction(() -> activateInLSPosed.setVisibility(GONE)).start();
						Toast.makeText(requireContext(), getText(R.string.package_activated), Toast.LENGTH_SHORT).show();
						binding.rebootButton.show();
						rebootPending = true;
					} else {
						Toast.makeText(requireContext(), getText(R.string.package_activation_failed), Toast.LENGTH_SHORT).show();
						activateInLSPosed.setEnabled(true);
					}
				} catch (RemoteException e) {
					Toast.makeText(requireContext(), getText(R.string.package_activation_failed), Toast.LENGTH_SHORT).show();
					activateInLSPosed.setEnabled(true);
					e.printStackTrace();
				}
			});

			list.setOnClickListener(view -> {
				try {
					Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(pack.get(finalI));
					if (intent != null) {
						startActivity(intent);
					}
				} catch (Exception ignored) {
				}
			});

			binding.content.addView(list);
		}
	}

	private void refreshListItem() {
		for (int i = 0; i < binding.content.getChildCount(); i++) {
			View list = binding.content.getChildAt(i);
			TextView desc = list.findViewById(R.id.desc);
			String pkgName = ((TextView) list.findViewById(R.id.title)).getText().toString();

			if (hookedPackageList.contains(pkgName)) {
				desc.setText(getText(R.string.package_hooked_successful));
				desc.setTextColor(requireContext().getColor(R.color.success));
			} else {
				desc.setTextColor(requireContext().getColor(R.color.error));

				desc.setText(getText(
						isAppInstalled(pkgName)
								? checkLSPosedDB(pkgName)
								? R.string.package_hook_no_response
								: R.string.package_not_hook_enabled
								: R.string.package_not_found));
			}

			if (desc.getText() == getText(R.string.package_not_hook_enabled)) {
				Button activateInLSPosed = list.findViewById(R.id.activate_in_lsposed);
				activateInLSPosed.setVisibility(VISIBLE);
				activateInLSPosed.setEnabled(true);
			}
		}
	}

	private boolean isAppInstalled(String packageName) {
		try {
			return mRootServiceIPC.isPackageInstalled(packageName);
		} catch (RemoteException e) {
			return false;
		}
	}

	private Drawable getAppIcon(String packageName) {
		try {
			return requireContext().getPackageManager().getApplicationIcon(packageName);
		} catch (PackageManager.NameNotFoundException ignored) {
			return ContextCompat.getDrawable(requireContext(), R.drawable.ic_android);
		}
	}

	private boolean checkLSPosedDB(String pkgName) {
		try {
			return mRootServiceIPC.checkLSPosedDB(pkgName);
		} catch (RemoteException e) {
			return false;
		}
	}

	private int dp2px(Context context, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

	@SuppressWarnings("unused")
	public static class StringBooleanMap {
		private final HashMap<String, Boolean> map = new HashMap<>();

		public void put(String key, boolean value) {
			map.put(key, value);
		}

		public boolean get(String key) {
			Boolean value = map.get(key);
			return value != null ? value : false;
		}

		public boolean containsKey(String key) {
			return map.containsKey(key);
		}

		public void remove(String key) {
			map.remove(key);
		}

		public void clear() {
			map.clear();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(reboot_key, rebootPending);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			rebootPending = savedInstanceState.getBoolean(reboot_key);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		countDownTimer.cancel();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			requireContext().unregisterReceiver(receiverHookedPackages);
		} catch (Exception ignored) {
		}
		countDownTimer.cancel();
	}
}