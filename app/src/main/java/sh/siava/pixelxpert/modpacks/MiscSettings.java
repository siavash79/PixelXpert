package sh.siava.pixelxpert.modpacks;

import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ObjectTools.addItemToCommaStringIfNotPresent;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ObjectTools.removeItemFromCommaString;

import android.content.Context;
import android.graphics.Color;
import android.os.RemoteException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.IRootProviderProxy;
import sh.siava.pixelxpert.modpacks.utils.ModuleFolderOperations;
import sh.siava.pixelxpert.modpacks.utils.StringFormatter;
import sh.siava.rangesliderpreference.RangeSliderPreference;

public class MiscSettings extends XposedModPack {

	public MiscSettings(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return; //it won't be null. but anyway...

		//netstat settings
		boolean netstatColorful = Xprefs.getBoolean("networkStatsColorful", false);

		int NetStatsStartMonthStart = 1;
		try {
			NetStatsStartMonthStart = Math.round(RangeSliderPreference.getValues(Xprefs, "NetworkStatsMonthStart", 1).get(0));
		} catch (Throwable ignored){}

		StringFormatter.RXColor = (netstatColorful) ? Xprefs.getInt("networkStatDLColor", Color.GREEN) : null;
		StringFormatter.TXColor = (netstatColorful) ? Xprefs.getInt("networkStatULColor", Color.RED) : null;
		StringFormatter.NetStatStartBase = Integer.parseInt(Xprefs.getString("NetworkStatsStartBase", "0"));
		StringFormatter.NetStatsStartTime = LocalTime.parse(Xprefs.getString("NetworkStatsStartTime", "0:0"), DateTimeFormatter.ofPattern("H:m"));

		StringFormatter.NetStatsDayOf = StringFormatter.NetStatStartBase == StringFormatter.NET_STAT_TYPE_MONTH
		? NetStatsStartMonthStart
		: Integer.parseInt(Xprefs.getString("NetworkStatsWeekStart", "1"));

		StringFormatter.refreshAll();

		if (Key.length > 0) {
			//we're not at startup
			//we know what has changed
			switch (Key[0]) {
				case "sysui_tuner":
					updateSysUITuner();
					break;
				case "wifi_cell":
					updateWifiCell();
					break;
				case "enableCustomFonts":
				case "gsans_override":
		//			updateFontsInfrastructure();
					break;
				case "volumeStps":
					setVolumeSteps();
					break;
			}
		} else {
			if (XPLauncher.isChildProcess) return;

			//startup jobs
			setDisplayOverride();

			updateSysUITuner();
	//		updateFontsInfrastructure();

			setVolumeSteps();
		}
	}

	private void setDisplayOverride() {
		if(!Xprefs.getBoolean("displayOverrideEnabled", false)) return;

		float displayOverride = 1f;
		try {
			displayOverride = RangeSliderPreference.getValues(Xprefs, "displayOverride", 100f).get(0) / 100f;
		} catch (Exception ignored) {}
		float finalDisplayOverride = displayOverride;
		XPLauncher.enqueueProxyCommand(new XPLauncher.ProxyRunnable(){
			@Override public void run(IRootProviderProxy proxy)
			{
				try {
					String sizeResult = proxy.runCommand("wm size")[0];

					String[] physicalSizes = sizeResult.replace("Physical size: ", "").split("x");
					int w = Integer.parseInt(physicalSizes[0]);
					int h = Integer.parseInt(physicalSizes[1]);

					int overrideW = Math.round(w * finalDisplayOverride);
					int overrideH = Math.round(h * finalDisplayOverride);

					proxy.runCommand(String.format("wm size %sx%s", overrideW, overrideH));
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private void setVolumeSteps() {
		int volumeStps = Xprefs.getInt("volumeStps", 0);

		ModuleFolderOperations.applyVolumeSteps(volumeStps, XPrefs.MagiskRoot, false);
	}

	private void updateWifiCell() {
		XPLauncher.enqueueProxyCommand(new XPLauncher.ProxyRunnable() {
			@Override
			public void run(IRootProviderProxy proxy) {
				boolean WifiCellEnabled = Xprefs.getBoolean("wifi_cell", false)
						&& Xprefs.getBoolean("InternetTileModEnabled", true);

				try {
					String currentTiles = proxy.runCommand("settings get secure sysui_qs_tiles")[0];

					boolean providerModel;

					if (WifiCellEnabled) {
						providerModel = false;
						currentTiles = addItemToCommaStringIfNotPresent(currentTiles, "cell_PixelXpert");
						currentTiles = addItemToCommaStringIfNotPresent(currentTiles, "wifi_PixelXpert");

						currentTiles = removeItemFromCommaString(currentTiles, "internet");
					} else {
						providerModel = true;

						currentTiles = removeItemFromCommaString(currentTiles, "cell_PixelXpert");
						currentTiles = removeItemFromCommaString(currentTiles, "wifi_PixelXpert");

						currentTiles = addItemToCommaStringIfNotPresent(currentTiles, "internet");
					}

					proxy.runCommand("settings put global settings_provider_model " + providerModel + "; settings put secure sysui_qs_tiles \"" + currentTiles + "\"");
				} catch (Exception ignored) {
				}
			}
		});
	}

	private void updateSysUITuner() {
		XPLauncher.enqueueProxyCommand(new XPLauncher.ProxyRunnable() {
			@Override
			public void run(IRootProviderProxy proxy) {
				try {
					boolean SysUITunerEnabled = Xprefs.getBoolean("sysui_tuner", false);
					String mode = (SysUITunerEnabled) ? "enable" : "disable";

					proxy.runCommand("pm " + mode + " com.android.systemui/.tuner.TunerActivity");
				} catch (Exception ignored) {
				}
			}
		});
	}

	@Deprecated
	private void updateFontsInfrastructure() {
		boolean customFontsEnabled = Xprefs.getBoolean("enableCustomFonts", false);
		boolean GSansOverrideEnabled = Xprefs.getBoolean("gsans_override", false);

		ModuleFolderOperations.applyFontSettings(customFontsEnabled, GSansOverrideEnabled, XPrefs.MagiskRoot, false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return packageName.equals(Constants.SYSTEM_UI_PACKAGE);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
	}
}
