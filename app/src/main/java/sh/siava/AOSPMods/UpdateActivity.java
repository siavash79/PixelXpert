package sh.siava.AOSPMods;

import static sh.siava.AOSPMods.Utils.Helpers.installDoubleZip;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.databinding.ActivityUpdateBinding;

public class UpdateActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityUpdateBinding binding;

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