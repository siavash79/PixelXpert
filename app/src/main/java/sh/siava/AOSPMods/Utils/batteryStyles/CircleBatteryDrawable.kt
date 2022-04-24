/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.siava.AOSPMods.Utils.batteryStyles

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.min

class CircleBatteryDrawable(
    private val context: Context,
    frameColor: Int
) : BatteryDrawable() {
    private var fastChargeColor: Int = Color.WHITE
    private val criticalLevel: Int
    private val warningString: String
    private val framePaint: Paint
    private val batteryPaint: Paint
    private val warningTextPaint: Paint
    private val textPaint: Paint
    private val boltPaint: Paint
    private val plusPaint: Paint
    private val powerSavePaint: Paint
    private val boltPoints: FloatArray
    private val boltPath = Path()
    private val padding = Rect()
    private val frame = RectF()
    private val boltFrame = RectF()
    private val pathEffect = DashPathEffect(floatArrayOf(3f,2f),0f)
    private val batteryLevels : FloatArray = FloatArray(0)
    private val batteryColors : IntArray = IntArray(0)
    private val indicateCharging : Boolean = false
    private val indicateFastCharging : Boolean = false

    private var chargeColor: Int = Color.WHITE
    private var iconTint = Color.WHITE
    private var intrinsicWidth: Int
    private var intrinsicHeight: Int
    private var height = 0
    private var width = 0

    private var BATTERY_STYLE_CIRCLE = 1
    private var BATTERY_STYLE_DOTTED_CIRCLE = 2

    // Dual tone implies that battery level is a clipped overlay over top of the whole shape
    private var dualTone = false

    override fun getIntrinsicHeight() = intrinsicHeight

    override fun getIntrinsicWidth() = intrinsicWidth

    private var fastCharging: Boolean = false

    override fun setFastCharging(isFastCharging: Boolean) {
        this.fastCharging = isFastCharging
        if(!isFastCharging)
        {
            charging = false
        }
        postInvalidate()
    }

    private var charging: Boolean = false

    override fun setCharging(isCharging : Boolean)
    {
        charging = isCharging
        if(!charging) fastCharging = false
        postInvalidate()
    }


    private var powerSaving: Boolean = false

    override fun setPowerSaveEnabled(isPowerSaving: Boolean) {
        powerSaving = isPowerSaving
        postInvalidate()
    }

    override fun refresh() {
        invalidateSelf()
    }


    private var showPercentage: Boolean = false

    override fun setShowPercent(showPercent: Boolean) {
        showPercentage = showPercent
        invalidateSelf()
    }

    private var batteryLevel: Int = -1

    override fun setBatteryLevel(mLevel: Int) {
        batteryLevel = mLevel
        postInvalidate()
    }

    private var bMeterStyle: Int = BATTERY_STYLE_CIRCLE

    override fun setMeterStyle(batteryStyle: Int) {
        bMeterStyle = batteryStyle
        postInvalidate()
    }

    // an approximation of View.postInvalidate()
    private fun postInvalidate() {
        unscheduleSelf { invalidateSelf() }
        scheduleSelf({ invalidateSelf() }, 0)
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        updateSize()
    }

    private fun updateSize() {
        val res = context.resources
        height = bounds.bottom - padding.bottom - (bounds.top + padding.top)
        width = bounds.right - padding.right - (bounds.left + padding.left)
        warningTextPaint.textSize = height * 0.75f
        intrinsicHeight = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", context.packageName))// res.getDimensionPixelSize(R.dimen.battery_height)
        intrinsicWidth = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", context.packageName))//res.getDimensionPixelSize(R.dimen.battery_height)
    }

    override fun getPadding(padding: Rect): Boolean {
        if (this.padding.left == 0 &&
                this.padding.top == 0 &&
                this.padding.right == 0 &&
                this.padding.bottom == 0
        ) {
            return super.getPadding(padding)
        }
        padding.set(this.padding)
        return true
    }

    private fun getColorForLevel(percent: Int): Int {
        for(i in 0 until batteryLevels.size)
        {
            if(percent < batteryLevels[i])
            {
                return batteryColors[i]
            }
        }
        return iconTint
    }

    private fun batteryColorForLevel(level: Int) =
            if (fastCharging && indicateFastCharging)
                    fastChargeColor
            else if (charging && indicateCharging)
                    chargeColor
            else
                getColorForLevel(level)

    override fun setColors(fgColor: Int, bgColor: Int, singleToneColor: Int) {
        val fillColor = if (dualTone) fgColor else singleToneColor

        iconTint = fillColor
        framePaint.color = bgColor
//<Sia: Fixed visibility issues
        framePaint.alpha = 80
//>
        invalidateSelf()
    }

    override fun draw(c: Canvas) {
        if (batteryLevel == -1) return
        val circleSize = min(width, height)
        val strokeWidth = circleSize / 6.5f
        framePaint.strokeWidth = strokeWidth
        framePaint.style = Paint.Style.STROKE
        batteryPaint.strokeWidth = strokeWidth
        batteryPaint.style = Paint.Style.STROKE
        if (bMeterStyle == BATTERY_STYLE_DOTTED_CIRCLE) {
            batteryPaint.pathEffect = pathEffect
            powerSavePaint.pathEffect = pathEffect
        } else {
            batteryPaint.pathEffect = null
            powerSavePaint.pathEffect = null
        }
        powerSavePaint.strokeWidth = strokeWidth
        frame[
                strokeWidth / 2.0f + padding.left, strokeWidth / 2.0f,
                circleSize - strokeWidth / 2.0f + padding.left
        ] = circleSize - strokeWidth / 2.0f
        // set the battery charging color
        batteryPaint.color = batteryColorForLevel(batteryLevel)
        batteryPaint.alpha = alpha;
        boltPaint.color = batteryPaint.color

        if (charging) { // define the bolt shape
            val bl = frame.left + frame.width() / 3.0f
            val bt = frame.top + frame.height() / 3.4f
            val br = frame.right - frame.width() / 4.0f
            val bb = frame.bottom - frame.height() / 5.6f
            if (boltFrame.left != bl ||
                    boltFrame.top != bt ||
                    boltFrame.right != br ||
                    boltFrame.bottom != bb
            ) {
                boltFrame[bl, bt, br] = bb
                boltPath.reset()
                boltPath.moveTo(
                        boltFrame.left + boltPoints[0] * boltFrame.width(),
                        boltFrame.top + boltPoints[1] * boltFrame.height()
                )
                var i = 2
                while (i < boltPoints.size) {
                    boltPath.lineTo(
                            boltFrame.left + boltPoints[i] * boltFrame.width(),
                            boltFrame.top + boltPoints[i + 1] * boltFrame.height()
                    )
                    i += 2
                }
                boltPath.lineTo(
                        boltFrame.left + boltPoints[0] * boltFrame.width(),
                        boltFrame.top + boltPoints[1] * boltFrame.height()
                )
            }
            c.drawPath(boltPath, boltPaint)
        }
        // draw thin gray ring first
        c.drawArc(frame, 270f, 360f, false, framePaint)
        // draw colored arc representing charge level
        if (batteryLevel > 0) {
            if (!charging && powerSaving) {
                c.drawArc(frame, 270f, 3.6f * batteryLevel, false, powerSavePaint)
            } else {
                c.drawArc(frame, 270f, 3.6f * batteryLevel, false, batteryPaint)
            }
        }
        // compute percentage text
        if (!charging && batteryLevel != 100 && showPercentage) {
            textPaint.color = getColorForLevel(batteryLevel)
            textPaint.textSize = height * 0.52f
            val textHeight = -textPaint.fontMetrics.ascent
            val pctText =
                    if (batteryLevel > criticalLevel)
                        batteryLevel.toString()
                    else
                        warningString
            val pctX = width * 0.5f
            val pctY = (height + textHeight) * 0.47f
            c.drawText(pctText, pctX, pctY, textPaint)
        }
    }

    // Some stuff required by Drawable.
    override fun setAlpha(alpha: Int) {
        boltPaint.alpha = alpha
        framePaint.alpha = if (alpha > 20) (alpha - 20) else alpha
        postInvalidate()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        framePaint.colorFilter = colorFilter
        batteryPaint.colorFilter = colorFilter
        warningTextPaint.colorFilter = colorFilter
        boltPaint.colorFilter = colorFilter
        plusPaint.colorFilter = colorFilter
    }

    override fun getOpacity() = PixelFormat.UNKNOWN

    companion object {
        private fun loadPoints(
                res: Resources,
                pointArrayRes: Int
        ): FloatArray {
            val pts = res.getIntArray(pointArrayRes)
            var maxX = 0
            var maxY = 0
            run {
                var i = 0
                while (i < pts.size) {
                    maxX = max(maxX, pts[i])
                    maxY = max(maxY, pts[i + 1])
                    i += 2
                }
            }
            val ptsF = FloatArray(pts.size)
            var i = 0
            while (i < pts.size) {
                ptsF[i] = pts[i].toFloat() / maxX
                ptsF[i + 1] = pts[i + 1].toFloat() / maxY
                i += 2
            }
            return ptsF
        }
    }

    init {
        val res = context.resources

//        this.batteryLevels = batteryLevels
//        this.batteryColors = batteryColors
//        this.indicateCharging = indicateCharging
//        this.indicateFastCharging = indicateFastCharging

        warningString = "!"//res.getString(R.string.battery_meter_very_low_overlay_symbol)
        criticalLevel = 5 /*res.getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel
        )*/
        framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        framePaint.color = frameColor
        framePaint.isDither = true
        batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        batteryPaint.isDither = true
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        warningTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        warningTextPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        warningTextPaint.textAlign = Paint.Align.CENTER


//        this.chargeColor = chargeColor
//        this.fastChargeColor = fastChargeColor
        boltPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        boltPaint.color = chargeColor
        boltPoints =
                loadPoints(res, res.getIdentifier("batterymeter_bolt_points", "array", context.packageName))// R.array.batterymeter_bolt_points)
        plusPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        plusPaint.color = getColorStateListDefaultColor(
                context,
                res.getIdentifier("batterymeter_plus_color", "color", context.packageName)//R.color.batterymeter_plus_color
        )
        powerSavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        powerSavePaint.color = plusPaint.color
        powerSavePaint.style = Paint.Style.STROKE
        intrinsicWidth = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", context.packageName))//R.dimen.battery_width)
        intrinsicHeight = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", context.packageName))//R.dimen.battery_height)

        dualTone = false//res.getBoolean(com.android.internal.R.bool.config_batterymeterDualTone)
    }

    @ColorInt
    private fun getColorStateListDefaultColor(context: Context, resId: Int): Int {
        val list = context.resources.getColorStateList(resId, context.theme)
        return list.defaultColor
    }

}