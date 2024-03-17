package sh.siava.pixelxpert.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.util.Arrays;
import java.util.List;

import sh.siava.pixelxpert.IRootProviderProxy;
import sh.siava.pixelxpert.R;

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

		private final List<String> rootAllowedPacks;
		private final boolean rootGranted;

		private RootPoviderProxyIPC(Context context)
		{
			rootGranted = Shell.getShell().isRoot();

			rootAllowedPacks = Arrays.asList(context.getResources().getStringArray(R.array.root_requirement));
		}

		/** @noinspection RedundantThrows*/
		@Override
		public String[] runCommand(String command) throws RemoteException {
			try {
				ensureEnvironment();

				List<String> result = Shell.cmd(command).exec().getOut();
				return result.toArray(new String[0]);
			}
			catch (Throwable t)
			{
				return new String[0];
			}
		}

		private void ensureEnvironment() throws RemoteException {
			if(!rootGranted)
			{
				throw new RemoteException("Root permission denied");
			}

			ensureSecurity(Binder.getCallingUid());
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
