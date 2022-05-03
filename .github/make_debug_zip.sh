#!/bin/bash

mkdir output
cd MagiskMod;
cp app/build/outputs/apk/debug/AOSPMods-signed.apk MagiskMod/system/priv-app/AOSPMods/AOSPMods.apk
FILENAME="AOSPMods.zip"
zip -r ../output/$FILENAME *;