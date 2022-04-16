DBPATH="/data/adb/lspd/config/modules_config.db"
SQLITEPATH="$TMPDIR/sqlite3"
PKGNAME="sh.siava.AOSPMods"
PKGPATH="/system/priv-app/AOSPMods/AOSPMods.apk"

activate_module()
{
	unzip $ZIPFILE sqlite3 -d $TMPDIR/ > /dev/null
	chmod 777 $TMPDIR/sqlite3
	
	ui_print 'Checking activation status in Lsposed...'	
	OLDMID=$($SQLITEPATH $DBPATH "select mid from modules where module_pkg_name like \"$PKGNAME\";" | xargs)

	if [ $(($OLDMID+0)) = 0 ]; then
		PRESENTINDB=false
	else
		PRESENTINDB=true
		REALMID=$($SQLITEPATH $DBPATH "select mid from modules where mid = $OLDMID and apk_path like \"$PKGPATH\" and enabled = 1;" | xargs)
		if [ $(($REALMID+0)) = 0 ]; then
			$($SQLITEPATH $DBPATH "delete from scope where mid = $OLDMID;")
			$($SQLITEPATH $DBPATH "delete from modules where mid = $OLDMID;")		
		fi
	fi
	
	ui_print 'Trying to activate the module in Lsposed...'	

#some commands may fail. It's OK if they do	
	$($SQLITEPATH $DBPATH "insert into modules (\"module_pkg_name\", \"apk_path\", \"enabled\") values (\"$PKGNAME\",\"$PKGPATH\", 1);")
	NEWMID=$($SQLITEPATH $DBPATH "select mid as ss from modules where module_pkg_name = \"$PKGNAME\";")

	$($SQLITEPATH $DBPATH "insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.android.systemui\",0);")
	$($SQLITEPATH $DBPATH "insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"com.google.android.apps.nexuslauncher\",0);")
#	$($SQLITEPATH $DBPATH "insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"android\",0);")
	$($SQLITEPATH $DBPATH "insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"$PKGNAME\",0);")

}

rm -f "$MODPATH/sqlite3"


if [ $(ls $DBPATH) = $DBPATH ]; then
	activate_module
	
	ui_print 'Installation Complete!'
	ui_print 'Please Reboot your device <2 times!> to activate'
else
	ui_print 'Lspsed not found!!'
	ui_print 'This module will not work without Lsposed'
	ui_print 'Please:'
	ui_print '- Insall Lsposed'
	ui_print '- Reboot'
	ui_print '- Manually enable AOSPMods in Lsposed'
	ui_print '- Reboot'

	exit
fi


