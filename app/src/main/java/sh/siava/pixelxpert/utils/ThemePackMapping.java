package sh.siava.pixelxpert.utils;

import java.util.HashMap;

public class ThemePackMapping {
	public static class OverlayID
	{
		public int resID;
		public String packageName;

		public OverlayID(String packageName, int resID)
		{
			this.resID = resID;
			this.packageName = packageName;
		}
	}

	public static class OverlayIDName
	{
		public String resName;
		public String packageName;

		public OverlayIDName(String packageName, String resName)
		{
			this.resName = resName;
			this.packageName = packageName;
		}
	}


	public static class IDMapping extends HashMap<Integer, OverlayID>
	{}

	public static class Mapping extends HashMap<String, OverlayIDName>
	{}

}
