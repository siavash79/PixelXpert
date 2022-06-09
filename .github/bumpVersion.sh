#!/bin/bash

NEWVERCODE=$(($(cat app/build.gradle | grep versionCode | tr -s ' ' | cut -d " " -f 3 | tr -d '\r')+1))
NEWVERNAME="canary-$NEWVERCODE"

sed -i 's/versionCode.*/versionCode '$NEWVERCODE'/' app/build.gradle
sed -i 's/versionName.*/versionName "'$NEWVERNAME'"/' app/build.gradle

sed -i 's/version=.*/version='$NEWVERNAME'/' MagiskMod/module.prop
sed -i 's/versionCode=.*/versionCode='$NEWVERCODE'/' MagiskMod/module.prop

sed -i 's/"version":.*/"version": "'$NEWVERNAME'",/' latestVersion.json
sed -i 's/"versionCode":.*/"versionCode": '$NEWVERCODE',/' latestVersion.json
sed -i 's/"zipUrl":.*/"zipUrl": "https:\/\/nightly.link\/siavash79\/AOSPMods\/actions\/runs\/'$1'\/AOSPMods.zip",/' latestVersion.json

# module changelog
echo "**$NEWVERNAME**  " > newChangeLog.md
cat changeLog.md >> newChangeLog.md
echo "  " >> newChangeLog.md
cat CanaryChangelog.md >> newChangeLog.md
mv  newChangeLog.md CanaryChangelog.md

echo "**$NEWVERNAME** released in canary channel  " > telegram.msg
echo "  " >> telegram.msg
echo "**Changelog:**  " >> telegram.msg
echo changeLog.md >> telegram.msg
TMessage=$(cat telegram.msg)
FinalTMessage=${TMessage@Q}
echo "TMessage=$FinalTMessage" >> $GITHUB_ENV