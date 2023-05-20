package sh.siava.AOSPMods;

import static sh.siava.AOSPMods.utils.AppUtils.installDoubleZip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import java.util.Locale;
import java.util.Objects;

import sh.siava.AOSPMods.databinding.ActivityUpdateBinding;
import sh.siava.AOSPMods.modpacks.utils.ModuleFolderOperations;

public class UpdateActivity extends AppCompatActivity {

	private AppBarConfiguration appBarConfiguration;
	@SuppressWarnings("FieldCanBeLocal")
	private ActivityUpdateBinding binding;

	public static final String MOD_NAME = "AOSPMods";
	public static final String MAGISK_UPDATE_DIR = "/data/adb/modules_update";
	public static final String MAGISK_MODULES_DIR = "/data/adb/modules";
	private static final String updateRoot = String.format("%s/%s", MAGISK_UPDATE_DIR, MOD_NAME);

	@Override
	protected void attachBaseContext(Context newBase) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase.createDeviceProtectedStorageContext());

		String localeCode = prefs.getString("appLanguage", "");
		Locale locale = (localeCode.length() > 0) ? Locale.forLanguageTag(localeCode) : Locale.getDefault();

		Resources res = newBase.getResources();
		Configuration configuration = res.getConfiguration();

		configuration.setLocale(locale);

		LocaleList localeList = new LocaleList(locale);
		LocaleList.setDefault(localeList);
		configuration.setLocales(localeList);

		super.attachBaseContext(newBase.createConfigurationContext(configuration));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getBooleanExtra("updateTapped", false)) {

			String downloadPath = getIntent().getStringExtra("filePath");

			installDoubleZip(downloadPath);
			applyPrefsToUpdate();
		}
		binding = ActivityUpdateBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.color_surface_overlay));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_update);
		appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
	}

	private void applyPrefsToUpdate() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(createDeviceProtectedStorageContext());

		int volumeStps = prefs.getInt("volumeStps", 0);
		boolean customFontsEnabled = prefs.getBoolean("enableCustomFonts", false);
		boolean GSansOverrideEnabled = prefs.getBoolean("gsans_override", false);
		boolean PowerMenuOverlayEnabled = prefs.getBoolean("enablePowerMenuTheme", false);

		ModuleFolderOperations.applyVolumeSteps(volumeStps, updateRoot);
		ModuleFolderOperations.applyFontSettings(customFontsEnabled, GSansOverrideEnabled, updateRoot);
		ModuleFolderOperations.applyPowerMenuOverlay(PowerMenuOverlayEnabled, updateRoot);
	}

	@Override
	public void onNewIntent(Intent i) {
		super.onNewIntent(i);
	}

	@Override
	public boolean onSupportNavigateUp() {
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_update);
		onBackPressed();
		return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
	}
}