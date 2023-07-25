#!/bin/bash

mkdir -p output
cp app/build/outputs/apk/release/AOSPMods-signed.apk MagiskModBase/system/priv-app/AOSPMods/AOSPMods.apk
cd MagiskModBase;
LITE_FILENAME="AOSPMods_Xposed.zip"
FULL_FILENAME="AOSPMods_Full.zip"

echo 1 > build.type
zip -r ../output/$LITE_FILENAME *;

cp -rf ../MagiskModAddon/* ./
echo 0 > build.type

mkdir ../tmp
cp module.prop ../tmp/module.prop

sed -i 's/Lite (Xposed only)/Full Version/' module.prop
sed -i 's/MagiskModuleUpdate_Xposed.json/MagiskModuleUpdate_Full.json/' module.prop

zip -r ../output/$FULL_FILENAME *;

rm -rf module.prop
mv ../tmp/module.prop module.prop