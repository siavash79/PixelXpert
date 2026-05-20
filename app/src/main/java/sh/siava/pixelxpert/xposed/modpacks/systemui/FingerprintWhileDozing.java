package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;


import android.content.Context;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class FingerprintWhileDozing extends XposedModPack {
	private static boolean fingerprintWhileDozing = true;

	public FingerprintWhileDozing(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		fingerprintWhileDozing = Xprefs.getBoolean("fingerprintWhileDozing", true);
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		if (android.os.Build.VERSION.SDK_INT >= 37) return;
		ReflectedClass KeyguardUpdateMonitorClass = ReflectedClass.of("com.android.keyguard.KeyguardUpdateMonitor");

		KeyguardUpdateMonitorClass
				.before("shouldListenForFingerprint")
				.run(param -> {
					if (fingerprintWhileDozing) return;

					if(!getBooleanField(param.thisObject, "mDeviceInteractive"))
					{
						param.setResult(false);
					}
				});
	}
}