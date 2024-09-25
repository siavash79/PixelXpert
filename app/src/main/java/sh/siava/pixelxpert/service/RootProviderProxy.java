package sh.siava.pixelxpert.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import sh.siava.pixelxpert.IRootProviderProxy;
import sh.siava.pixelxpert.PixelXpert;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.utils.BitmapSubjectSegmenter;

public class RootProviderProxy extends Service {
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return new RootProviderProxyIPC(this);
	}

	class RootProviderProxyIPC extends IRootProviderProxy.Stub
	{
		/** @noinspection unused*/
		String TAG = getClass().getSimpleName();

		private final List<String> rootAllowedPacks;
		private final boolean rootGranted;

		private RootProviderProxyIPC(Context context)
		{
			try {
				Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
			}
			catch (Throwable ignored){}
			rootGranted = Shell.getShell().isRoot();

			if(!rootGranted)
			{
				context.sendBroadcast(new Intent(Constants.ACTION_KSU_ACQUIRE_ROOT));
			}

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

		@Override
		public Bitmap extractSubject(Bitmap input) throws RemoteException {
			ensureEnvironment();

			if(!PixelXpert.get().isCoreRootServiceBound())
			{
				PixelXpert.get().tryConnectRootService();
			}

			final Bitmap[] resultBitmap = new Bitmap[]{null};
			CountDownLatch resultWaiter = new CountDownLatch(1);
			try {
					new BitmapSubjectSegmenter(getApplicationContext()).segmentSubject(input, new BitmapSubjectSegmenter.SegmentResultListener() {
						@Override
						public void onSuccess(Bitmap result) {
							resultBitmap[0] = result;
							resultWaiter.countDown();
						}

						@Override
						public void onFail() {
							resultWaiter.countDown();
						}
					});
				resultWaiter.await();
				return resultBitmap[0];
			} catch (Throwable ignored) {}
			return null;
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