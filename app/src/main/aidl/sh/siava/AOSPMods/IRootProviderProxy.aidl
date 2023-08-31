// IRootProviderProxy.aidl
package sh.siava.AOSPMods;

// Declare any non-default types here with import statements

interface IRootProviderProxy {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     String[] runCommand(String command);
     IBinder getFileSystemService();
}