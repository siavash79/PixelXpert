/*
* Original package: dev.eren.removebg
* This file is copied from that repository, because I needed to reposition the model path
*/


package sh.siava.pixelxpert.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils


/**
 * Created by erenalpaslan on 18.08.2023
 */
class PyTorchBackgroundRemover(context: Context, modelPath: String) {

    private var module: Module = LiteModuleLoader.load(String.format("%s/%s", context.cacheDir.absolutePath, modelPath))
    private val maskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val size = 320

    init {
        maskPaint.isAntiAlias = true
        maskPaint.style = Paint.Style.FILL
        maskPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))
    }

    private fun getMaskedImage(input: Bitmap, mask: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(result)

        mCanvas.drawBitmap(input, 0f, 0f, null)
        mCanvas.drawBitmap(mask, 0f, 0f, maskPaint)
        return result
    }

    fun removeBackground(input: Bitmap): Bitmap? {
        val mutable = input.copy(Bitmap.Config.ARGB_8888, true)

        val width = mutable.width
        val height = mutable.height

        val scaledBitmap = Bitmap.createScaledBitmap(mutable, size, size, true)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            scaledBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        val outputTensor = module.forward(IValue.from(inputTensor)).toTuple()
        val arr = outputTensor[0].toTensor().dataAsFloatArray
        val scaledMask = convertArrayToBitmap(arr, size, size)?.let {
            Bitmap.createScaledBitmap(it, width, height, true)
        }
        return scaledMask?.let { getMaskedImage(mutable, it) }
    }

    private fun convertArrayToBitmap(arr: FloatArray, width: Int, height: Int): Bitmap? {
        val grayToneImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (i in 0 until width) {
            for (j in 0 until height) {
                grayToneImage.setPixel(j, i, (arr[i * height + j] * 255f).toInt() shl 24)
            }
        }
        return grayToneImage
    }
}