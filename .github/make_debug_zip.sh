#!/bin/bash

mkdir output
cp app/build/outputs/apk/debug/AOSPMods-signed.apk MagiskModBase/system/priv-app/AOSPMods/AOSPMods.apk
cd MagiskModBase

cp -r ../MagiskModAddon/* ./
echo 0 > build.type

FILENAME="AOSPMods.zip"
zip -r ../output/$FILENAME *;