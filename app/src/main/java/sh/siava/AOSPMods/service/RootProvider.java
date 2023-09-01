package sh.siava.AOSPMods.service;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.util.List;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.IRootProviderService;
import sh.siava.AOSPMods.modpacks.Constants;

public class RootProvider extends RootService {
	/** @noinspection unused*/
	String TAG = getClass().getSimpleName();

	static final String LSPD_DB_PATH = "/data/adb/lspd/config/modules_config.db";
	static final String SQLITE_BIN = "/data/adb/modules/AOSPMods/sqlite3";

	@Override
	public IBinder onBind(@NonNull Intent intent) {
		return new RootServicesIPC();
	}

	/** @noinspection RedundantThrows*/
	class RootServicesIPC extends IRootProviderService.Stub
	{
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


				return "1".equals(
						runLSposedSQLiteQuery(
								String.format("select count(*) from scope where mid = %s and user_id = 0 and app_pkg_name = '%s'", mLSPosedMID, packageName)
						).get(0));
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
			if (Constants.SYSTEM_FRAMEWORK_PACKAGE.equals(packageName)) //new LSPosed versions renamed framework
				packageName = "system";

			if (checkLSPosedDB(packageName))
				return true;

			getModuleMID();

			if (!mLSPosedEnabled) {
				enableModuleLSPosed();

				if (checkLSPosedDB(packageName))
					return true;
			}

			runLSposedSQLiteQuery(
					String.format("insert into scope (mid, app_pkg_name, user_id) values (%s, '%s', 0)", mLSPosedMID, packageName));

			return checkLSPosedDB(packageName);
		}
		private void enableModuleLSPosed() {
			runLSposedSQLiteQuery(String.format("update modules set enabled = 1 where mid = %s", mLSPosedMID));
		}

		private void getModuleMID()
		{
			mLSPosedMID = Integer.parseInt(
					runLSposedSQLiteQuery(
							String.format("select mid from modules where module_pkg_name = '%s'", BuildConfig.APPLICATION_ID)
					).get(0));

			mLSPosedEnabled = "1".equals(
					runLSposedSQLiteQuery(
							String.format("select enabled from modules where mid = %s", mLSPosedMID)
					).get(0));
		}

		private List<String> runLSposedSQLiteQuery(String command)
		{
			return Shell.cmd(String.format("%s %s \"%s\"", SQLITE_BIN, LSPD_DB_PATH, command)).exec().getOut();
		}

		@Override
		public IBinder getFileSystemService(){
			return FileSystemManager.getService();
		}
	}
}
