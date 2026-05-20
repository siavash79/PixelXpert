package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;


import static sh.siava.pixelxpert.xposed.utils.toolkit.ObjectTools.getStateFlowImplOf;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class UDFPSManager extends XposedModPack {
	private static final int TRANSPARENT = 0;
	private static final int OPAQUE = 255;
	private static boolean transparentBG = false;
	private static boolean transparentFG = false;
	private View mDeviceEntryIconView;
	private ReflectedClass ReadonlyStateFlowClass;

	public UDFPSManager(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;
		transparentBG = Xprefs.getBoolean("fingerprint_circle_hide", false);
		transparentFG = Xprefs.getBoolean("fingerprint_icon_hide", false);

		if(Key.length == 0) return;

		switch (Key[0])
		{
			case "fingerprint_circle_hide":
			case "fingerprint_icon_hide":
				setUDFPSGraphics(true);
		}
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) {
		ReflectedClass DeviceEntryIconViewClass = ReflectedClass.of("com.android.systemui.keyguard.ui.view.DeviceEntryIconView");
		ReflectedClass DeviceEntryIconViewModelClass = ReflectedClass.of("com.android.systemui.keyguard.ui.viewmodel.DeviceEntryIconViewModel");

		ReadonlyStateFlowClass = ReflectedClass.of("kotlinx.coroutines.flow.ReadonlyStateFlow");

		DeviceEntryIconViewModelClass
				.afterConstruction()
				.run(param -> {
					if((transparentBG && !transparentFG)) {
						try {
							setObjectField(param.thisObject, "useBackgroundProtection", ReadonlyStateFlowClass.getClazz().getConstructors()[0].newInstance(getStateFlowImplOf(false)));
						} catch (Throwable ignored) {}
					}
				});

		DeviceEntryIconViewClass
				.afterConstruction()
				.run(param -> {
					mDeviceEntryIconView = (View) param.thisObject;

					setUDFPSGraphics(false);


					//making sure it remains on top on wallpaper subject (only needed when depth wallpaper is active)
					if (android.os.Build.VERSION.SDK_INT < 37) {
						mDeviceEntryIconView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
							@Override
							public void onViewAttachedToWindow(@NonNull View v) {
								v.setZ(100);
							}

							@Override
							public void onViewDetachedFromWindow(@NonNull View v) {

							}
						});

						mDeviceEntryIconView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
								                                               v.setZ(100));
					}
				});
	}

	/** @noinspection ConstantValue*/
	private void setUDFPSGraphics(boolean force) {
		if(mDeviceEntryIconView == null) return;

		if(transparentFG || force)
		{
			((ImageView) getObjectField(mDeviceEntryIconView, "iconView"))
					.setImageAlpha(transparentFG
							? TRANSPARENT
							: OPAQUE);
		}
		if(transparentFG || transparentBG || force) {
			((ImageView) getObjectField(mDeviceEntryIconView, "bgView"))
					.setImageAlpha(transparentFG || transparentBG
							? TRANSPARENT
							: OPAQUE);
		}
	}
}
