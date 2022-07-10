cp app\release\AOSPMods.apk MagiskModBase\system\priv-app\AOSPMods

cd MagiskModBase

zip -r -9 -q ..\AOSPMods.zip *.*

rm -Rf system\priv-app\AOSPMods\AOSPMods.apk