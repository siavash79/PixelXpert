package sh.siava.AOSPMods.ui.activities;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import static sh.siava.AOSPMods.ui.fragments.UpdateFragment.UPDATES_CHANNEL_ID;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.utils.UpdateScheduler;
import sh.siava.AOSPMods.ui.preferences.preferencesearch.SearchConfiguration;
import sh.siava.AOSPMods.ui.preferences.preferencesearch.SearchPreference;
import sh.siava.AOSPMods.ui.preferences.preferencesearch.SearchPreferenceResult;
import sh.siava.AOSPMods.ui.preferences.preferencesearch.SearchPreferenceResultListener;
import sh.siava.AOSPMods.utils.AppUtils;
import sh.siava.AOSPMods.utils.ControlledPreferenceFragmentCompat;
import sh.siava.AOSPMods.utils.PrefManager;
import sh.siava.AOSPMods.utils.PreferenceHelper;
import sh.siava.rangesliderpreference.RangeSliderPreference;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SearchPreferenceResultListener {
	private static final int REQUEST_IMPORT = 7;
	private static final int REQUEST_EXPORT = 9;
	private static final String TITLE_TAG = "settingsActivityTitle";
	Context DPContext;

	String TAG = getClass().getSimpleName();

	private static FragmentManager fragmentManager;
	private HeaderFragment headerFragment;
	private static final List<Object[]> prefsList = new ArrayList<>();

	private static ActionBar actionBar;

	public static void backButtonEnabled() {
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	public void backButtonDisabled() {
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(false);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DPContext = this.createDeviceProtectedStorageContext();
		DPContext.moveSharedPreferencesFrom(this, BuildConfig.APPLICATION_ID + "_preferences");
		super.onCreate(savedInstanceState);

		backButtonDisabled();
		createNotificationChannel();
		fragmentManager = getSupportFragmentManager();
		actionBar = getSupportActionBar();

		prefsList.add(new Object[]{R.xml.header_preferences, R.string.app_name, new HeaderFragment()});
		prefsList.add(new Object[]{R.xml.dialer_prefs, R.string.dialer_header, new DialerFragment()});
		prefsList.add(new Object[]{R.xml.gesture_nav_prefs, R.string.gesturenav_header, new GestureNavFragment()});
		prefsList.add(new Object[]{R.xml.hotspot_prefs, R.string.hotspot_header, new HotSpotFragment()});
		prefsList.add(new Object[]{R.xml.lock_screen_prefs, R.string.lockscreen_header_title, new LockScreenFragment()});
		prefsList.add(new Object[]{R.xml.lsqs_custom_text, R.string.netstat_header, new NetworkStatFragment()});
		prefsList.add(new Object[]{R.xml.misc_prefs, R.string.misc_header, new MiscFragment()});
		prefsList.add(new Object[]{R.xml.nav_prefs, R.string.nav_header, new NavFragment()});
		prefsList.add(new Object[]{R.xml.own_prefs_header, R.string.own_prefs_header, new OwnPrefsFragment()});
		prefsList.add(new Object[]{R.xml.packagemanger_prefs, R.string.pm_header, new PackageManagerFragment()});
		prefsList.add(new Object[]{R.xml.qs_tile_qty_prefs, R.string.qs_tile_qty_title, new QSTileQtyFragment()});
		prefsList.add(new Object[]{R.xml.quicksettings_prefs, R.string.qs_panel_category_title, new QuickSettingsFragment()});
		prefsList.add(new Object[]{R.xml.sbqs_network_prefs, R.string.ntsb_category_title, new NetworkFragment()});
		prefsList.add(new Object[]{R.xml.statusbar_batterybar_prefs, R.string.sbbb_header, new SBBBFragment()});
		prefsList.add(new Object[]{R.xml.statusbar_batteryicon_prefs, R.string.sbbIcon_header, new SBBIconFragment()});
		prefsList.add(new Object[]{R.xml.statusbar_clock_prefs, R.string.sbc_header, new SBCFragment()});
		prefsList.add(new Object[]{R.xml.statusbar_settings, R.string.statusbar_header, new StatusbarFragment()});
		prefsList.add(new Object[]{R.xml.theming_prefs, R.string.theme_customization_category, new ThemingFragment()});
		prefsList.add(new Object[]{R.xml.three_button_prefs, R.string.threebutton_header_title, new ThreeButtonNavFragment()});

		PreferenceHelper.init(getDefaultSharedPreferences(createDeviceProtectedStorageContext()));

		setContentView(R.layout.settings_activity);

		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			fragmentTransaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out, R.anim.fragment_fade_in, R.anim.fragment_fade_out);
			fragmentTransaction.replace(R.id.settings, new HeaderFragment()).commit();
		} else {
			setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
		}

		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
			if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
				setTitle(R.string.title_activity_settings);
				backButtonDisabled();
			}
		});

		Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.color_surface_overlay));
	}

	@Override
	public void onSearchResultClicked(@NonNull final SearchPreferenceResult result) {
		headerFragment = new HeaderFragment();
		new Handler(getMainLooper()).post(() -> headerFragment.onSearchResultClicked(result));
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		SharedPreferences prefs = getDefaultSharedPreferences(newBase.createDeviceProtectedStorageContext());

		String localeCode = prefs.getString("appLanguage", "");

		if (!localeCode.isEmpty()) {
			Locale locale = Locale.forLanguageTag(localeCode);

			Resources res = newBase.getResources();
			Configuration configuration = res.getConfiguration();

			configuration.setLocale(locale);

			LocaleList localeList = new LocaleList(locale);
			LocaleList.setDefault(localeList);
			configuration.setLocales(localeList);

			newBase = newBase.createConfigurationContext(configuration);
		}

		super.attachBaseContext(newBase);
	}

	private void createNotificationChannel() {
		CharSequence name = getString(R.string.update_channel_name);
		int importance = NotificationManager.IMPORTANCE_DEFAULT;

		NotificationChannel channel = new NotificationChannel(UPDATES_CHANNEL_ID, name, importance);
		NotificationManager notificationManager = getSystemService(NotificationManager.class);
		notificationManager.createNotificationChannel(channel);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getTitle() == getString(R.string.title_activity_settings)) {
			backButtonDisabled();
		} else {
			backButtonEnabled();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		// Save current activity title so we can set it again after a configuration change
		outState.putCharSequence(TITLE_TAG, getTitle());
	}

	@Override
	public boolean onSupportNavigateUp() {
		if (getSupportFragmentManager().popBackStackImmediate()) {
			return true;
		}
		return super.onSupportNavigateUp();
	}

	@Override
	public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
		// Instantiate the new Fragment
		final Bundle args = pref.getExtras();
		final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), Objects.requireNonNull(pref.getFragment()));
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);
		// Replace the existing Fragment with the new Fragment
		replaceFragment(fragment);
		setTitle(pref.getTitle());
		backButtonEnabled();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences prefs = getDefaultSharedPreferences(getApplicationContext().createDeviceProtectedStorageContext());

		int itemID = item.getItemId();

		if (itemID == android.R.id.home) {
			onBackPressed();
		} else if (itemID == R.id.menu_clearPrefs) {
			PrefManager.clearPrefs(prefs);
			AppUtils.Restart("systemui");
		} else if (itemID == R.id.menu_exportPrefs) {
			importExportSettings(true);
		} else if (itemID == R.id.menu_importPrefs) {
			importExportSettings(false);
		} else if (itemID == R.id.hooked_packages) {
			Intent intent = new Intent(this, HookedPackagesActivity.class);
			startActivity(intent);
		} else if (itemID == R.id.menu_restart) {
			AppUtils.Restart("system");
		} else if (itemID == R.id.menu_restartSysUI) {
			AppUtils.Restart("systemui");
		} else if (itemID == R.id.menu_soft_restart) {
			AppUtils.Restart("zygote");
		}
		return true;
	}

	private void importExportSettings(boolean export) {
		Intent fileIntent = new Intent();
		fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
		fileIntent.setType("*/*");
		fileIntent.putExtra(Intent.EXTRA_TITLE, "AOSPMods_Config" + ".bin");
		startActivityForResult(fileIntent, export ? REQUEST_EXPORT : REQUEST_IMPORT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data == null) return; //user hit cancel. Nothing to do

		SharedPreferences prefs = getDefaultSharedPreferences(getApplicationContext().createDeviceProtectedStorageContext());
		switch (requestCode) {
			case REQUEST_IMPORT:
				try {
					PrefManager.importPath(prefs, getContentResolver().openInputStream(data.getData()));
					AppUtils.Restart("systemui");
				} catch (Exception ignored) {
				}
				break;
			case REQUEST_EXPORT:
				try {
					PrefManager.exportPrefs(prefs, getContentResolver().openOutputStream(data.getData()));
				} catch (Exception ignored) {
				}
				break;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class HeaderFragment extends ControlledPreferenceFragmentCompat {
		SearchPreference searchPreference;

		@Override
		public String getTitle() {
			return getString(R.string.app_name);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.header_preferences;
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			super.onCreatePreferences(savedInstanceState, rootKey);
			try {
				Process process = Runtime.getRuntime().exec("getprop ro.build.id");
				process.waitFor();
				byte[] buffer = new byte[process.getInputStream().available()];
				//noinspection ResultOfMethodCallIgnored
				process.getInputStream().read(buffer);
				String result = new String(buffer, StandardCharsets.US_ASCII).replace("\n", "");
				if (!Pattern.matches("^[T|U][A-Z]([A-Z0-9]){2}\\.[0-9]{6}\\.[0-9]{3}(\\.[A-Z0-9]{2})?$", result)) //Pixel standard build number of A13
				{
					new MaterialAlertDialogBuilder(getContext(), R.style.MaterialComponents_MaterialAlertDialog)
							.setTitle(R.string.incompatible_alert_title)
							.setMessage(R.string.incompatible_alert_body)
							.setPositiveButton(R.string.incompatible_alert_ok_btn, (dialog, which) -> dialog.dismiss())
							.show();
				}
			} catch (Throwable ignored) {
			}

			searchPreference = findPreference("searchPreference");
			SearchConfiguration config = searchPreference.getSearchConfiguration();
			config.setActivity((AppCompatActivity) getActivity());
			config.setFragmentContainerViewId(R.id.settings);

			for (Object[] obj : prefsList) {
				config.index((Integer) obj[0]).addBreadcrumb(this.getResources().getString((Integer) obj[1]));
			}

			config.setBreadcrumbsEnabled(true);
			config.setHistoryEnabled(true);
			config.setFuzzySearchEnabled(false);
		}

		private void onSearchResultClicked(SearchPreferenceResult result) {
			if (result.getResourceFile() == R.xml.header_preferences) {
				searchPreference.setVisible(false);
				SearchPreferenceResult.highlight(new HeaderFragment(), result.getKey());
			} else {
				for (Object[] obj : prefsList) {
					if ((Integer) obj[0] == result.getResourceFile()) {
						replaceFragment((PreferenceFragmentCompat) obj[2]);
						SearchPreferenceResult.highlight((PreferenceFragmentCompat) obj[2], result.getKey());
						break;
					}
				}
			}
		}
	}

	private static void replaceFragment(Fragment fragment) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out, R.anim.fragment_fade_in, R.anim.fragment_fade_out);
		fragmentTransaction.replace(R.id.settings, fragment).addToBackStack(null).commit();
	}

	@SuppressWarnings("ConstantConditions")
	public static class NavFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.nav_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.nav_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class ThemingFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.theme_customization_category);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.theming_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class LockScreenFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.lockscreen_header_title);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.lock_screen_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class SBBBFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.sbbb_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.statusbar_batterybar_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class NetworkFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.ntsb_category_title);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.sbqs_network_prefs;
		}
	}


	@SuppressWarnings("ConstantConditions")
	public static class SBBIconFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.sbbIcon_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.statusbar_batteryicon_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class MiscFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.misc_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.misc_prefs;
		}
	}

	public static class PackageManagerFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.pm_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.packagemanger_prefs;
		}
	}

	public static class HotSpotFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.hotspot_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.hotspot_prefs;
		}
	}


	@SuppressWarnings("ConstantConditions")
	public static class SBCFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.sbc_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.statusbar_clock_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class ThreeButtonNavFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.threebutton_header_title);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.three_button_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class StatusbarFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.statusbar_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.statusbar_settings;
		}
	}

	public static class QSTileQtyFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.qs_tile_qty_title);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.qs_tile_qty_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class QuickSettingsFragment extends ControlledPreferenceFragmentCompat {
		private FrameLayout pullDownIndicator;

		@Override
		public String getTitle() {
			return getString(R.string.qs_panel_category_title);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.quicksettings_prefs;
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			super.onCreatePreferences(savedInstanceState, rootKey);

			createPullDownIndicator();
		}

		@Override
		public void onDestroy() {
			((ViewGroup) pullDownIndicator.getParent()).removeView(pullDownIndicator);
			super.onDestroy();
		}

		@SuppressLint("RtlHardcoded")
		@Override
		public void updateScreen(String key) {
			super.updateScreen(key);
			try {
				int displayWidth = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();

				pullDownIndicator.setVisibility(PreferenceHelper.isVisible("QSPulldownPercent") ? View.VISIBLE : View.GONE);
				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pullDownIndicator.getLayoutParams();
				lp.width = Math.round(mPreferences.getInt("QSPulldownPercent", 25) * displayWidth / 100f);
				lp.gravity = Gravity.TOP | (Integer.parseInt(mPreferences.getString("QSPulldownSide", "1")) == 1 ? Gravity.RIGHT : Gravity.LEFT);
				pullDownIndicator.setLayoutParams(lp);
			} catch (Exception ignored) {
			}
		}

		private void createPullDownIndicator() {
			pullDownIndicator = new FrameLayout(getContext());
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, 25);
			lp.gravity = Gravity.TOP;

			pullDownIndicator.setLayoutParams(lp);
			pullDownIndicator.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_200));
			pullDownIndicator.setAlpha(.7f);
			pullDownIndicator.setVisibility(View.VISIBLE);

			((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(pullDownIndicator);
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class GestureNavFragment extends ControlledPreferenceFragmentCompat {

		FrameLayout leftBackGestureIndicator, rightBackGestureIndicator;
		FrameLayout leftSwipeGestureIndicator, rightSwipeGestureIndicator;

		@Override
		public String getTitle() {
			return getString(R.string.gesturenav_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.gesture_nav_prefs;
		}

		@SuppressLint("RtlHardcoded")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			super.onCreatePreferences(savedInstanceState, rootKey);

			rightBackGestureIndicator = prepareBackGestureView(Gravity.RIGHT);
			leftBackGestureIndicator = prepareBackGestureView(Gravity.LEFT);

			rightSwipeGestureIndicator = prepareSwipeGestureView(Gravity.RIGHT);
			leftSwipeGestureIndicator = prepareSwipeGestureView(Gravity.LEFT);
		}

		@Override
		public void updateScreen(String key) {
			super.updateScreen(key);
			try {
				int displayHeight = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().height();
				int displayWidth = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();

				float leftSwipeUpPercentage = 25f;
				try {
					leftSwipeUpPercentage = RangeSliderPreference.getValues(mPreferences, "leftSwipeUpPercentage", 25).get(0);
				} catch (Exception ignored) {
				}

				float rightSwipeUpPercentage = 25f;
				try {
					rightSwipeUpPercentage = RangeSliderPreference.getValues(mPreferences, "rightSwipeUpPercentage", 25).get(0);
				} catch (Exception ignored) {
				}

				int edgeWidth = Math.round(displayWidth * leftSwipeUpPercentage / 100f);
				ViewGroup.LayoutParams lp = leftSwipeGestureIndicator.getLayoutParams();
				lp.width = edgeWidth;
				leftSwipeGestureIndicator.setLayoutParams(lp);

				edgeWidth = Math.round(displayWidth * rightSwipeUpPercentage / 100f);
				lp = rightSwipeGestureIndicator.getLayoutParams();
				lp.width = edgeWidth;
				rightSwipeGestureIndicator.setLayoutParams(lp);

				setVisibility(rightSwipeGestureIndicator, PreferenceHelper.isVisible("rightSwipeUpPercentage"), 400);
				setVisibility(leftSwipeGestureIndicator, PreferenceHelper.isVisible("leftSwipeUpPercentage"), 400);

				setVisibility(rightBackGestureIndicator, PreferenceHelper.isVisible("BackRightHeight"), 400);
				setVisibility(leftBackGestureIndicator, PreferenceHelper.isVisible("BackLeftHeight"), 400);

				int edgeHeight = Math.round(displayHeight * mPreferences.getInt("BackRightHeight", 100) / 100f);
				lp = rightBackGestureIndicator.getLayoutParams();
				lp.height = edgeHeight;
				rightBackGestureIndicator.setLayoutParams(lp);

				edgeHeight = Math.round(displayHeight * mPreferences.getInt("BackLeftHeight", 100) / 100f);
				lp = leftBackGestureIndicator.getLayoutParams();
				lp.height = edgeHeight;
				leftBackGestureIndicator.setLayoutParams(lp);

			} catch (Exception ignored) {
			}
		}

		private FrameLayout prepareSwipeGestureView(int gravity) {
			int navigationBarHeight = 0;
			@SuppressLint({"DiscouragedApi", "InternalInsetResource"})
			int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
			if (resourceId > 0) {
				navigationBarHeight = getContext().getResources().getDimensionPixelSize(resourceId);
			}

			FrameLayout result = new FrameLayout(getContext());
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, navigationBarHeight);
			lp.gravity = gravity | Gravity.BOTTOM;
			lp.bottomMargin = 0;
			result.setLayoutParams(lp);

			result.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_300));
			result.setAlpha(.7f);
			((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(result);
			result.setVisibility(View.GONE);
			return result;
		}

		private FrameLayout prepareBackGestureView(int gravity) {
			int navigationBarHeight = 0;
			@SuppressLint({"InternalInsetResource", "DiscouragedApi"})
			int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
			if (resourceId > 0) {
				navigationBarHeight = getContext().getResources().getDimensionPixelSize(resourceId);
			}

			FrameLayout result = new FrameLayout(getContext());
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(50, 0);
			lp.gravity = gravity | Gravity.BOTTOM;
			lp.bottomMargin = navigationBarHeight;
			result.setLayoutParams(lp);

			result.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_300));
			result.setAlpha(.7f);
			((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(result);
			result.setVisibility(View.GONE);
			return result;
		}

		@SuppressWarnings("SameParameterValue")
		private void setVisibility(View v, boolean visible, long duration) {
			if ((v.getVisibility() == View.VISIBLE) == visible) return;

			float basicAlpha = v.getAlpha();
			float destAlpha = (visible) ? 1f : 0f;

			if (visible) v.setAlpha(0f);
			v.setVisibility(View.VISIBLE);

			v.animate().setDuration(duration).alpha(destAlpha).setListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animator) {
				}

				@Override
				public void onAnimationEnd(Animator animator) {
					if (!visible) v.setVisibility(View.GONE);
					v.setAlpha(basicAlpha);
				}

				@Override
				public void onAnimationCancel(Animator animator) {
				}

				@Override
				public void onAnimationRepeat(Animator animator) {
				}
			}).start();
		}

		@Override
		public void onDestroy() {
			((ViewGroup) rightBackGestureIndicator.getParent()).removeView(rightBackGestureIndicator);
			((ViewGroup) leftBackGestureIndicator.getParent()).removeView(leftBackGestureIndicator);

			((ViewGroup) rightSwipeGestureIndicator.getParent()).removeView(rightSwipeGestureIndicator);
			((ViewGroup) leftSwipeGestureIndicator.getParent()).removeView(leftSwipeGestureIndicator);

			super.onDestroy();
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class NetworkStatFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.netstat_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.lsqs_custom_text;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class DialerFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.dialer_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.dialer_prefs;
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class OwnPrefsFragment extends ControlledPreferenceFragmentCompat {
		@Override
		public String getTitle() {
			return getString(R.string.own_prefs_header);
		}

		@Override
		public int getLayoutResource() {
			return R.xml.own_prefs_header;
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			super.onCreatePreferences(savedInstanceState, rootKey);

			findPreference("CheckForUpdate")
					.setOnPreferenceClickListener(preference -> {
						startActivity(new Intent(getActivity(), UpdateActivity.class));
						return true;
					});

			findPreference("GitHubRepo")
					.setOnPreferenceClickListener(preference -> {
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse("https://github.com/siavash79/AOSPMods"));
							startActivity(intent);
						} catch (Exception ignored) {
							Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
						}
						return true;
					});

			findPreference("TelegramGroup")
					.setOnPreferenceClickListener(preference -> {
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse("https://t.me/AOSPMods_Support"));
							startActivity(intent);
						} catch (Exception ignored) {
							Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
						}
						return true;
					});

			findPreference("UsageWiki")
					.setOnPreferenceClickListener(preference -> {
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse("https://github.com/siavash79/AOSPMods/wiki/AOSPMods-Wiki"));
							startActivity(intent);
						} catch (Exception ignored) {
							Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
						}
						return true;
					});
		}

		@Override
		public void updateScreen(String key) {
			super.updateScreen(key);

			if (key == null) return;

			switch (key) {
				case "appLanguage":
					try {
						getActivity().recreate();
					} catch (Exception ignored) {
					}
					break;

				case "AlternativeThemedAppIcon":
					try {
						boolean AlternativeThemedAppIconEnabled = mPreferences.getBoolean("AlternativeThemedAppIcon", false);

						new MaterialAlertDialogBuilder(getContext(), R.style.MaterialComponents_MaterialAlertDialog)
								.setTitle(R.string.app_kill_alert_title)
								.setMessage(R.string.app_kill_alert_body)
								.setPositiveButton(R.string.app_kill_ok_btn, (dialog, which) -> setAlternativeAppIcon(AlternativeThemedAppIconEnabled))
								.setCancelable(false)
								.show();
					} catch (Exception ignored) {
					}
					break;

				case "AutoUpdate":
					UpdateScheduler.scheduleUpdates(getContext());
					break;
			}
		}

		private void setAlternativeAppIcon(boolean alternativeThemedAppIconEnabled) {
			PackageManager packageManager = getActivity().getPackageManager();

			packageManager.setComponentEnabledSetting(
					new ComponentName(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + ".SplashScreenActivityNormalIcon"),
					alternativeThemedAppIconEnabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
					PackageManager.DONT_KILL_APP
			);

			// Enable themed app icon component
			packageManager.setComponentEnabledSetting(
					new ComponentName(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + ".SplashScreenActivityAlternateIcon"),
					alternativeThemedAppIconEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP
			);

			getActivity().finish();
		}
	}
}