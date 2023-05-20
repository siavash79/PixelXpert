package sh.siava.AOSPMods.utils;

import static com.topjohnwu.superuser.Shell.cmd;

import android.os.FileUtils;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipFile;

public class AppUtils {

	public static void RestartSystemUI() {
		cmd("killall com.android.systemui").submit();
	}

	public static void Restart() {
		cmd("am start -a android.intent.action.REBOOT").submit();
	}

	public static boolean installDoubleZip(String DoubleZipped) //installs the zip magisk module. even if it's zipped inside another zip
	{
		try {
			//copy it to somewhere under our control
			File tempFile = File.createTempFile("doubleZ", ".zip");
			Shell.cmd(String.format("cp %s %s", DoubleZipped, tempFile.getAbsolutePath())).exec();

			//unzip once, IF double zipped
			ZipFile unzipper = new ZipFile(tempFile);

			File unzippedFile;
			if (unzipper.stream().count() == 1) {
				unzippedFile = File.createTempFile("singleZ", "zip");
				FileOutputStream unzipOutputStream = new FileOutputStream(unzippedFile);
				FileUtils.copy(unzipper.getInputStream(unzipper.entries().nextElement()), unzipOutputStream);
				unzipOutputStream.close();
			} else {
				unzippedFile = tempFile;
			}

			//install
			Shell.cmd(String.format("magisk --install-module %s", unzippedFile.getAbsolutePath())).exec();

			//cleanup
			//noinspection ResultOfMethodCallIgnored
			tempFile.delete();
			//noinspection ResultOfMethodCallIgnored
			unzippedFile.delete();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}