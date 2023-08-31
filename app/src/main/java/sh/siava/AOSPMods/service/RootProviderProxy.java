package sh.siava.AOSPMods.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.Arrays;
import java.util.List;

import sh.siava.AOSPMods.IRootProviderProxy;
import sh.siava.AOSPMods.IRootProviderService;
import sh.siava.AOSPMods.R;

public class RootProviderProxy extends Service {
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return new RootPoviderProxyIPC(this);
	}

	class RootPoviderProxyIPC extends IRootProviderProxy.Stub
	{
		/** @noinspection unused*/
		String TAG = getClass().getSimpleName();

		private final Context mContext;
		private IRootProviderService mRootServiceIPC;
		private final List<String> rootAllowedPacks;

		private final ServiceConnection mRootServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRootServiceIPC = IRootProviderService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mRootServiceIPC = null;
		}
	};


		private RootPoviderProxyIPC(Context context)
		{
			mContext = context;

			rootAllowedPacks = Arrays.asList(mContext.getResources().getStringArray(R.array.root_requirement));

			startRootService();
		}

		private void startRootService()
		{
			// Start RootService connection
			Intent intent = new Intent(mContext, RootProvider.class);
			RootService.bind(intent, mRootServiceConnection);
		}

		@Override
		public String[] runCommand(String command) throws RemoteException {
			ensureEnvironment();

			List<String> result = Shell.cmd(command).exec().getOut();
			return result.toArray(new String[0]);
		}

		@Override
		public IBinder getFileSystemService() throws RemoteException {
			ensureEnvironment();

			return mRootServiceIPC.getFileSystemService();
		}

		private void ensureEnvironment() throws RemoteException {
			ensureSecurity(Binder.getCallingUid());

			if(mRootServiceIPC == null)
			{
				startRootService();

				long startTime = System.currentTimeMillis();
				while(mRootServiceIPC == null && System.currentTimeMillis() > startTime + 5000)
				{
					try {
						//noinspection BusyWait
						Thread.sleep(50);
					} catch (InterruptedException ignored) {
						throw new RemoteException("Root service connection interrupted");
					}
				}

				if(mRootServiceIPC == null)
				{
					//This shouldn't happen at all
					throw new RemoteException("Timeout: Could not connect to root service");
				}
			}
		}

		private void ensureSecurity(int uid) throws RemoteException {
			for (String packageName : getPackageManager().getPackagesForUid(uid)) {
				if(rootAllowedPacks.contains(packageName))
					return;
			}
			throw new RemoteException("You do know you're not supposed to use this service. So...");
		}
	}
}
