#!/bin/bash

mkdir -p output
cp app/build/outputs/apk/release/AOSPMods-signed.apk MagiskModBase/system/priv-app/AOSPMods/AOSPMods.apk
cd MagiskModBase;
LITE_FILENAME="AOSPMods_Xposed.zip"
FULL_FILENAME="AOSPMods_Full.zip"

echo 1 > build.type
zip -r ../output/$LITE_FILENAME *;

#we'll need this file to return safely
mkdir ../tmp
mv module.prop ../tmp/module.prop

cp -rf ../MagiskModAddon/* ./
echo 0 > build.type

zip -r ../output/$FULL_FILENAME *;

#bring xposed mod prop back for commit
rm -rf module.prop
mv ../tmp/module.prop module.prop