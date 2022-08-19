package sh.siava.AOSPMods.utils;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setFloatField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.view.View;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;

public class NotificationIconContainerOverride {
    public static Class<?> StatusBarIconViewClass = null;

    public static final int STATE_ICON = 0;
    public static final int STATE_DOT = 1;
    public static final int STATE_HIDDEN = 2;

    private static final int MAX_VISIBLE_ICONS_ON_LOCK = 5;
    public static int MAX_STATIC_ICONS = 4;
    private static final int MAX_ICONS_ON_AOD = 3;
    private static final int MAX_DOTS = 1;
    private static final int NO_VALUE = Integer.MIN_VALUE;

    public static void calculateIconTranslations(XC_MethodHook.MethodHookParam param) { //A12
        FrameLayout thisObject = (FrameLayout) param.thisObject;

        Object mIconStates = getObjectField(thisObject, "mIconStates");

        float translationX = (float) callMethod(thisObject, "getActualPaddingStart");
        int firstOverflowIndex = -1;
        int childCount = thisObject.getChildCount();
        int maxVisibleIcons = getBooleanField(thisObject, "mOnLockScreen") ? MAX_VISIBLE_ICONS_ON_LOCK :
                getBooleanField(thisObject, "mIsStaticLayout") ? MAX_STATIC_ICONS : childCount;
        float layoutEnd = (float) callMethod(thisObject, "getLayoutEnd");
        float overflowStart = (float) callMethod(thisObject, "getMaxOverflowStart");
        setObjectField(thisObject, "mVisualOverflowStart", 0);
        setObjectField(thisObject, "mFirstVisibleIconState", null);
        for (int i = 0; i < childCount; i++) {
            View view = thisObject.getChildAt(i);
            Object iconState = callMethod(mIconStates, "get", view);
            if (getFloatField(iconState, "iconAppearAmount") == 1.0f) {
                // We only modify the xTranslation if it's fully inside of the container
                // since during the transition to the shelf, the translations are controlled
                // from the outside
                setObjectField(iconState, "xTranslation", translationX);
            }
            if (getObjectField(thisObject, "mFirstVisibleIconState") == null) {
                setObjectField(thisObject, "mFirstVisibleIconState", iconState);
            }
            int mSpeedBumpIndex = getIntField(thisObject, "mSpeedBumpIndex");
            boolean forceOverflow = mSpeedBumpIndex != -1 && i >= mSpeedBumpIndex
                    && getFloatField(iconState, "iconAppearAmount") > 0.0f || i >= maxVisibleIcons;
            boolean noOverflowAfter = i == childCount - 1;
            float drawingScale = getBooleanField(thisObject, "mOnLockScreen") && view.getClass() == StatusBarIconViewClass
                    ? (float) callMethod(view, "getIconScaleIncreased")
                    : 1f;
            setObjectField(iconState, "visibleState", getBooleanField(iconState, "hidden")
                    ? STATE_HIDDEN
                    : STATE_ICON);

            int mIconSize = getIntField(thisObject, "mIconSize");
            boolean isOverflowing =
                    (translationX > (noOverflowAfter ? layoutEnd - mIconSize
                            : overflowStart - mIconSize));
            if (firstOverflowIndex == -1 && (forceOverflow || isOverflowing)) {
                firstOverflowIndex = noOverflowAfter && !forceOverflow ? i - 1 : i;
                setFloatField(thisObject, "mVisualOverflowStart", layoutEnd - getIntField(thisObject, "mOverflowWidth"));
                if (forceOverflow || getBooleanField(thisObject, "mIsStaticLayout")) {
                    setObjectField(thisObject, "mVisualOverflowStart", Math.min(translationX, getFloatField(thisObject, "mVisualOverflowStart")));
                }
            }
            translationX += getFloatField(iconState, "iconAppearAmount") * view.getWidth() * drawingScale;
        }
        setObjectField(thisObject, "mNumDots", 0);
        if (firstOverflowIndex != -1) {
            translationX = getFloatField(thisObject, "mVisualOverflowStart");
            for (int i = firstOverflowIndex; i < childCount; i++) {
                View view = thisObject.getChildAt(i);
                Object iconState = callMethod(mIconStates, "get", view);
                int dotWidth = getIntField(thisObject, "mStaticDotDiameter") + getIntField(thisObject, "mDotPadding");
                setObjectField(iconState, "xTranslation", translationX);
                if (getIntField(thisObject, "mNumDots") < MAX_DOTS) {
                    if (getIntField(thisObject, "mNumDots") == 0 && getFloatField(iconState, "iconAppearAmount") < 0.8f) {
                        setObjectField(iconState, "visibleState", STATE_ICON);
                    } else {
                        setObjectField(iconState, "visibleState", STATE_DOT);
                        setObjectField(thisObject, "mNumDots", getIntField(thisObject, "mNumDots") + 1);
                    }
                    translationX += (getIntField(thisObject, "mNumDots") == MAX_DOTS ? MAX_DOTS * dotWidth : dotWidth)
                            * getFloatField(iconState, "iconAppearAmount");
                    setObjectField(thisObject, "mLastVisibleIconState", iconState);
                } else {
                    setObjectField(iconState, "visibleState", STATE_HIDDEN);
                }
            }
        } else if (childCount > 0) {
            View lastChild = thisObject.getChildAt(childCount - 1);

            setObjectField(thisObject, "mLastVisibleIconState", callMethod(mIconStates, "get", lastChild));
            setObjectField(thisObject, "mFirstVisibleIconState", callMethod(mIconStates, "get", thisObject.getChildAt(0)));
        }

        if ((boolean) callMethod(thisObject, "isLayoutRtl")) {
            for (int i = 0; i < childCount; i++) {
                View view = thisObject.getChildAt(i);
                Object iconState = callMethod(mIconStates, "get", view);
                setObjectField(iconState, "xTranslation", thisObject.getWidth() - getFloatField(iconState, "xTranslation") - view.getWidth());
            }
        }
        if (getObjectField(thisObject, "mIsolatedIcon") != null) {
            Object iconState = callMethod(mIconStates, "get", getObjectField(thisObject, "mIsolatedIcon"));
            if (iconState != null) {
                // Most of the time the icon isn't yet added when this is called but only happening
                // later
                int[] mAbsolutePosition = (int[]) getObjectField(thisObject, "mAbsolutePosition");
                setObjectField(iconState, "xTranslation", getFloatField(getObjectField(thisObject, "mIsolatedIconLocation"), "left") - mAbsolutePosition[0]
                        - (1 - (float) callMethod(getObjectField(thisObject, "mIsolatedIcon"), "getIconScale")) * (int) callMethod(getObjectField(thisObject, "mIsolatedIcon"), "getWidth") / 2.0f);
                setObjectField(iconState, "visibleState", STATE_ICON);
            }
        }
    }

    public static void calculateIconXTranslations(XC_MethodHook.MethodHookParam param) { //A13
        FrameLayout thisObject = (FrameLayout) param.thisObject;

        Object mIconStates = getObjectField(thisObject, "mIconStates");

        float translationX = (float) callMethod(thisObject, "getActualPaddingStart");
        int firstOverflowIndex = -1;
        int childCount = thisObject.getChildCount();
        int maxVisibleIcons = getBooleanField(thisObject, "mOnLockScreen") ? MAX_ICONS_ON_AOD :
                getBooleanField(thisObject, "mIsStaticLayout") ? MAX_STATIC_ICONS : childCount;
        float layoutEnd = getLayoutEnd(thisObject);
        setObjectField(thisObject, "mVisualOverflowStart", 0);
        setObjectField(thisObject, "mFirstVisibleIconState", null);
        for (int i = 0; i < childCount; i++) {
            View view = thisObject.getChildAt(i);
            Object iconState = callMethod(mIconStates,"get", view);
            if (getFloatField(iconState, "iconAppearAmount") == 1.0f) {
                // We only modify the xTranslation if it's fully inside of the container
                // since during the transition to the shelf, the translations are controlled
                // from the outside
                setObjectField(iconState, "xTranslation", translationX);
            }
            if (getObjectField(thisObject, "mFirstVisibleIconState") == null) {
                setObjectField(thisObject, "mFirstVisibleIconState", iconState);
            }
            int mSpeedBumpIndex = getIntField(thisObject, "mSpeedBumpIndex");
            boolean forceOverflow = mSpeedBumpIndex != -1 && i >= mSpeedBumpIndex
                    && getFloatField(iconState, "iconAppearAmount") > 0.0f || i >= maxVisibleIcons;
            boolean isLastChild = i == childCount - 1;
            float drawingScale = getBooleanField(thisObject, "mOnLockScreen") && view.getClass().getName().endsWith("StatusBarIconView")
                    ? getIconScaleIncreased(view)
                    : 1f;
            setObjectField(iconState, "visibleState", getBooleanField(iconState, "hidden")
                    ? STATE_HIDDEN
                    : STATE_ICON);

            final float overflowDotX = layoutEnd - getIntField(thisObject, "mIconSize");
            boolean isOverflowing = translationX > overflowDotX;

            if (firstOverflowIndex == -1 && (forceOverflow || isOverflowing)) {
                firstOverflowIndex = isLastChild && !forceOverflow ? i - 1 : i;
                setObjectField(thisObject, "mVisualOverflowStart", layoutEnd - getIntField(thisObject, "mIconSize"));
                if (forceOverflow || getBooleanField(thisObject, "mIsStaticLayout")) {
                    setObjectField(thisObject, "mVisualOverflowStart", Math.min(translationX, getFloatField(thisObject, "mVisualOverflowStart")));
                }
            }
            translationX += getFloatField(iconState, "iconAppearAmount") * view.getWidth() * drawingScale;
        }
        setObjectField(thisObject, "mNumDots", 0);
        if (firstOverflowIndex != -1) {
            translationX = getFloatField(thisObject, "mVisualOverflowStart");
            for (int i = firstOverflowIndex; i < childCount; i++) {
                View view = thisObject.getChildAt(i);
                Object iconState = callMethod(mIconStates, "get", view);
                int dotWidth = getIntField(thisObject, "mStaticDotDiameter") + getIntField(thisObject, "mDotPadding");
                setObjectField(iconState, "xTranslation", translationX);
                if (getIntField(thisObject, "mNumDots") < MAX_DOTS) {
                    if (getIntField(thisObject, "mNumDots") == 0 && getFloatField(iconState, "iconAppearAmount") < 0.8f) {
                        setObjectField(iconState, "visibleState", STATE_ICON);
                    } else {
                        setObjectField(iconState, "visibleState", STATE_DOT);
                        setObjectField(thisObject, "mNumDots", getIntField(thisObject, "mNumDots") + 1);
                    }
                    translationX += (getIntField(thisObject, "mNumDots") == MAX_DOTS ? MAX_DOTS * dotWidth : dotWidth)
                            * getFloatField(iconState, "iconAppearAmount");
                    try{
                        setObjectField(thisObject, "mLastVisibleIconState", iconState);
                    }catch (Throwable ignored){} //unfortunately classloader can't locate this field
                } else {
                    setObjectField(iconState, "visibleState", STATE_HIDDEN);
                }
            }
        } else if (childCount > 0) {
            View lastChild = thisObject.getChildAt(childCount - 1);
            try {
                setObjectField(thisObject, "mLastVisibleIconState", callMethod(mIconStates, "get", lastChild));
            }catch (Throwable ignored){} //unfortunately classloader can't locate this field

            setObjectField(thisObject, "mFirstVisibleIconState", callMethod(mIconStates, "get", thisObject.getChildAt(0)));
        }
        if ((boolean)callMethod(thisObject, "isLayoutRtl")) {
            for (int i = 0; i < childCount; i++) {
                View view = thisObject.getChildAt(i);
                Object iconState = callMethod(mIconStates, "get", view);
                setObjectField(iconState, "xTranslation", thisObject.getWidth() - getIntField(iconState, "xTranslation") - view.getWidth());
            }
        }
        Object mIsolatedIcon = getObjectField(thisObject, "mIsolatedIcon");
        if (mIsolatedIcon != null) {
            Object iconState = callMethod(mIconStates, "get", mIsolatedIcon);
            if (iconState != null) {
                // Most of the time the icon isn't yet added when this is called but only happening
                // later
                setObjectField(iconState, "visibleState", STATE_ICON);
                setObjectField(iconState, "xTranslation", getIntField(getObjectField(thisObject, "mIsolatedIconLocation"),"left") - ((int[])getObjectField(thisObject, "mAbsolutePosition"))[0]
                        - (1 - (int)callMethod(getObjectField(thisObject, "mIsolatedIcon"), "getIconScale")) * (int)callMethod(mIsolatedIcon, "getWidth") / 2.0f);
            }
        }
    }

    //Since classloader can't find these methods, we implement them ourselves
    private static float getLayoutEnd(View v) {
        return getActualWidth(v) - getActualPaddingEnd(v);
    }

    private static int getActualWidth(View v) {
        int mActualLayoutWidth = getIntField(v, "mActualLayoutWidth");
        if (mActualLayoutWidth == NO_VALUE) {
            return v.getWidth();
        }
        return mActualLayoutWidth;
    }

    private static float getActualPaddingEnd(View v) {
        float mActualPaddingEnd = getFloatField(v, "mActualPaddingEnd");
        if (mActualPaddingEnd == NO_VALUE) {
            return v.getPaddingEnd();
        }
        return mActualPaddingEnd;
    }
    
    private static float getIconScaleIncreased(View v)
    {
        return (float) getIntField(v, "mStatusBarIconDrawingSizeIncreased") / getIntField(v, "mStatusBarIconDrawingSize");
    }
}
