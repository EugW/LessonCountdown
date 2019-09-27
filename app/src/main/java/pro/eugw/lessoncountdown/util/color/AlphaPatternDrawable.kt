package pro.eugw.lessoncountdown.util.color

import android.graphics.*
import android.graphics.Bitmap.Config
import android.graphics.drawable.Drawable
import kotlin.math.ceil

/**
 * This drawable will draw a simple white and gray chessboard pattern.
 * It's the pattern you will often see as a background behind a partly transparent image in many applications.
 */
internal class AlphaPatternDrawable(rectangleSize: Int) : Drawable() {

    private var rectangleSize = 10

    private val paint = Paint()
    private val paintWhite = Paint()
    private val paintGray = Paint()

    private var numRectanglesHorizontal: Int = 0
    private var numRectanglesVertical: Int = 0

    /**
     * Bitmap in which the pattern will be cached.
     * This is so the pattern will not have to be recreated each time draw() gets called.
     * Because recreating the pattern i rather expensive. I will only be recreated if the size changes.
     */
    private var bitmap: Bitmap? = null

    init {
        this.rectangleSize = rectangleSize
        paintWhite.color = -0x1
        paintGray.color = -0x343435
    }

    override fun draw(canvas: Canvas) {
        if (bitmap != null && !bitmap!!.isRecycled) {
            canvas.drawBitmap(bitmap!!, null, bounds, paint)
        }
    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }

    override fun setAlpha(alpha: Int) {
        throw UnsupportedOperationException("Alpha is not supported by this drawable.")
    }

    override fun setColorFilter(cf: ColorFilter?) {
        throw UnsupportedOperationException("ColorFilter is not supported by this drawable.")
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val height = bounds.height()
        val width = bounds.width()
        numRectanglesHorizontal = ceil((width / rectangleSize).toDouble()).toInt()
        numRectanglesVertical = ceil((height / rectangleSize).toDouble()).toInt()
        generatePatternBitmap()
    }

    private fun generatePatternBitmap() {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return
        }

        bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Config.ARGB_8888)
        val canvas = Canvas(bitmap!!)

        val r = Rect()
        var verticalStartWhite = true
        for (i in 0..numRectanglesVertical) {
            var isWhite = verticalStartWhite
            for (j in 0..numRectanglesHorizontal) {
                r.top = i * rectangleSize
                r.left = j * rectangleSize
                r.bottom = r.top + rectangleSize
                r.right = r.left + rectangleSize
                canvas.drawRect(r, if (isWhite) paintWhite else paintGray)
                isWhite = !isWhite
            }
            verticalStartWhite = !verticalStartWhite
        }
    }

}
