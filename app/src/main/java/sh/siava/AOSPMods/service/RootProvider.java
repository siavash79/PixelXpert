package sh.siava.AOSPMods.service;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;

import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.sqlite.database.sqlite.SQLiteDatabase;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.IRootProviderService;
import sh.siava.AOSPMods.modpacks.Constants;

public class RootProvider extends RootService {
	static {
		// Only load the library when this class is loaded in a root process.
		// The classloader will load this class (and call this static block) in the non-root process because we accessed it when constructing the Intent to send.
		// Add this check so we don't unnecessarily load native code that'll never be used.
		if (Process.myUid() == 0)
			System.loadLibrary("sqliteX");
	}
	/** @noinspection unused*/
	String TAG = getClass().getSimpleName();

	static final String LSPD_DB_PATH = "/data/adb/lspd/config/modules_config.db";
	static final String MAGISK_DB_PATH = "/data/adb/magisk.db";

	@Override
	public IBinder onBind(@NonNull Intent intent) {
		return new RootServicesIPC();
	}

	/** @noinspection RedundantThrows*/
	class RootServicesIPC extends IRootProviderService.Stub
	{
		private SQLiteDatabase mLSposedDB;
		private SQLiteDatabase mMagiskDB;

		int mLSPosedMID = -1;
		private boolean mLSPosedEnabled = false;


		@Override
		public boolean checkLSPosedDB(String packageName) {
			if(Constants.SYSTEM_FRAMEWORK_PACKAGE.equals(packageName))
				packageName = "system";

			try
			{
				if(mLSPosedMID < 0 || !mLSPosedEnabled)
					getModuleMID();

				if(!mLSPosedEnabled)
					return false;

				Cursor cursor = getLSPosedDB().rawQuery(
						"select count(*) from scope where mid = ? and user_id = 0 and app_pkg_name = ?",
						new String[]{String.valueOf(mLSPosedMID), packageName});

				cursor.moveToFirst();
				int count = cursor.getInt(0);
				cursor.close();

				return count > 0;
			}
			catch (Throwable ignored) {
				return false;
			}
		}

		@Override
		public boolean isPackageInstalled(String packageName) throws RemoteException {
			PackageManager pm = getPackageManager();
			try {
				pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
				return pm.getApplicationInfo(packageName, 0).enabled;
			} catch (PackageManager.NameNotFoundException ignored) {
				return false;
			}
		}

		@Override
		public boolean activateInLSPosed(String packageName) throws RemoteException {
			if(Constants.SYSTEM_FRAMEWORK_PACKAGE.equals(packageName)) //new LSPosed versions renamed framework
				packageName = "system";

			if(checkLSPosedDB(packageName))
				return true;

			getModuleMID();

			if(!mLSPosedEnabled)
			{
				enableModuleLSPosed();

				if(checkLSPosedDB(packageName))
					return true;
			}

			try
			{
				ContentValues values = new ContentValues();
				values.put("mid", mLSPosedMID);
				values.put("app_pkg_name", packageName);
				values.put("user_id", 0);
				getLSPosedDB().insert("scope", null, values);
				return true;
			}
			catch (Throwable ignored)
			{
				return false;
			}
		}

		private void enableModuleLSPosed() {
			try
			{
				ContentValues values = new ContentValues();
				values.put("enabled", 1);
				getLSPosedDB().update("modules", values, "mid = ?", new String[] {String.valueOf(mLSPosedMID)});
			}
			catch (Throwable ignored)
			{}
		}

		@Override
		public boolean grantRootMagisk(String packageName) throws RemoteException {
			try
			{
				int uid = getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid;
				try
				{
					ContentValues values = new ContentValues();
					values.put("uid", uid);
					values.put("policy", 2);
					values.put("until", 0);
					values.put("logging", 1);
					values.put("notification", 0);
					getMagiskDB().insertOrThrow("policies", null, values);
					return true;
				}
				catch (Throwable ignored)
				{
					ContentValues values = new ContentValues();
					values.put("policy", 2);
					getMagiskDB().update("policies", values, "uid = ?", new String[]{String.valueOf(uid)});
					return true;
				}
			} catch (Throwable ignored)
			{
				return false;
			}
		}

		private void getModuleMID()
		{
			Cursor cursor = getLSPosedDB().rawQuery(
					"select mid from modules where module_pkg_name = ?",
					new String[] {BuildConfig.APPLICATION_ID});

			cursor.moveToFirst();
			mLSPosedMID = cursor.getInt(0);
			cursor.close();

			cursor = getLSPosedDB().rawQuery(
					"select enabled from modules where mid = ?",
					new String[]{String.valueOf(mLSPosedMID)});

			cursor.moveToFirst();
			mLSPosedEnabled = cursor.getInt(0) == 1;
			cursor.close();
		}

		private SQLiteDatabase getLSPosedDB()
		{
			if(mLSposedDB == null || !mLSposedDB.isOpen())
			{
				mLSposedDB = SQLiteDatabase.openDatabase(LSPD_DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
			}
			return mLSposedDB;
		}

		private SQLiteDatabase getMagiskDB()
		{
			if(mMagiskDB == null || !mMagiskDB.isOpen())
			{
				mMagiskDB = SQLiteDatabase.openDatabase(MAGISK_DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
			}
			return mMagiskDB;
		}

		@Override
		public IBinder getFileSystemService(){
			return FileSystemManager.getService();
		}
	}
}
