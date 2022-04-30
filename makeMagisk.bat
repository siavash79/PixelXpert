cp app\release\AOSPMods.apk MagiskMod\system\priv-app\AOSPMods

cd MagiskMod

zip -r -9 -q ..\AOSPMods.zip *.*

rm -Rf system\priv-app\AOSPMods\AOSPMods.apk