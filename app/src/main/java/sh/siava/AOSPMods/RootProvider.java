package sh.siava.AOSPMods;

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

public class RootProvider extends RootService {
	static {
		// Only load the library when this class is loaded in a root process.
		// The classloader will load this class (and call this static block) in the non-root process because we accessed it when constructing the Intent to send.
		// Add this check so we don't unnecessarily load native code that'll never be used.
		if (Process.myUid() == 0)
			System.loadLibrary("sqliteX");
	}
	String TAG = getClass().getSimpleName();

	static final String LSPosedDB = "/data/adb/lspd/config/modules_config.db";
	int mLSPosedMID = -1;

	@Override
	public IBinder onBind(@NonNull Intent intent) {
		return new RootServicesIPC();
	}

	class RootServicesIPC extends IRootProviderService.Stub
	{
		private SQLiteDatabase mLSposedDB;

		@Override
		public boolean checkLSPosedDB(String packageName) {
			try
			{
				if(mLSPosedMID < 0)
					getModuleMID();

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

		private void getModuleMID()
		{
			Cursor cursor = getLSPosedDB().rawQuery(
					"select mid from modules where module_pkg_name = ?",
					new String[] {BuildConfig.APPLICATION_ID});

			cursor.moveToFirst();
			mLSPosedMID = cursor.getInt(0);
			cursor.close();
		}

		private SQLiteDatabase getLSPosedDB()
		{
			if(mLSposedDB == null || !mLSposedDB.isOpen())
			{
				mLSposedDB = SQLiteDatabase.openDatabase(LSPosedDB, null, SQLiteDatabase.OPEN_READWRITE);
			}
			return mLSposedDB;
		}

		@Override
		public IBinder getFileSystemService(){
			return FileSystemManager.getService();
		}
	}
}
