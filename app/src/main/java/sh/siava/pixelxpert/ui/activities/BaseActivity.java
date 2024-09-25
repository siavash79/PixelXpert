package sh.siava.pixelxpert.ui.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.shape.MaterialShapeDrawable;

import sh.siava.pixelxpert.R;

public class BaseActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupEdgeToEdge();
	}

	private void setupEdgeToEdge() {
		try {
			((AppBarLayout) findViewById(R.id.appBarLayout)).setStatusBarForeground(MaterialShapeDrawable.createWithElevationOverlay(getApplicationContext()));
		} catch (Exception ignored) {
		}

		Window window = getWindow();
		WindowCompat.setDecorFitsSystemWindows(window, false);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			ViewGroup viewGroup = getWindow().getDecorView().findViewById(android.R.id.content);
			ViewCompat.setOnApplyWindowInsetsListener(viewGroup, (v, windowInsets) -> {
				Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

				ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
				v.setPadding(
						params.leftMargin + insets.left,
						0,
						params.rightMargin + insets.right,
						0
				);
				params.bottomMargin = 0;
				v.setLayoutParams(params);

				return windowInsets;
			});
		}
	}
}
