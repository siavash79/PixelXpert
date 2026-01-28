package sh.siava.pixelxpert.xposed.utils.toolkit;

import static de.robv.android.xposed.XposedBridge.log;

import android.view.View;
import android.view.ViewGroup;

/** @noinspection unused, RedundantThrows */
public class ReflectionTools {
	public static void runDelayedOnMainThread(View viewObject, long delay, Runnable runnable)
	{
		new Thread(() -> {
			try
			{
				Thread.sleep(delay);
				viewObject.post(runnable);
			}
			catch (Throwable ignored)
			{}
		}).start();
	}

	/**
	 Takes the view, removes it from wherever it is (if at all), and adds it to the parent we ask
	 */
	public static void reAddView(ViewGroup parentView, View childView) {
		if(childView == null) return;

		try {
			((ViewGroup)childView.getParent()).removeView(childView);
		} catch (Throwable ignored) {}
		parentView.addView(childView);
	}

	/**
	Takes the view, removes it from wherever it is, and adds it to the parent we ask for the index we ask
	*/
	public static void reAddView(ViewGroup parentView, View childView, int index)
	{
		if(childView == null) return;

		try {
			((ViewGroup)childView.getParent()).removeView(childView);
		} catch (Throwable ignored) {}
		parentView.addView(childView, index);
	}

	public static void dumpParentIDs(View v)
	{
		dumpParentIDs(v, 0);
	}

	private static void dumpParentIDs(View v, int level) {
		dumpID(v, level);
		try {
			if(v.getParent() instanceof View)
			{
				dumpParentIDs((View) v.getParent(), level+1);
			}
		}
		catch (Throwable ignored){}
	}

	public static void dumpIDs(View v)
	{
		dumpIDs(v, 0);
	}

	private static void dumpIDs(View v, int level)
	{
		dumpID(v, level);
		if(v instanceof ViewGroup)
		{
			for(int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
			{
				dumpIDs(((ViewGroup) v).getChildAt(i), level+1);
			}
		}
	}
	private static void dumpID(View v, int level)
	{
		String name = "**";
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < level; i++)
		{
			str.append("\t");
		}

		try {
			name = v.getContext().getResources().getResourceName(v.getId());
		}
		catch (Throwable ignored){}

		log(str+ "id " + name + " type " + v.getClass().getName());
	}
}
