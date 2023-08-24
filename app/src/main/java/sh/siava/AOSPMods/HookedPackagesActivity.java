package sh.siava.AOSPMods;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import sh.siava.AOSPMods.databinding.ActivityHookedPackagesBinding;
import sh.siava.AOSPMods.modpacks.Constants;

@SuppressWarnings("FieldCanBeLocal")
public class HookedPackagesActivity extends AppCompatActivity {

	private final String TAG = getClass().getSimpleName();
	private ActivityHookedPackagesBinding binding;
	IntentFilter intentFilterHookedPackages = new IntentFilter();
	private final List<String> hookedPackageList = new ArrayList<>();
	private final StringBooleanMap map = new StringBooleanMap();
	private List<String> packageList;
	private final String SQLite3 = "/data/adb/modules/AOSPMods/sqlite3";
	private final String LSPosedDB = "/data/adb/lspd/config/modules_config.db";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityHookedPackagesBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		setTitle(getText(R.string.hooked_packages_title));

		Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.color_surface_overlay));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		intentFilterHookedPackages.addAction(Constants.ACTION_XPOSED_CONFIRMED);
		registerReceiver(receiverHookedPackages, intentFilterHookedPackages, RECEIVER_EXPORTED);

		packageList = Arrays.asList(getResources().getStringArray(R.array.module_scope));
		checkHookedPackages();

		SwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
		mSwipeRefreshLayout.setOnRefreshListener(() -> {
			checkHookedPackages();
			mSwipeRefreshLayout.setRefreshing(false);
		});
	}

	private final BroadcastReceiver receiverHookedPackages = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), Constants.ACTION_XPOSED_CONFIRMED)) {
				String packageName = intent.getStringExtra("packageName");
				boolean isRoot = intent.getBooleanExtra("isRoot", false);
				map.put(packageName, isRoot);

				if (!hookedPackageList.contains(packageName)) {
					hookedPackageList.add(packageName);
				}
				Log.i(TAG, "packageName: " + packageName + " isRoot: " + isRoot);
			}
		}
	};

	private void checkHookedPackages() {
		hookedPackageList.clear();
		map.clear();

		initListItem(packageList);
		new Thread(() -> sendBroadcast(new Intent().setAction(Constants.ACTION_CHECK_XPOSED_ENABLED))).start();
		waitAndRefresh();
	}

	private void waitAndRefresh() {
		new CountDownTimer(3000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
			}

			@Override
			public void onFinish() {
				refreshListItem();
			}
		}.start();
	}

	private void initListItem(List<String> pack) {
		if (binding.content.getChildCount() > 0)
			binding.content.removeAllViews();

		for (int i = 0; i < pack.size(); i++) {
			View list = LayoutInflater.from(this).inflate(R.layout.view_hooked_package_list, binding.content, false);
			if (i == 0) {
				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) list.getLayoutParams();
				int margin = getResources().getDimensionPixelSize(R.dimen.ui_container_margin_side);
				params.setMargins(margin, dp2px(this, 12), margin, dp2px(this, 6));
			} else if (i == pack.size() - 1) {
				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) list.getLayoutParams();
				int margin = getResources().getDimensionPixelSize(R.dimen.ui_container_margin_side);
				params.setMargins(margin, dp2px(this, 6), margin, dp2px(this, 12));
			}

			TextView title = list.findViewById(R.id.title);
			title.setText(pack.get(i));

			TextView desc = list.findViewById(R.id.desc);
			if (isAppInstalled(pack.get(i))) {
				desc.setText(getText(R.string.package_checking));
			} else {
				desc.setText(getText(R.string.package_not_found));
				desc.setTextColor(getColor(R.color.error));
			}

			TextView root = list.findViewById(R.id.root);
			if (map.get(pack.get(i))) {
				root.setText(getText(R.string.root_granted));
				root.setVisibility(View.VISIBLE);
			}

			ImageView preview = list.findViewById(R.id.icon);
			preview.setImageDrawable(getAppIcon(pack.get(i)));

			int finalI = i;
			list.setOnClickListener(view -> {
				try {
					Intent intent = getPackageManager().getLaunchIntentForPackage(pack.get(finalI));
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
			TextView root = list.findViewById(R.id.root);
			String pkgName = ((TextView) list.findViewById(R.id.title)).getText().toString();

			if (hookedPackageList.contains(pkgName)) {
				desc.setText(getText(R.string.package_hooked_successful));
				desc.setTextColor(getColor(R.color.success));
			} else {
				if (isAppInstalled(pkgName)) {
					if (hookedPackageList.contains(BuildConfig.APPLICATION_ID) && checkLSPosedDB(pkgName)) {
						desc.setText(getText(R.string.package_hooked_successful));
						desc.setTextColor(getColor(R.color.success));
					} else {
						desc.setText(getText(R.string.package_hooked_fail));
						desc.setTextColor(getColor(R.color.error));
					}
				} else {
					desc.setText(getText(R.string.package_not_found));
					desc.setTextColor(getColor(R.color.error));
				}
			}

			if (map.get(pkgName)) {
				root.setText(getText(R.string.root_granted));
			} else {
				root.setText(getText(R.string.root_not_granted));
			}
			if (isAppInstalled(pkgName)) {
				root.setVisibility(View.VISIBLE);
			}
		}
	}

	private boolean isAppInstalled(String packageName) {
		PackageManager pm = getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			return pm.getApplicationInfo(packageName, 0).enabled;
		} catch (PackageManager.NameNotFoundException ignored) {
		}
		return false;
	}

	private Drawable getAppIcon(String packageName) {
		Drawable appIcon = ContextCompat.getDrawable(this, R.drawable.ic_android);
		try {
			appIcon = getPackageManager().getApplicationIcon(packageName);
		} catch (PackageManager.NameNotFoundException ignored) {
		}
		return appIcon;
	}

	private boolean checkLSPosedDB(String pkgName) {
		return Shell.cmd(SQLite3 + ' ' + LSPosedDB + " \"SELECT EXISTS (SELECT 1 FROM scope WHERE app_pkg_name = '" + pkgName + "');\"").exec().getOut().get(0).equals("1");
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
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiverHookedPackages);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}
}