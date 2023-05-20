package sh.siava.AOSPMods.modpacks.utils;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("ViewConstructor")
public class FlexStatusIconContainer extends LinearLayout {
	Class<?> StatusIconStateClass;

	public static final int STATE_ICON = 0;
	public static final int STATE_DOT = 1;
	public static final int STATE_HIDDEN = 2;

	public static final int SORT_CLEAN = 0;
	public static final int SORT_TIGHT = 1;

	private static final boolean DEBUG_OVERFLOW = false;
	// Max 8 status icons including battery
	private static final int MAX_ROW_ICONS = 7;

	private int mIconSpacing;
	private int mUnderflowWidth;
	// Individual StatusBarIconViews draw their etc dots centered in this width
	private int mIconDotFrameWidth;
	// So we can count and measure properly
	private final List<View> mMeasureViews = new ArrayList<>();
	// Any ignored icon will never be added as a child
	private final ArrayList<String> mIgnoredSlots = new ArrayList<>();
	private int mIconSize;
	private View mDotIcon = null;
	private static int sortPlan = SORT_CLEAN;
	private int mTagID;
	private final SparseIntArray mColWidths = new SparseIntArray();
	private final List<Integer> mIconWidths = new ArrayList<>();
	private boolean mHasDot = false;
	private int mRowCount;

	private final String stateAlphaField;
	private final String stateXTranslationField;
	private final String stateYTranslationField;

	public static void setSortPlan(int plan) {
		sortPlan = plan;
	}

	public FlexStatusIconContainer(Context context, ClassLoader classLoader) {
		this(context, null, classLoader);
	}

	public FlexStatusIconContainer(Context context, AttributeSet attrs, ClassLoader classLoader) {
		super(context, attrs);

		StatusIconStateClass = findClass("com.android.systemui.statusbar.phone.StatusIconContainer$StatusIconState", classLoader);
		Class<?> ViewStateClass = findClass("com.android.systemui.statusbar.notification.stack.ViewState", classLoader);

		if(findFieldIfExists(ViewStateClass, "mAlpha") == null)
		{ // 13 QPR1
			stateAlphaField = "alpha";
			stateXTranslationField = "xTranslation";
			stateYTranslationField = "yTranslation";
		}
		else
		{ // 13 QPR2
			stateAlphaField = "mAlpha";
			stateXTranslationField = "mXTranslation";
			stateYTranslationField = "mYTranslation";

		}

		initDimens();
		setWillNotDraw(!DEBUG_OVERFLOW);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
	}

	@SuppressWarnings("unused")
	public void setShouldRestrictIcons(boolean should) {
	}

	@SuppressWarnings("unused")
	public boolean isRestrictingIcons() {
		return true;
	}

	@SuppressLint("DiscouragedApi")
	private void initDimens() {
		Resources res = getResources();

		mTagID = res.getIdentifier("status_bar_view_state_tag", "id", getContext().getPackageName());
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
			mColWidths.clear();
			mMeasureViews.clear();
			mIconWidths.clear();
			mDotIcon = null;
			mHasDot = false;

			int mode = MeasureSpec.getMode(widthMeasureSpec);
			int width = MeasureSpec.getSize(widthMeasureSpec);
			int height = MeasureSpec.getSize(heightMeasureSpec);

			if (width == 0) //nothing to render. ignore
			{
				setDefaultResponse(widthMeasureSpec, heightMeasureSpec);
				return;
			}

			int totalIconHeight = mIconSize;
			int mTotalPossibleRows = height / totalIconHeight;

			int paddings = getPaddingLeft() + getPaddingRight();
			int availableWidth = width - paddings;

			int totalWidth = 0;

			switch (sortPlan) {
				case SORT_CLEAN:
					mRowCount = 0;
					mIconWidths.clear();
					int initialCapacity = mTotalPossibleRows * MAX_ROW_ICONS;

					for (int i = getChildCount() - 1; i >= 0; i--) {
						View icon = getChildAt(i);

						boolean isBlocked = false;
						try {
							isBlocked = (boolean) callMethod(icon, "isIconBlocked");
						} catch (Throwable ignored) {
						}

						if ((boolean) callMethod(icon, "isIconVisible") && !isBlocked
								&& !mIgnoredSlots.contains((String) callMethod(icon, "getSlot"))) {   //icon should be considered!

							if (mIconWidths.size() <= initialCapacity) //enough widths recorded
							{
								//measuring child width
								int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED);
								measureChild(icon, childWidthSpec, heightMeasureSpec);
								int iconWidth = getViewTotalMeasuredWidth(icon);

								//adding width to list
								mIconWidths.add(iconWidth);
								mMeasureViews.add(icon);
							} else {
								setChildVisibleState(icon, STATE_HIDDEN);
							}
						} else {
							setChildVisibleState(icon, STATE_HIDDEN);
						}
					}

					int iconsPerRow = Math.min(mIconWidths.size(), MAX_ROW_ICONS);

					//init the loop
					boolean success = false;
					mHasDot = false;

					//reduce iconsperrow until we find a winner
					for (; iconsPerRow > 0 && !success; iconsPerRow--) {
						int iconCapacity = iconsPerRow * mTotalPossibleRows - ((mHasDot) ? 1 : 0);

						if (mIconWidths.size() > iconCapacity) {
							if (!mHasDot) {
								mHasDot = true;
								iconCapacity--;
							}
							mIconWidths.subList(iconCapacity, mIconWidths.size()).clear();
						}
						mColWidths.clear();

						int colIndex = 0;
						totalWidth = (iconsPerRow - 1) * mIconSpacing;

						success = true;

						for (int i = 0; i < mIconWidths.size() + (mHasDot ? 1 : 0); i++, colIndex++) {
							if (colIndex == iconsPerRow) {
								colIndex = 0;
							}
							int currentWidth = i == mIconWidths.size()
									? mIconDotFrameWidth
									: mIconWidths.get(i);

							totalWidth -= mColWidths.get(colIndex, 0);
							mColWidths.put(colIndex, Math.max(currentWidth, mColWidths.get(colIndex, 0)));
							totalWidth += mColWidths.get(colIndex, 0);

							if (totalWidth > availableWidth) {
								success = false;
								iconsPerRow = Math.min(i + 1, iconsPerRow);
								break;
							}
						}
					}

					if (mHasDot) {
						mMeasureViews.subList(mIconWidths.size() + 1, mMeasureViews.size()).forEach(icon -> setChildVisibleState(icon, STATE_HIDDEN));
						setChildVisibleState(mMeasureViews.get(mIconWidths.size()), STATE_DOT);
						mMeasureViews.subList(mIconWidths.size(), mMeasureViews.size()).clear();
					}

					break;
				case SORT_TIGHT:
					mRowCount = 1;
					int availableRows = mTotalPossibleRows - 1;
					int remainingWidth = availableWidth;
					int colIndex = 0;

					for (int i = getChildCount() - 1; i >= 0; i--) {
						View icon = getChildAt(i);

						boolean isBlocked = false;
						try {
							isBlocked = (boolean) callMethod(icon, "isIconBlocked");
						} catch (Throwable ignored) {
						}

						if ((boolean) callMethod(icon, "isIconVisible") && !isBlocked
								&& !mIgnoredSlots.contains((String) callMethod(icon, "getSlot"))) {   //icon should be considered!

							if (mHasDot) {
								setChildVisibleState(icon, STATE_HIDDEN);
								continue;
							}

							int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED);
							measureChild(icon, childWidthSpec, heightMeasureSpec);
							int iconWidth = getViewTotalMeasuredWidth(icon);

							if (iconWidth > remainingWidth || colIndex >= MAX_ROW_ICONS) // row is full
							{
								if (availableRows > 0) { //we still have more rows to fill: prepare for it
									colIndex = 0;
									mRowCount++;
									availableRows--;
									i++;
									totalWidth = Math.max(totalWidth, availableWidth - remainingWidth);
									remainingWidth = availableWidth;
								} else //no more rows available. let's close the case with a dot
								{
									if (mIconDotFrameWidth < remainingWidth || mMeasureViews.isEmpty()) {
										setChildVisibleState(icon, STATE_DOT);
									} else {
										setChildVisibleState(icon, STATE_HIDDEN);

										icon = mMeasureViews.get(mMeasureViews.size() - 1);
										remainingWidth += icon.getMeasuredWidth();
										setChildVisibleState(icon, STATE_DOT);
										mMeasureViews.remove(icon);
									}

									remainingWidth -= mIconDotFrameWidth;
									totalWidth = Math.max(totalWidth, availableWidth - remainingWidth);
								}
								//All lines calculate, to find....... THIS:
							} else {
								remainingWidth -= iconWidth + mIconSpacing;
								mMeasureViews.add(icon);
								totalWidth = Math.max(totalWidth, availableWidth - remainingWidth);
							}
						} else {
							setChildVisibleState(icon, STATE_HIDDEN);
						}
					}
					break;
			}
			totalWidth += paddings;

			if (mode == MeasureSpec.EXACTLY) {
				setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
			} else {
				if (totalWidth > width && mode == MeasureSpec.AT_MOST) {
					totalWidth = width;
				}
				setMeasuredDimension(totalWidth, MeasureSpec.getSize(heightMeasureSpec));
			}
		} catch (Throwable e) {
			log("AOSPMODS Error - Flex Statusbar Container");
			e.printStackTrace();
			setDefaultResponse(widthMeasureSpec, heightMeasureSpec);
		}
	}

	private void setDefaultResponse(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	private void setChildVisibleState(View child, int state) {
		if (state == STATE_DOT) {
			mDotIcon = child;
			mHasDot = true;
		}
		Object childState = getViewStateFromChild(child);
		setObjectField(childState, "visibleState", state);
	}

	@Override
	public void onViewAdded(View child) {
		super.onViewAdded(child);
		try {
			Object vs = StatusIconStateClass.newInstance();
			setObjectField(vs, "justAdded", true);
			child.setTag(mTagID, vs);
		} catch (Throwable ignored) {
		}
	}

	@Override
	public void onViewRemoved(View child) {
		super.onViewRemoved(child);
		child.setTag(mTagID, null);
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
	public void removeIgnoredSlot(String slotName) {
		mIgnoredSlots.remove(slotName);

		requestLayout();
	}

	@SuppressWarnings("unused")
	public void removeIgnoredSlots(List<String> slots) {
		for (String slot : slots) {
			mIgnoredSlots.remove(slot);
		}

		requestLayout();
	}

	@SuppressWarnings("unused")
	public void setIgnoredSlots(List<String> slots) {
		mIgnoredSlots.clear();
		addIgnoredSlots(slots);
	}

	@SuppressWarnings("unused")
	public View getViewForSlot(String slot) {
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			try {
				if (callMethod(child, "getSlot").equals(slot)) {
					return child;
				}
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	/**
	 * Layout is happening from end -> start
	 */
	private void calculateIconTranslations() {
		try {
			int iconCount = mMeasureViews.size();

			float width = getWidth();

			if (width == 0)
				return;

			if (mRowCount == 0) {
				mRowCount = (int) Math.ceil((mIconWidths.size() + (mHasDot ? 1 : 0)) / (double) mColWidths.size());
			}

			final float XEndPoint = width - getPaddingEnd();
			float xPosition = XEndPoint;
			int currentRow = 0;
			float rowTop;

			if (iconCount == 0 /*unlikely!*/) {
				rowTop = (getHeight() - mIconDotFrameWidth) / 2f;
			} else {
                /* keep this formula here
                toplineX = height+paddingtop-paddingbottom-(totalrow*lineheight)+(2*lineheight*x)  /2	-> x=0.....n
                */
				rowTop = (getHeight() - (mRowCount * mIconSize)) / 2f;
			}

			int colIndex = 0;
			for (int i = 0; i < iconCount + (mHasDot ? 1 : 0); i++, colIndex++) {
				View icon = (i < iconCount) ? mMeasureViews.get(i) : mDotIcon;

				int iconWidth = (i < iconCount) ? icon.getWidth() : mIconDotFrameWidth;
				Object childState = getViewStateFromChild(icon);

				float iconTranslationY = 0;
				float iconTranslationX = 0;

				switch (sortPlan) {
					case SORT_CLEAN:
						if (colIndex == mColWidths.size()) {
							colIndex = 0;
							xPosition = XEndPoint;
							currentRow++;
							rowTop = (getHeight() - (mRowCount * mIconSize) + (2 * mIconSize * currentRow)) / 2f;
						}

						if (i < iconCount) {
							setChildVisibleState(icon, STATE_ICON);
						}

						float shift = ((mColWidths.get(colIndex) - iconWidth) / 2f);

						iconTranslationX = xPosition - mColWidths.get(colIndex) + shift;
						xPosition -= mColWidths.get(colIndex) + mIconSpacing;
						iconTranslationY = Math.round(rowTop + (mIconSize - icon.getMeasuredHeight()) / 2f);

						break;
					case SORT_TIGHT:
						if (xPosition >= iconWidth) {
							xPosition -= iconWidth;
							iconTranslationX = xPosition;

							setObjectField(childState, stateXTranslationField, xPosition);
						} else {
							currentRow++;
							i--;
							rowTop = (getHeight() - (mRowCount * mIconSize) + (2 * mIconSize * currentRow)) / 2f;
							xPosition = XEndPoint;
							continue;
						}
						iconTranslationY = Math.round(rowTop + (mIconSize - icon.getMeasuredHeight()) / 2f);

						break;
				}
				if (icon != mDotIcon) {
					setChildVisibleState(icon, STATE_ICON);
				}
				setObjectField(childState, stateXTranslationField, iconTranslationX);
				setObjectField(childState, stateYTranslationField, iconTranslationY);

			}

			//handing data to garbage collector, if applicable. we don't need them anyway
			mMeasureViews.clear();
			mDotIcon = null;
			System.gc();
		} catch (Throwable t) {
			log("AOSPMODS Error - Flex Statusbar Container");
			t.printStackTrace();
		}
	}

	private void applyIconStates() {
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			Object vs = getViewStateFromChild(child);
			if (vs != null) {
				callMethod(vs, "applyToView", child);
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

			callMethod(vs, "initFrom", child);

			setObjectField(vs, stateAlphaField, 1.0f);

			setObjectField(vs, "hidden", false);
		}
	}

	private @Nullable
	Object getViewStateFromChild(View child) {
		return child.getTag(mTagID);
	}

	private static int getViewTotalMeasuredWidth(View child) {
		return child.getMeasuredWidth() + child.getPaddingStart() + child.getPaddingEnd();
	}

	@SuppressWarnings("unused")
	private static int getViewTotalMeasuredHeight(View child) {
		return child.getMeasuredHeight() + child.getPaddingTop() + child.getPaddingBottom();
	}

    /* not used
    private static int getViewTotalWidth(View child) {
        return child.getWidth() + child.getPaddingStart() + child.getPaddingEnd();
    }*/

	@Override
	public boolean hasOverlappingRendering() {
		return false;
	}
}