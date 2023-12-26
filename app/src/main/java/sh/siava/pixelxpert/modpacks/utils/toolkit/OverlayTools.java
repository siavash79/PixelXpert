package sh.siava.pixelxpert.modpacks.utils.toolkit;

import static de.robv.android.xposed.XposedBridge.log;

import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import sh.siava.pixelxpert.IRootProviderProxy;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.utils.Overlays;

public class OverlayTools {
	public static List<String> activeOverlays = null;

	public static void getActiveOverlays(IRootProviderProxy proxy) throws RemoteException {
		List<String> result = new ArrayList<>();
		String[] lines = proxy.runCommand("cmd overlay list --user 0");
		for (String thisLine : lines) {
			if (thisLine.startsWith("[x]")) {
				result.add(thisLine.replace("[x] ", ""));
			}
		}
		activeOverlays = result;
	}

	public static void setOverlay(String Key, boolean enabled, boolean refresh, boolean force) {
		XPLauncher.enqueueProxyCommand(proxy -> {
			if (refresh) {
				try {
					getActiveOverlays(proxy);
				} catch (Throwable ignored) {}
			}
			setOverlay(Key, enabled, force);
		});
	}

	private static void setOverlay(String Key, boolean enabled, boolean force, IRootProviderProxy proxy) throws RemoteException {
		if (XPLauncher.isChildProcess) return;

		if (activeOverlays == null) getActiveOverlays(proxy); //make sure we have a list in hand

		String mode = (enabled) ? "enable" : "disable";
		String packageName;
//        boolean exclusive = false;

		if (Key.endsWith("Overlay")) {
			Overlays.OverlayProp op = (Overlays.OverlayProp) Overlays.Overlays.get(Key);
			//noinspection ConstantConditions
			packageName = op.name;
//            exclusive = op.exclusive;
		} else if (Key.endsWith("OverlayG")) //It's a group of overlays to work together as a team
		{
			setOverlayGroup(Key, enabled, force, proxy);
			return;
		} else {
			packageName = Key;
//            exclusive = true;
		}

/*        if (enabled && exclusive) {
            mode += "-exclusive"; //since we are checking all overlays, we don't need exclusive anymore.
        }*/

		boolean wasEnabled = (activeOverlays.contains(packageName));

		if (enabled == wasEnabled && !force) {
			return; //nothing to do. We're already set
		}

		try {
			proxy.runCommand("cmd overlay " + mode + " --user 0 " + packageName);
		} catch (Throwable t) {
			log(t);
		}
	}

	public static void setOverlay(String Key, boolean enabled, boolean force) {
		XPLauncher.enqueueProxyCommand(proxy -> setOverlay(Key, enabled, force, proxy));
	}

	private static void setOverlayGroup(String key, boolean enabled, boolean force, IRootProviderProxy proxy) {
		Overlays.OverlayGroup thisGroup = (Overlays.OverlayGroup) Overlays.Overlays.get(key);

		//noinspection ConstantConditions
		for (Overlays.OverlayProp thisProp : thisGroup.members) {
			try {
				setOverlay(thisProp.name, enabled, force, proxy);
			} catch (Throwable ignored) {}
		}
	}

}
