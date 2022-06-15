package sh.siava.AOSPMods.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

@SuppressLint("ViewConstructor")
@SuppressWarnings("unused")
public class FlexStatusIconContainer extends LinearLayout {
    ClassLoader mClassloader;
    Class<?> StatusIconStateClass;

    public static final int STATE_ICON = 0;
    public static final int STATE_DOT = 1;
    public static final int STATE_HIDDEN = 2;

    public static final int SORT_CLEAN = 0;
    public static final int SORT_TIGHT = 1;


    private static final String TAG = "StatusIconContainer";
    private static final boolean DEBUG_OVERFLOW = false;
    // Max 8 status icons including battery
    private static final int MAX_ROW_ICONS = 7;

    private int mIconSpacing;
    private int mUnderflowWidth;
    // Individual StatusBarIconViews draw their etc dots centered in this width
    private int mIconDotFrameWidth;
    private boolean mShouldRestrictIcons = true;
    // So we can count and measure properly
    private final List<View> mMeasureViews = new ArrayList<>();
    // Any ignored icon will never be added as a child
    private final ArrayList<String> mIgnoredSlots = new ArrayList<>();
    private int mIconSize;
    private final List<Integer> mMaxWidths = new ArrayList<>(), mMaxHeights = new ArrayList<>();
    private int mTotalRows;
    private View mDotIcon = null;
    private static int sortPlan = SORT_CLEAN;

    public static void setSortPlan(int plan)
    {
        sortPlan = plan;
    }

    public FlexStatusIconContainer(Context context, ClassLoader classLoader) {
        this(context, null, classLoader);
    }

    public FlexStatusIconContainer(Context context, AttributeSet attrs, ClassLoader classLoader) {
        super(context, attrs);

        mClassloader = classLoader;
        StatusIconStateClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusIconContainer$StatusIconState", mClassloader);

        initDimens();
        setWillNotDraw(!DEBUG_OVERFLOW);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setShouldRestrictIcons(boolean should) {
        mShouldRestrictIcons = should;
    }

    public boolean isRestrictingIcons() {
        return mShouldRestrictIcons;
    }

    private void initDimens() {
        Resources res = getResources();
        // This is the same value that StatusBarIconView uses
        mIconDotFrameWidth = res.getDimensionPixelSize(
                res.getIdentifier("status_bar_icon_size", "dimen", "android"));
        mIconSize = mIconDotFrameWidth;
        int mDotPadding = getResources().getDimensionPixelSize(
                res.getIdentifier("overflow_icon_dot_padding", "dimen", getContext().getPackageName()));
        mIconSpacing = getResources().getDimensionPixelSize(
                res.getIdentifier("status_bar_system_icon_spacing", "dimen", getContext().getPackageName()));
        int radius = getResources().getDimensionPixelSize(
                res.getIdentifier("overflow_dot_radius", "dimen", getContext().getPackageName()));
        int mStaticDotDiameter = 2 * radius;
        mUnderflowWidth = mIconDotFrameWidth + mStaticDotDiameter + mDotPadding;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        // Layout all child views so that we can move them around later
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            child.layout(0, 0, width, height);
        }

        resetViewStates();
        calculateIconTranslations();
        applyIconStates();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (DEBUG_OVERFLOW) {
            @SuppressLint("DrawAllocation") Paint paint = new Paint();
            paint.setStyle(Style.STROKE);
            paint.setColor(Color.RED);

            // Show bounding box
            canvas.drawRect(getPaddingStart(), 0, getWidth() - getPaddingEnd(), getHeight(), paint);

            // Show etc box
            paint.setColor(Color.GREEN);
            int mUnderflowStart = 0;
            canvas.drawRect(
                    mUnderflowStart, 0, mUnderflowStart + mUnderflowWidth, getHeight(), paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            mDotIcon = null;
            mMeasureViews.clear();
            mMaxWidths.clear();
            mMaxHeights.clear();
            int mode = MeasureSpec.getMode(widthMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);

            final int height = MeasureSpec.getSize(heightMeasureSpec);
            int totalIconHeight = mIconSize;
            int mTotalPossiblRow = height / totalIconHeight;

            int availableRows = mTotalPossiblRow - 1;
            mTotalRows = 1;
            int paddings = getPaddingLeft() + getPaddingRight();
            int totalWidthNeeded = paddings;
            int totalIconCapacity;

            for(int i = getChildCount() - 1; i >= 0; i--)
            {
                View icon = getChildAt(i);

                boolean isBlocked = false;
                try {
                    isBlocked = (boolean)XposedHelpers.callMethod(icon,"isIconBlocked");
                }catch (Throwable ignored){}

                if ((boolean)XposedHelpers.callMethod(icon,"isIconVisible") && !isBlocked
                        && !mIgnoredSlots.contains((String)XposedHelpers.callMethod(icon,"getSlot")))
                {
                    if(mDotIcon != null)
                    {
                        setChildVisibleState(icon, STATE_DOT);
                        continue;
                    }
                    setChildVisibleState(icon, STATE_ICON);

                    int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED);
                    measureChild(icon, childWidthSpec, heightMeasureSpec);

                    int iconWidth = getViewTotalMeasuredWidth(icon);
                    int iconHeight = getViewTotalMeasuredHeight(icon);

                    mMaxWidths.add(mMeasureViews.size() == 0 ?
                            iconWidth
                            : Math.max(
                            mMaxWidths.get(mMeasureViews.size()-1),
                            iconWidth));

                    mMaxHeights.add(mMeasureViews.size() == 0 ?
                            iconHeight
                            : Math.max(
                            mMaxHeights.get(mMeasureViews.size()-1),
                            iconHeight));

                    mMeasureViews.add(icon);

                    int iconsPerRow = MAX_ROW_ICONS;

                    if(mTotalRows == 1 || sortPlan == SORT_TIGHT) {
                        totalWidthNeeded += iconWidth + mIconSpacing;
                    }
                    else
                    {
                        boolean reCheck;
                        do {
                            reCheck = false;
                            iconsPerRow = Math.floorDiv(width - paddings, mMaxWidths.get(mMeasureViews.size()-1)+mIconSpacing);
                            totalWidthNeeded = mMaxWidths.get(mMeasureViews.size()-1) * iconsPerRow;
                            totalIconCapacity = iconsPerRow * mTotalPossiblRow - (mDotIcon == null ? 0 : 1);

                            if(mMeasureViews.size() > totalIconCapacity) {
                                setChildVisibleState(mMeasureViews.remove(mMeasureViews.size() - 1), STATE_DOT);
                                reCheck = true;
                            }
                        }while(mMeasureViews.size() > totalIconCapacity || reCheck);
                    }

                    while((totalWidthNeeded-mIconSpacing) > width || mMeasureViews.size() > (iconsPerRow * mTotalRows))
                    {
                        if (availableRows > 0)
                        {
                            mMeasureViews.remove(icon);
                            i++;
                            mTotalRows++;
                            availableRows--;
                            totalWidthNeeded = paddings;
                            break;
                        }
                        else
                        {
                            totalWidthNeeded += mUnderflowWidth; //adding required space for dot
                            while((totalWidthNeeded-mIconSpacing) > width)
                            {
                                iconWidth = getViewTotalMeasuredWidth(mMeasureViews.get(mMeasureViews.size()-1));
                                setChildVisibleState(mMeasureViews.remove(mMeasureViews.size()-1), STATE_DOT);
                                totalWidthNeeded -= (iconWidth + mIconSpacing);
                            }
                        }
                    }
                }
                else
                {
                    setChildVisibleState(icon, STATE_HIDDEN);
                }
            }

            if (mode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
            } else {
                if(totalWidthNeeded > width && mode == MeasureSpec.AT_MOST)
                {
                    totalWidthNeeded = width;
                }
                setMeasuredDimension(totalWidthNeeded, MeasureSpec.getSize(heightMeasureSpec));
            }
        }catch (Throwable e){
            XposedBridge.log("AOSPMODS Error");
            e.printStackTrace();
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        }
    }

    private void setChildVisibleState(View child, int state) {
        if (state == STATE_DOT)
        {
            if(mDotIcon != null)
            {
                setChildVisibleState(mDotIcon, STATE_HIDDEN);
            }
            mDotIcon = child;
        }
        Object childState = getViewStateFromChild(child);
        XposedHelpers.setObjectField(childState, "visibleState", state);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        try {
            Object vs = StatusIconStateClass.newInstance();
            XposedHelpers.setObjectField(vs, "justAdded", true);
            child.setTag(
                    getResources().getIdentifier("status_bar_view_state_tag", "id", getContext().getPackageName()), vs);
        }catch (Throwable ignored){}
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        child.setTag(
                getResources().getIdentifier("status_bar_view_state_tag", "id", getContext().getPackageName()), null);
    }

    public void addIgnoredSlot(String slotName) {
        addIgnoredSlotInternal(slotName);
        requestLayout();
    }

    public void addIgnoredSlots(List<String> slots) {
        for (String slot : slots) {
            addIgnoredSlotInternal(slot);
        }
        requestLayout();
    }

    private void addIgnoredSlotInternal(String slotName) {
        if (!mIgnoredSlots.contains(slotName)) {
            mIgnoredSlots.add(slotName);
        }
    }

    public void removeIgnoredSlot(String slotName) {
        mIgnoredSlots.remove(slotName);

        requestLayout();
    }

    public void removeIgnoredSlots(List<String> slots) {
        for (String slot : slots) {
            mIgnoredSlots.remove(slot);
        }

        requestLayout();
    }

    public void setIgnoredSlots(List<String> slots) {
        mIgnoredSlots.clear();
        addIgnoredSlots(slots);
    }

    public View getViewForSlot(String slot) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            try {
                if (((String)(XposedHelpers.callMethod(child, "getSlot"))).equals(slot)) {
                    return child;
                }
            }catch(Throwable ignored){}
        }
        return null;
    }

    /**
    * Layout is happening from end -> start
    */
    private void calculateIconTranslations() {
        XposedBridge.log("total row " + mTotalRows);
        XposedBridge.log("underflow " + (mDotIcon != null));

        try {
            XposedBridge.log("total icons " + mMeasureViews.size());

            int iconCount = mMeasureViews.size();

            float width = getWidth();

            int iconFixedWidth = 0;
            if (mTotalRows > 1 && sortPlan == SORT_CLEAN) {
                iconFixedWidth = mMaxWidths.get(iconCount - 1);
            }

            final float XEndPoint = width - getPaddingEnd();
            float translationX = XEndPoint;

            int currentRow = 0;
//        toplineX = hight+paddingtop-paddingbottom-(totalrow*lineheight)+(2*lineheight*x)  /2	-> x=0.....n

            float rowTop;
            if (mMeasureViews.size() == 0 /*unlikely!*/) {
                rowTop = (getHeight() - mIconDotFrameWidth) / 2f;
            } else {
                rowTop = (getHeight() - (mTotalRows * mMaxHeights.get(iconCount - 1)) + (2 * mMaxHeights.get(iconCount - 1) * currentRow)) / 2f;
            }

            for (int i = 0; i <= iconCount; i++) {
                View icon = (i == iconCount) ? mDotIcon : mMeasureViews.get(i);
                Object childState = getViewStateFromChild(icon);

                int iconWidth = (icon == mDotIcon) ? mIconDotFrameWidth : getViewTotalMeasuredWidth(icon);

                if (mTotalRows > 1) {
                    translationX -= (sortPlan == SORT_CLEAN) ? iconFixedWidth : iconWidth;

                    if (translationX < 0) {
                        currentRow++;
                        translationX = XEndPoint;
                        i--;
                        rowTop = (getHeight() + getPaddingTop() - getPaddingEnd() - (mTotalRows * mMaxHeights.get(iconCount - 1)) + (2 * mMaxHeights.get(iconCount - 1) * currentRow)) / 2f;
                        continue;
                    }

                    float iconTranslationX =  sortPlan == SORT_CLEAN ?
                            translationX + ((iconFixedWidth - iconWidth) / 2f) :
                            translationX;

                    //icontop = topXline + (lineheight-iconheight)/2
                    int iconTranslationY = Math.round(rowTop + (mMaxHeights.get(iconCount - 1) - icon.getMeasuredHeight()) / 2f);

                    XposedHelpers.setObjectField(childState, "xTranslation", iconTranslationX);
                    XposedHelpers.setObjectField(childState, "yTranslation", iconTranslationY);
                } else {
                    translationX -= iconWidth;
                    XposedHelpers.setObjectField(childState, "xTranslation", translationX);
                    XposedHelpers.setObjectField(childState, "yTranslation", rowTop);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("AOSPMods Error");
            t.printStackTrace();
        }
    }

    private void applyIconStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Object vs = getViewStateFromChild(child);
            if (vs != null) {
                XposedHelpers.callMethod(vs, "applyToView", child);
            }
        }
    }

    private void resetViewStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Object vs = getViewStateFromChild(child);
            if (vs == null) {
                continue;
            }

            XposedHelpers.callMethod(vs, "initFrom",child);
            XposedHelpers.setObjectField(vs, "alpha", 1.0f);
            XposedHelpers.setObjectField(vs, "hidden", false);
        }
    }

    private @Nullable Object getViewStateFromChild(View child) {
        return child.getTag(
                getResources().getIdentifier("status_bar_view_state_tag", "id", getContext().getPackageName()));
    }

    private static int getViewTotalMeasuredWidth(View child) {
        return child.getMeasuredWidth() + child.getPaddingStart() + child.getPaddingEnd();
    }

    private static int getViewTotalMeasuredHeight(View child) {
        return child.getMeasuredHeight() + child.getPaddingTop() + child.getPaddingBottom();
    }

    private static int getViewTotalWidth(View child) {
        return child.getWidth() + child.getPaddingStart() + child.getPaddingEnd();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}