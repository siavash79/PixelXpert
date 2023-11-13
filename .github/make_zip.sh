#!/bin/bash

mkdir -p output
cp app/build/outputs/apk/release/PixelXpert.apk MagiskModBase/system/priv-app/PixelXpert/PixelXpert.apk
cd MagiskModBase;
FILENAME="PixelXpert.zip"

echo 1 > build.type
zip -r ../output/$FILENAME *;