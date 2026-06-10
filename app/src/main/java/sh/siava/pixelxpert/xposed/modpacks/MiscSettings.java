package sh.siava.pixelxpert.xposed.modpacks;

import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Color;
import android.os.RemoteException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XPLauncher;
import sh.siava.pixelxpert.xposed.XPrefs;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.ModuleFolderOperations;
import sh.siava.pixelxpert.xposed.utils.StringFormatter;

@SystemUIModPack
public class MiscSettings extends XposedModPack {

	public MiscSettings(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return; //it won't be null. but anyway...

		//netstat settings
		boolean netstatColorful = Xprefs.getBoolean("networkStatsColorful", false);

		int NetStatsStartMonthStart = Xprefs.getSliderInt( "NetworkStatsMonthStart", 1);

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
				case "volumeStps":
					setVolumeSteps();
					break;
				case "force_volte":
					force_volte();
					break;
				case "force_ss_over_ut":
					force_ss_over_ut();
					break;
				case "force_cross_sim":
					force_cross_sim();
					break;
				case "force_wfc":
					force_wfc();
					break;
				case "force_hotspot":
					force_hotspot();
					break;
			}
		} else {
			//startup jobs
			setDisplayOverride();

			updateSysUITuner();

			setVolumeSteps();

			force_volte();

			force_ss_over_ut();

			force_cross_sim();

			force_wfc();

			force_hotspot();
		}
	}

	private void force_hotspot()
	{
		if(Xprefs.getBoolean("force_hotspot", false))
		{
			XPLauncher.enqueueProxyCommand(proxy -> proxy.runRootCommand("cmd wifi force-country-code enabled US"));
		}
	}

	private void force_volte() {
		if(Xprefs.getBoolean("force_volte", false))
		{
			XPLauncher.enqueueProxyCommand(proxy -> {
				proxy.runRootCommand("setprop persist.dbg.volte_avail_ovr 1");
				proxy.runRootCommand("setprop persist.dbg.vonr_avail_ovr 1");
			});
		}
	}

	private void force_ss_over_ut() {
		if(Xprefs.getBoolean("force_ss_over_ut", false))
		{
			XPLauncher.enqueueProxyCommand(proxy -> proxy.runRootCommand("cmd phone cc set-value carrier_supports_ss_over_ut_bool 1"));
		}
	}

	private void force_cross_sim() {
		if(Xprefs.getBoolean("force_cross_sim", false))
		{
			XPLauncher.enqueueProxyCommand(proxy -> proxy.runRootCommand("cmd phone cc set-value carrier_cross_sim_ims_available_bool 1; cmd phone cc set-value enable_cross_sim_calling_on_opportunistic_data_bool 1"));
		}
	}

	private void force_wfc() {
		if(Xprefs.getBoolean("force_wfc", false))
		{
			XPLauncher.enqueueProxyCommand(proxy -> proxy.runRootCommand("setprop persist.dbg.wfc_avail_ovr 1; cmd phone cc set-value carrier_wfc_ims_available_bool 1; cmd phone cc set-value carrier_wfc_supports_wifi_only_bool 1; cmd phone cc set-value editable_wfc_mode_bool 1; cmd phone cc set-value editable_wfc_roaming_mode_bool 1"));
		}
	}

	private void setDisplayOverride() {
		if(!Xprefs.getBoolean("displayOverrideEnabled", false)) return;

		float displayOverride = Xprefs.getSliderFloat( "displayOverride", 100f) / 100f;
		XPLauncher.enqueueProxyCommand(proxy -> {
			try {
				String sizeResult = proxy.runRootCommand("wm size")[0];

				String[] physicalSizes = sizeResult.replace("Physical size: ", "").split("x");
				int w = Integer.parseInt(physicalSizes[0]);
				int h = Integer.parseInt(physicalSizes[1]);

				int overrideW = Math.round(w * displayOverride);
				int overrideH = Math.round(h * displayOverride);

				proxy.runRootCommand(String.format("wm size %sx%s", overrideW, overrideH));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void setVolumeSteps() {
		int volumeStps = Xprefs.getSliderInt("volumeStps", 0);

		ModuleFolderOperations.applyVolumeSteps(volumeStps, XPrefs.MagiskRoot, false);
	}

	private void updateSysUITuner() {
		XPLauncher.enqueueProxyCommand(proxy -> {
			try {
				boolean SysUITunerEnabled = Xprefs.getBoolean("sysui_tuner", false);
				String mode = (SysUITunerEnabled) ? "enable" : "disable";

				proxy.runRootCommand("pm " + mode + " com.android.systemui/.tuner.TunerActivity");
			} catch (Exception ignored) {}
		});
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
	}
}
