#!/bin/bash

NEWVERCODE=$(($(cat build.gradle | grep versionCode | tr -s ' ' |cut -d " " -f 3 | tr -d '\r')+1))
NEWVERNAME=${GITHUB_REF_NAME/v/}

sed -i 's/versionCode.*/versionCode '$NEWVERCODE'/' build.gradle
sed -i 's/versionName.*/versionName "'$NEWVERNAME'"/' build.gradle

sed -i 's/version=.*/version='$NEWVERNAME'/' MagiskMod/module.prop
sed -i 's/versionCode=.*/versionCode='$NEWVERCODE'/' MagiskMod/module.prop