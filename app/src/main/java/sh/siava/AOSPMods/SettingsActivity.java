package sh.siava.AOSPMods;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.topjohnwu.superuser.Shell;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    public void RestartSysUI(View view) {
        Shell.su("killall com.android.systemui").submit();
//        runCommandAction("killall com.android.systemui");

    }

    public void backButtonEnabled(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void backButtonDisabled(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backButtonDisabled();

        try {
            getApplicationContext().getSharedPreferences("sh.siava.AOSPMods_preferences", Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            Log.d("SIAPOSED", "onCreate: failed to world read");
            // The new XSharedPreferences is not enabled or module's not loading
            // other fallback, if any
        }

        //update settings from previous config file
        try {
            if(PreferenceManager.getDefaultSharedPreferences(this).contains("Show4GIcon"))
            {
                boolean fourGEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("Show4GIcon", false);
                if(fourGEnabled)
                {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("LTE4GIconMod", 2).apply();
                }
                PreferenceManager.getDefaultSharedPreferences(this).edit().remove("Show4GIcon").commit();
            }
        }
        catch(Exception e){}


        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                            backButtonDisabled();
                        }
                    }
                });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (getTitle() == getString(R.string.title_activity_settings)){
        backButtonDisabled();
        } else {
            backButtonEnabled();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
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
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        backButtonEnabled();
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class NavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.nav_prefs, rootKey);
        }
    }

    public static class LockScreenFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.lock_screen_prefs, rootKey);
        }
    }

    public static class MiscFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.misc_prefs, rootKey);
        }
    }

    public static class SBCFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.statusbar_clock_prefs, rootKey);
        }
    }

    public static class ThreeButtonNavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.three_button_prefs, rootKey);
        }
    }

    public static class StatusbarFragment extends PreferenceFragmentCompat {

        Preference QSPulldownPercent;


        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("QSPulldownPercent")) {
                    updateQSPulldownPercent();
                } else if (key.equals("QSPullodwnEnabled")) {
                    updateQSPulldownEnabld();
                } else if (key.equals("QSFooterMod")) {
                    updateQSFooterMod();
                } else if (key.equals("BatteryStyle")) {
                    updateBatteryMod();
                } else if (key.equals("BatteryIconScaleFactor")) {
                    updateBatteryIconScaleFactor();
                }
            }

        };

        private void updateBatteryIconScaleFactor() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                findPreference("BatteryIconScaleFactor").setSummary(prefs.getInt("BatteryIconScaleFactor", 50)*2 + getString(R.string.battery_size_summary));
            }
            catch(Exception e){
                return;
            }

        }

        private void updateBatteryMod() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                boolean enabled = !(prefs.getString("BatteryStyle", "0").equals(("0")));
                findPreference("BatteryShowPercent").setEnabled(enabled);
            }catch(Exception e){}
        }

        private void updateQSFooterMod() {
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean enabled = prefs.getBoolean("QSFooterMod", false);

                findPreference("QSFooterText").setEnabled(enabled);
            } catch(Exception e){}
        }

        private void updateQSPulldownEnabld() {
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean enabled = prefs.getBoolean("QSPullodwnEnabled", false);

                findPreference("QSPulldownPercent").setEnabled(enabled);
                findPreference("QSPulldownSide").setEnabled(enabled);
            }catch(Exception e){}
        }

        private void updateQSPulldownPercent() {
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                int value = prefs.getInt("QSPulldownPercent", 25);
                QSPulldownPercent.setSummary(value + "%");
            }catch(Exception e){}
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            setPreferencesFromResource(R.xml.statusbar_settings, rootKey);

            QSPulldownPercent = findPreference("QSPulldownPercent");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            prefs.registerOnSharedPreferenceChangeListener(listener);

            updateQSPulldownPercent();
            updateQSPulldownEnabld();
            updateQSFooterMod();
            updateBatteryMod();
            updateBatteryIconScaleFactor();
        }
    }

    public static class GestureNavFragment extends PreferenceFragmentCompat {

        Context context;
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateBackGesture();
                updateNavPill();
            }
        };

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            this.context= context;
        }

        private void updateNavPill() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                findPreference("GesPillWidthModPos").setEnabled(prefs.getBoolean("GesPillWidthMod", true));
                findPreference("GesPillWidthModPos").setSummary(prefs.getInt("GesPillWidthModPos", 50)*2 + getString(R.string.pill_width_summary));
            } catch(Exception e){}
        }

        private void updateBackGesture() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                findPreference("BackLeftHeight").setEnabled(prefs.getBoolean("BackFromLeft", true));
                findPreference("BackRightHeight").setEnabled(prefs.getBoolean("BackFromRight", true));

                findPreference("BackLeftHeight").setSummary(prefs.getInt("BackLeftHeight", 100) + "%");
                findPreference("BackRightHeight").setSummary(prefs.getInt("BackRightHeight", 100) + "%");
            } catch(Exception e){}
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.gesture_nav_perfs, rootKey);

            PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(listener);

            updateBackGesture();
            updateNavPill();
        }

    }
}