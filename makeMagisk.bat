cp app\release\PixelXpert.apk MagiskModBase\system\priv-app\PixelXpert

cd MagiskModBase

zip -r -9 -q ..\PixelXpert.zip *.*

rm -Rf system\priv-app\PixelXpert\PixelXpert.apk