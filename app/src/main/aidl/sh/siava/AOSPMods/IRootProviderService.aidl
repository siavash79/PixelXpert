// IRootProviderService.aidl
package sh.siava.AOSPMods;

// Declare any non-default types here with import statements

interface IRootProviderService {
	boolean checkLSPosedDB(String packageName);
	boolean isPackageInstalled(String packageName);
	boolean activateInLSPosed(String packageName);
	boolean grantRootMagisk(String packageName);
	IBinder getFileSystemService();
}