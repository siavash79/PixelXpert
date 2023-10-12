PKGNAME="sh.siava.AOSPMods"
PKGPATH="/system/priv-app/AOSPMods/AOSPMods.apk"
LSPDDBPATH="/data/adb/lspd/config/modules_config.db"
MAGISKDBPATH="/data/adb/magisk.db"

prepareSQL(){
	unzip $ZIPFILE sqlite3 -d $TMPDIR/ > /dev/null
	chmod +x $TMPDIR/sqlite3

	SQLITEPATH="$TMPDIR/sqlite3"
}

# runSQL "database path" "command" - then you can use $SQLRESULT to read the outcome
runSQL(){
	SQLRESULT=$($SQLITEPATH $DBPATH "$CMD")
}

#grant silent root access to given UID
grantRootUID(){
	DBPATH=$MAGISKDBPATH
	
	#new record - older magisk compatibility
	CMD="insert into policies (uid, package_name, policy, until, logging, notification) values ($1, '$2', 2, 0, 1, 0);" && runSQL
	#new record
	CMD="insert into policies (uid, policy, until, logging, notification) values ($1, 2, 0, 1, 0);" && runSQL
	#previously present record
	CMD="update policies set policy = 2, until = 0, logging = 1, notification = 0 where uid = $1;" && runSQL
}


#grant root access to given package name
grantRootPkg(){
	ui_print "- 	Granting root access to $1..."
	UID=$(pm list packages -U $1 --user 0 | grep ":$1 " | awk -F 'uid:' '{ print $2 }' | cut -d ',' -f 1)

	grantRootUID $UID $1
}

#grant root access to required apps
grantRootApps(){
	grantRootPkg $PKGNAME
}

migratePrefs(){
  am start -n "$PKGNAME/.UpdateActivity" -e migratePrefs true > /dev/null
}

#activate PKGNAME in Lsposed
activateModuleLSPD()
{	
	DBPATH=$LSPDDBPATH
	
	ui_print '- Trying to activate the module in Lsposed...'	
	
	CMD="select mid from modules where module_pkg_name like \"$PKGNAME\";" && runSQL
	OLDMID=$(echo $SQLRESULT | xargs)


	if [ $(($OLDMID+0)) -gt 0 ]; then
		CMD="select mid from modules where mid = $OLDMID and apk_path like \"$PKGPATH\" and enabled = 1;" && runSQL
		REALMID=$(echo $SQLRESULT | xargs)
		
		if [ $(($REALMID+0)) = 0 ]; then
			CMD="delete from scope where mid = $OLDMID;" && runSQL
			CMD="delete from modules where mid = $OLDMID;" && runSQL
		fi
	fi
	
#some commands may fail. It's OK if they do	
	CMD="insert into modules (\"module_pkg_name\", \"apk_path\", \"enabled\") values (\"$PKGNAME\",\"$PKGPATH\", 1);" && runSQL
	
	CMD="select mid as ss from modules where module_pkg_name = \"$PKGNAME\";" && runSQL
	
	NEWMID=$(echo $SQLRESULT | xargs)

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"android\",0);" && runSQL
	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"system\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.systemui\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.google.android.apps.nexuslauncher\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.google.android.dialer\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.phone\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.settings\",0);" && runSQL

	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"$PKGNAME\",0);" && runSQL
}

prepareSQL

ui_print ''
ui_print ''

grantRootApps

if [ $(ls $LSPDDBPATH) = $LSPDDBPATH ]; then
	ui_print ''
	ui_print ''

	activateModuleLSPD
	migratePrefs


	ui_print ''
	ui_print ''
	ui_print 'Installation Complete!'
	ui_print 'Please Reboot your device to activate'
	ui_print '(Activation of additional fonts may take one more reboot)'
else
	ui_print 'Lsposed not found!!'
	ui_print 'This module will not work without Lsposed'
	ui_print 'Please:'
	ui_print '- Insall Lsposed'
	ui_print '- Reboot'
	ui_print '- Manually enable AOSPMods in Lsposed'
	ui_print '- Reboot'
fi

	ui_print ''
	ui_print '  **********************'
	ui_print '  * Brought to you by: *'
	ui_print '  *                    *'
	ui_print '  * siavash79@github   *'
	ui_print '  *                    *'
	ui_print '  * eltifo@github      *'
	ui_print '  **********************'
	ui_print ''
