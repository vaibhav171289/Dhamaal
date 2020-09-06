package com.lotus.dhamaal.utils

import android.app.Activity
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

private const  val TAG = "UTILS"
/** Combination of all flags required to put activity into immersive mode */
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

/**
 * Simulate a button click, including a small delay while it is being pressed to trigger the
 * appropriate animations.
 */
fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}

/** Pad this view with the insets provided by the device cutout (i.e. notch) */
@RequiresApi(Build.VERSION_CODES.P)
fun View.padWithDisplayCutout() {

    /** Helper method that applies padding from cutout's safe insets */
    fun doPadding(cutout: DisplayCutout) = setPadding(
        cutout.safeInsetLeft,
        cutout.safeInsetTop,
        cutout.safeInsetRight,
        cutout.safeInsetBottom
    )

    // Apply padding using the display cutout designated "safe area"
    rootWindowInsets?.displayCutout?.let { doPadding(it) }

    // Set a listener for window insets since view.rootWindowInsets may not be ready yet
    setOnApplyWindowInsetsListener { _, insets ->
        insets.displayCutout?.let { doPadding(it) }
        insets
    }
}

/** Same as [AlertDialog.show] but setting immersive mode in the dialog's window */
fun AlertDialog.showImmersive() {
    // Set the dialog to not focusable
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )

    // Make sure that the dialog's window is in full screen
    window?.decorView?.systemUiVisibility = FLAGS_FULLSCREEN

    // Show the dialog while still in immersive mode
    show()

    // Set the dialog to focusable again
    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
}

/**
 * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as well Also, we don't use sizes
 * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
 *
 * @param choices The list of available sizes
 * @return The video size 1080p,720px
 */
fun chooseVideoSize(choices: Array<Size>): Size {
    for (size in choices) {
        if (1920 == size.width && 1080 == size.height) {
            Log.d(TAG, "Returning size: as ${size.width}x${size.height}")
            return size
        }
    }
    for (size in choices) {
        if (size.width == size.height * 16 / 9 && size.width <= 1080) {
            Log.d(TAG, "Returning size: as ${size.width}x${size.height}")
            return size
        }
    }
    Log.e(TAG, "Couldn't find any suitable video size")
    return choices[choices.size - 1]
}
/**
 * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
 * width and height are at least as large as the respective requested values, and whose aspect
 * ratio matches with the specified value.
 *
 * @param choices     The list of sizes that the camera supports for the intended output class
 * @param width       The minimum desired width
 * @param height      The minimum desired height
 * @param aspectRatio The aspect ratio
 * @return The optimal `Size`, or an arbitrary one if none were big enough
 */
 fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size): Size {
    // Collect the supported resolutions that are at least as big as the preview Surface
    val bigEnough: MutableList<Size> = ArrayList()
    val w = aspectRatio.width
    val h = aspectRatio.height
    for (option in choices) {
        if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
            bigEnough.add(option)
        }
    }
    Log.d(TAG,"Big enough for preview size: ${bigEnough.toList()}")
    // Pick the smallest of those, assuming we found any
    return if (bigEnough.size > 0) {
        Collections.min(bigEnough, CompareSizesByArea())
    } else {
        Log.e(TAG, "Couldn't find any suitable preview size")
        choices[0]
    }
}

/**
 * Compares two `Size`s based on their areas.
 */
internal class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
        // We cast here to ensure the multiplications won't overflow
        return java.lang.Long.signum(
            lhs.width.toLong() * lhs.height -
                    rhs.width.toLong() * rhs.height
        )
    }
}

/**
 * Configures the necessary [Matrix] transformation to `mTextureView`.
 * This method should not to be called until the camera preview size is determined in
 * openCamera, or until the size of `mTextureView` is fixed.
 *
 * @param viewWidth  The width of `mTextureView`
 * @param viewHeight The height of `mTextureView`
 */
fun configureTransform(activity: Activity, mPreviewSize: Size, textureView: AutoFitTextureView ,viewWidth: Int, viewHeight: Int) {
    val rotation = activity.windowManager.defaultDisplay.rotation
    val matrix = Matrix()
    val viewRectF = RectF(0F, 0F, viewWidth.toFloat(), viewHeight.toFloat())
    val bufferRectF = RectF(0F, 0F, mPreviewSize.height.toFloat(), mPreviewSize.width.toFloat())
    val centerX = viewRectF.centerX()
    val centerY = viewRectF.centerY()
    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
        bufferRectF.offset(centerX - bufferRectF.centerX(), centerY - bufferRectF.centerY())
        matrix.setRectToRect(viewRectF, bufferRectF, Matrix.ScaleToFit.FILL)
        val scale: Float =
            Math.max(viewHeight.toFloat() / mPreviewSize.height , viewWidth.toFloat() / mPreviewSize.width)
        matrix.postScale(scale, scale, centerX, centerY)
        matrix.postRotate(90 * (rotation - 2).toFloat(), centerX, centerY)
    }
    textureView.setTransform(matrix)
}
/** Helper class used to pre-compute shortest and longest sides of a [Size] */
class SmartSize(width: Int, height: Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}
/** Standard High Definition size for pictures and video */
val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

/** Returns a [SmartSize] object for the given [Display] */
fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}
/**
 * Returns the largest available PREVIEW size. For more information, see:
 * https://d.android.com/reference/android/hardware/camera2/CameraDevice and
 * https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap
 */
fun <T>getPreviewOutputSize(
    display: Display,
    characteristics: CameraCharacteristics,
    targetClass: Class<T>,
    format: Int? = null
): Size {

    // Find which is smaller: screen or 1080p
    val screenSize = getDisplaySmartSize(display)
    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
    val maxSize = if (hdScreen) SIZE_1080P else screenSize

    // If image format is provided, use it to determine supported sizes; else use target class
    val config = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    if (format == null)
        assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
    else
        assert(config.isOutputSupportedFor(format))
    val allSizes = if (format == null)
        config.getOutputSizes(targetClass) else config.getOutputSizes(format)

    // Get available sizes and sort them by area from largest to smallest
    val validSizes = allSizes
        .sortedWith(compareBy { it.height * it.width })
        .map { SmartSize(it.width, it.height) }.reversed()

    // Then, get the largest output size that is smaller or equal than our max size
    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
}