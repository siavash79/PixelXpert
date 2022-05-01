#!/bin/bash

NEWVERCODE=$(($(cat app/build.gradle | grep versionCode | tr -s ' ' |cut -d " " -f 3 | tr -d '\r')+1))
NEWVERNAME=${GITHUB_REF_NAME/v/}

sed -i 's/versionCode.*/versionCode '$NEWVERCODE'/' app/build.gradle
sed -i 's/versionName.*/versionName "'$NEWVERNAME'"/' app/build.gradle

sed -i 's/version=.*/version='$NEWVERNAME'/' MagiskMod/module.prop
sed -i 's/versionCode=.*/versionCode='$NEWVERCODE'/' MagiskMod/module.prop

sed -i 's/"version":.*/"version": "'$NEWVERNAME'",/' MagiskModuleUpdate.json
sed -i 's/"versionCode":.*/"versionCode": '$NEWVERCODE',/' MagiskModuleUpdate.json
sed -i 's/""zipUrl":.*/""zipUrl": "https://github.com/siavash79/AOSPMods/releases/download/"'$GITHUB_REF_NAME'/AOSPMods.zip",/' MagiskModuleUpdate.json