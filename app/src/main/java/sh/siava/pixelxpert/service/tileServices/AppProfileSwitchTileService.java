package sh.siava.pixelxpert.service.tileServices;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import sh.siava.pixelxpert.modpacks.Constants;

public class AppProfileSwitchTileService extends TileService{
	// Called when the user adds your tile.
	@Override
	public void onTileAdded() {
		super.onTileAdded();
	}

	// Called when your app can update your tile.
	@Override
	public void onStartListening() {
		super.onStartListening();
	}

	// Called when your app can no longer update your tile.
	@Override
	public void onStopListening() {
		super.onStopListening();
	}

	// Called when the user taps on your tile in an active or inactive state.
	@SuppressLint("StartActivityAndCollapseDeprecated")
	@Override
	public void onClick() {
		super.onClick();

		getQsTile().setState(Tile.STATE_UNAVAILABLE);
		getQsTile().updateTile();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			startActivityAndCollapse(PendingIntent.getActivity(
					this,
					0,
					new Intent(),
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
		} else
		{
			startActivityAndCollapse(new Intent());
		}

		sendBroadcast();
	}

	private void sendBroadcast() {
		new Thread(() -> {
			sendBroadcast(Constants.getAppProfileSwitchIntent());

			getQsTile().setState(Tile.STATE_INACTIVE);
			getQsTile().updateTile();
		}).start();
	}

	// Called when the user removes your tile.
	@Override
	public void onTileRemoved() {
		super.onTileRemoved();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

}
