package sh.siava.AOSPMods;

import android.content.res.XModuleResources;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class UDFPSResources extends aResManager {

    public UDFPSResources(String MODULE_PATH, XC_InitPackageResources.InitPackageResourcesParam resparam, XModuleResources modRes) {
        super(MODULE_PATH, resparam, modRes);
    }

    public void setTransparent(boolean transparent)
    {
        if(transparent)
        {
            resparam.res.setReplacement(resparam.packageName, "drawable", "fingerprint_bg", modRes.fwd(R.drawable.fingerprint_bg_transparent));
        }
        else
        {
            //undo it......... somehow......
        }
    }

    @Override
    public void hookResources() {
        /*
        resparam.res.hookLayout("com.android.systemui", "layout", "udfps_keyguard_view", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                XposedBridge.log("RSIAPOSED: found res hook");
                ArrayList<View> vf = new ArrayList<View>();
                liparam.view.findViewsWithText(vf, "", View.FIND_VIEWS_WITH_TEXT);
                XposedBridge.log("RSIAPOSED: found as many" + vf.size());

                for(int i = 0; i<vf.size(); i++)
                {
                    XposedBridge.log("RSIAPOSED: focusable:" + vf.get(i).getId());
                }
                Object bg = liparam.view.findViewById(liparam.res.getIdentifier("udfps_keyguard_fp_bg","layout", "com.android.systemui"));
                XposedBridge.log("RSIAPOSED: found bg img: " + bg.getClass().getName());
 //               bg.setVisibility(View.INVISIBLE);
                XposedBridge.log("RSIAPOSED: set invis");

            }
        });*/
    }
}
