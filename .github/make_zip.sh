#!/bin/bash

mkdir -p output/full output/xposed
cp app/build/outputs/apk/release/AOSPMods.apk MagiskModBase/system/priv-app/AOSPMods/AOSPMods.apk
cd MagiskModBase;
LITE_FILENAME="AOSPMods_Xposed.zip"
FULL_FILENAME="AOSPMods_Full.zip"

echo 1 > build.type
zip -r ../output/xposed/$LITE_FILENAME *;

cp -r ../MagiskModAddon/* ./
echo 0 > build.type

zip -r ../output/full/$FULL_FILENAME *;
