#!/bin/bash

NEWVERCODE=$(($(cat app/build.gradle | grep versionCode | tr -s ' ' | cut -d " " -f 3 | tr -d '\r')+1))
NEWVERNAME=${GITHUB_REF_NAME/v/}

#Prepare gradle script for build
sed -i 's/versionCode.*/versionCode '$NEWVERCODE'/' app/build.gradle
sed -i 's/versionName.*/versionName "'$NEWVERNAME'"/' app/build.gradle

#prepare magisk module props
#Xposed
sed -i 's/version=.*/version='$NEWVERNAME'/' MagiskModBase/module.prop
sed -i 's/versionCode=.*/versionCode='$NEWVERCODE'/' MagiskModBase/module.prop
#Full
sed -i 's/version=.*/version='$NEWVERNAME'/' MagiskModAddon/module.prop
sed -i 's/versionCode=.*/versionCode='$NEWVERCODE'/' MagiskModAddon/module.prop

#Prepare magisk module update files of legacy, full and xposed types
#legacy
sed -i 's/"version":.*/"version": "'$NEWVERNAME'",/' MagiskModuleUpdate.json
sed -i 's/"versionCode":.*/"versionCode": '$NEWVERCODE',/' MagiskModuleUpdate.json
sed -i 's/"zipUrl":.*/"zipUrl": "https:\/\/github.com\/siavash79\/AOSPMods\/releases\/download\/'$GITHUB_REF_NAME'\/AOSPMods_Full.zip",/' MagiskModuleUpdate.json
#Full
sed -i 's/"version":.*/"version": "'$NEWVERNAME'",/' MagiskModuleUpdate_Full.json
sed -i 's/"versionCode":.*/"versionCode": '$NEWVERCODE',/' MagiskModuleUpdate_Full.json
sed -i 's/"zipUrl":.*/"zipUrl": "https:\/\/github.com\/siavash79\/AOSPMods\/releases\/download\/'$GITHUB_REF_NAME'\/AOSPMods_Full.zip",/' MagiskModuleUpdate_Full.json
#Xposed
sed -i 's/"version":.*/"version": "'$NEWVERNAME'",/' MagiskModuleUpdate_Xposed.json
sed -i 's/"versionCode":.*/"versionCode": '$NEWVERCODE',/' MagiskModuleUpdate_Xposed.json
sed -i 's/"zipUrl":.*/"zipUrl": "https:\/\/github.com\/siavash79\/AOSPMods\/releases\/download\/'$GITHUB_REF_NAME'\/AOSPMods_Xposed.zip",/' MagiskModuleUpdate_Xposed.json
#In-app updater
sed -i 's/"version":.*/"version": "'$NEWVERNAME'",/' latestStable.json
sed -i 's/"versionCode":.*/"versionCode": '$NEWVERCODE',/' latestStable.json
sed -i 's/"zipUrl_FUll":.*/"zipUrl": "https:\/\/github.com\/siavash79\/AOSPMods\/releases\/download\/'$GITHUB_REF_NAME'\/AOSPMods_Full.zip",/' latestStable.json
sed -i 's/"zipUrl_Xposed":.*/"zipUrl": "https:\/\/github.com\/siavash79\/AOSPMods\/releases\/download\/'$GITHUB_REF_NAME'\/AOSPMods_Xposed.zip",/' latestStable.json

# module changelog
echo "**$NEWVERNAME**" > newChangeLog.md
cat .github/workflowFiles/FutureChanageLog.md >> newChangeLog.md
echo "  " >> newChangeLog.md
cat MagiskChangelog.md >> newChangeLog.md
mv  newChangeLog.md MagiskChangelog.md

# release notes
echo "**Changelog:**  " > releaseNotes.md
cat .github/workflowFiles/FutureChanageLog.md >> releaseNotes.md
cat .github/workflowFiles/ReleaseNotesTemplate.md >> releaseNotes.md

echo "*$NEWVERNAME* released in stable channel  " > telegram.msg
echo "  " >> telegram.msg
echo "*Changelog:*  " >> telegram.msg
cat .github/workflowFiles/FutureChanageLog.md >> telegram.msg
echo 'TMessage<<EOF' >> $GITHUB_ENV
cat telegram.msg >> $GITHUB_ENV
echo >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV