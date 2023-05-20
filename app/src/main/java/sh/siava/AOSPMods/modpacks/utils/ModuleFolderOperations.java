package sh.siava.AOSPMods.modpacks.utils;

import com.topjohnwu.superuser.Shell;

public class ModuleFolderOperations {
	public static void applyFontSettings(boolean customFontsEnabled, boolean GSansOverrideEnabled, String rootPath) {
		new Thread(() -> {
			try {
				if (customFontsEnabled && GSansOverrideEnabled) {
					Shell.cmd(String.format("cp %s/data/etcz/fonts.xml %s/system/etc/ && cp %s/data/fontz/GSans/*.ttf %s/system/fonts/ && cp %s/data/productz/etc/fonts_customization.xml.NEW %s/system/product/etc/fonts_customization.xml", rootPath, rootPath, rootPath, rootPath, rootPath, rootPath)).exec();
				} else if (customFontsEnabled) {
					Shell.cmd(String.format("cp %s/data/etcz/fonts.xml %s/system/etc/ && rm -rf %s/system/fonts/*.ttf && cp %s/data/productz/etc/fonts_customization.xml.OLD %s/system/product/etc/fonts_customization.xml && cp -r %s/data/productz/fonts/* %s/system/product/fonts/", rootPath, rootPath, rootPath, rootPath, rootPath, rootPath, rootPath)).exec();
				} else {
					Shell.cmd(String.format("rm -rf %s/system/fonts/*.ttf && rm -f %s/system/product/etc/fonts_customization.xml && rm -rf %s/system/product/fonts/* && rm -rf %s/system/etc/fonts.xml", rootPath, rootPath, rootPath, rootPath)).exec();
				}
			} catch (Exception ignored) {
			}
		}).start();
	}

	public static void applyVolumeSteps(int volumeStps, String rootPath) {
		if (volumeStps <= 10) {
			Shell.cmd("rm -Rf " + rootPath + "/system.prop").exec();
			return;
		}
		Shell.cmd("echo ro.config.media_vol_steps=" + volumeStps + " > " + rootPath + "/system.prop").exec();
	}

	public static void applyPowerMenuOverlay(boolean PowerMenuOverlayEnabled, String rootPath) {
		new Thread(() -> {
			try {
				if (PowerMenuOverlayEnabled ) {
					Shell.cmd(String.format("cp %s/data/productz/overlay/AOSPMods_System/AOSPModsSystem.apk %s/system/product/overlay/AOSPMods_System/AOSPModsSystem.apk && cp %s/data/productz/overlay/AOSPMods_SystemUI/AOSPModsSystemUI.apk %s/system/product/overlay/AOSPMods_SystemUI/AOSPModsSystemUI.apk", rootPath, rootPath, rootPath, rootPath)).exec();
				} else {
					Shell.cmd(String.format("rm -rf %s/system/product/overlay/AOSPMods_System/AOSPModsSystem.apk && rm -rf %s/system/product/overlay/AOSPMods_SystemUI/AOSPModsSystemUI.apk", rootPath, rootPath)).exec();
				}
			} catch (Exception ignored) {
			}
		}).start();
	}
}
