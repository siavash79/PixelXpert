package sh.siava.AOSPMods.android;

import android.view.InputDevice;
import android.view.View;
import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class VolumeToSkip implements IXposedModPack {
  public static final String listenPackage = "android.inputmethodservice.InputMethodService";
  public static boolean VolumeToSkipEnabled = false;
  
  
  public void updatePrefs()
    {
        if(XPrefs.Xprefs == null) return;
        VolumeToSkipEnabled = XPrefs.Xprefs.getBoolean("VolumeToSkip", false);
    }
  
  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;
    
  findAndHookMethod(listenPackage, null, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (mService.isInputViewShown()) {
                            int newKeyCode = (cursorControl == 1) ?
                                    KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                            mService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                        } else
                            param.setResult(false);
                    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        if (mService.isInputViewShown()) {
                            int newKeyCode = (cursorControl == 1) ?
                                    KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
                            mService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                        } else
                            param.setResult(false);
                    }
                }
            });
        
findAndHookMethod(listenPackage, null, "onKeyUp", int.class, KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        param.setResult(mService.isInputViewShown());

                }
            });
        }
