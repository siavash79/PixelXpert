package sh.siava.AOSPMods;

import static sh.siava.AOSPMods.Utils.Helpers.installDoubleZip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.databinding.ActivityUpdateBinding;

public class UpdateActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityUpdateBinding binding;

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

        if(getIntent().getBooleanExtra("updateTapped", false)) {

            String downloadPath = getIntent().getStringExtra("filePath");

            installDoubleZip(downloadPath);
        }
        binding = ActivityUpdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_update);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public void onNewIntent(Intent i)
    {
        super.onNewIntent(i);


    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_update);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}