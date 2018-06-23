package pro.eugw.lessoncountdown.util.color

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.Config
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.graphics.Shader.TileMode
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import pro.eugw.lessoncountdown.R


class ColorPickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    /**
     * The width in px of the hue panel.
     */
    private var huePanelWidthPx: Int = 0
    /**
     * The height in px of the alpha panel
     */
    /**
     * The distance in px between the different
     * color panels.
     */
    private var panelSpacingPx: Int = 0
    /**
     * The radius in px of the color palette tracker circle.
     */
    private var circleTrackerRadiusPx: Int = 0
    /**
     * The px which the tracker of the hue or alpha panel
     * will extend outside of its bounds.
     */
    private var sliderTrackerOffsetPx: Int = 0
    /**
     * Height of slider tracker on hue panel,
     * width of slider on alpha panel.
     */
    private var sliderTrackerSizePx: Int = 0

    private var satValPaint: Paint? = null
    private var satValTrackerPaint: Paint? = null

    private var alphaPaint: Paint? = null
    private var alphaTextPaint: Paint? = null
    private var hueAlphaTrackerPaint: Paint? = null

    private var borderPaint: Paint? = null

    private var valShader: Shader? = null
    private var satShader: Shader? = null
    private var alphaShader: Shader? = null


    private var satValBackgroundCache: BitmapCache? = null
    private var hueBackgroundCache: BitmapCache? = null

    private var alpha = 0xff
    private var hue = 360f
    private var sat = 0f
    private var `val` = 0f

    private var alphaSliderText: String? = null
    private var sliderTrackerColor = DEFAULT_SLIDER_COLOR
    private var borderColor = DEFAULT_BORDER_COLOR

    /**
     * Minimum required padding. The offset from the
     * edge we must have or else the finger tracker will
     * get clipped when it's drawn outside of the view.
     */
    private var mRequiredPadding: Int = 0

    /**
     * The Rect in which we are allowed to draw.
     * Trackers can extend outside slightly,
     * due to the required padding we have set.
     */
    private var drawingRect: Rect? = null

    private var satValRect: Rect? = null
    private var hueRect: Rect? = null

    private var startTouchPoint: Point? = null

    private var onColorChangedListener: OnColorChangedListener? = null

    var color: Int
        get() = Color.HSVToColor(alpha, floatArrayOf(hue, sat, `val`))
        set(color) = setColor(color, false)

    init {
        init(context, attrs)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val state = Bundle()
        state.putParcelable("instanceState", super.onSaveInstanceState())
        state.putInt("alpha", alpha)
        state.putFloat("hue", hue)
        state.putFloat("sat", sat)
        state.putFloat("val", `val`)
        state.putString("alpha_text", alphaSliderText)

        return state
    }

    public override fun onRestoreInstanceState(state: Parcelable?) {
        var newState = state

        if (newState is Bundle) {
            val bundle = newState as Bundle?

            alpha = bundle!!.getInt("alpha")
            hue = bundle.getFloat("hue")
            sat = bundle.getFloat("sat")
            `val` = bundle.getFloat("val")
            alphaSliderText = bundle.getString("alpha_text")

            newState = bundle.getParcelable("instanceState")
        }
        super.onRestoreInstanceState(newState)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPickerView)
        alphaSliderText = a.getString(R.styleable.ColorPickerView_cpv_alphaChannelText)
        sliderTrackerColor = a.getColor(R.styleable.ColorPickerView_cpv_sliderColor, -0x424243)
        borderColor = a.getColor(R.styleable.ColorPickerView_cpv_borderColor, -0x919192)
        a.recycle()

        applyThemeColors(context)

        huePanelWidthPx = DrawingUtils.dpToPx(getContext(), HUE_PANEL_WIDTH_DP.toFloat())
        panelSpacingPx = DrawingUtils.dpToPx(getContext(), PANEL_SPACING_DP.toFloat())
        circleTrackerRadiusPx = DrawingUtils.dpToPx(getContext(), CIRCLE_TRACKER_RADIUS_DP.toFloat())
        sliderTrackerSizePx = DrawingUtils.dpToPx(getContext(), SLIDER_TRACKER_SIZE_DP.toFloat())
        sliderTrackerOffsetPx = DrawingUtils.dpToPx(getContext(), SLIDER_TRACKER_OFFSET_DP.toFloat())

        mRequiredPadding = resources.getDimensionPixelSize(R.dimen.cpv_required_padding)

        initPaintTools()

        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun applyThemeColors(c: Context) {

        val value = TypedValue()
        val a = c.obtainStyledAttributes(value.data, intArrayOf(android.R.attr.textColorSecondary))

        if (borderColor == DEFAULT_BORDER_COLOR) {
            borderColor = a.getColor(0, DEFAULT_BORDER_COLOR)
        }

        if (sliderTrackerColor == DEFAULT_SLIDER_COLOR) {
            sliderTrackerColor = a.getColor(0, DEFAULT_SLIDER_COLOR)
        }

        a.recycle()
    }

    private fun initPaintTools() {

        satValPaint = Paint()
        satValTrackerPaint = Paint()
        hueAlphaTrackerPaint = Paint()
        alphaPaint = Paint()
        alphaTextPaint = Paint()
        borderPaint = Paint()

        satValTrackerPaint!!.style = Style.STROKE
        satValTrackerPaint!!.strokeWidth = DrawingUtils.dpToPx(context, 2f).toFloat()
        satValTrackerPaint!!.isAntiAlias = true

        hueAlphaTrackerPaint!!.color = sliderTrackerColor
        hueAlphaTrackerPaint!!.style = Style.STROKE
        hueAlphaTrackerPaint!!.strokeWidth = DrawingUtils.dpToPx(context, 2f).toFloat()
        hueAlphaTrackerPaint!!.isAntiAlias = true

        alphaTextPaint!!.color = -0xe3e3e4
        alphaTextPaint!!.textSize = DrawingUtils.dpToPx(context, 14f).toFloat()
        alphaTextPaint!!.isAntiAlias = true
        alphaTextPaint!!.textAlign = Align.CENTER
        alphaTextPaint!!.isFakeBoldText = true

    }

    override fun onDraw(canvas: Canvas) {
        if (drawingRect!!.width() <= 0 || drawingRect!!.height() <= 0) {
            return
        }

        drawSatValPanel(canvas)
        drawHuePanel(canvas)
    }

    private fun drawSatValPanel(canvas: Canvas) {
        val rect = satValRect

        borderPaint!!.color = borderColor
        canvas.drawRect(drawingRect!!.left.toFloat(), drawingRect!!.top.toFloat(),
                (rect!!.right + BORDER_WIDTH_PX).toFloat(),
                (rect.bottom + BORDER_WIDTH_PX).toFloat(), borderPaint!!)

        if (valShader == null) {
            valShader = LinearGradient(rect.left.toFloat(), rect.top.toFloat(), rect.left.toFloat(), rect.bottom.toFloat(), -0x1, -0x1000000, TileMode.CLAMP)
        }

        if (satValBackgroundCache == null || satValBackgroundCache!!.value != hue) {

            if (satValBackgroundCache == null) {
                satValBackgroundCache = BitmapCache()
            }

            if (satValBackgroundCache!!.bitmap == null) {
                satValBackgroundCache!!.bitmap = Bitmap
                        .createBitmap(rect.width(), rect.height(), Config.ARGB_8888)
            }

            if (satValBackgroundCache!!.canvas == null) {
                satValBackgroundCache!!.canvas = Canvas(satValBackgroundCache!!.bitmap!!)
            }

            val rgb = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

            satShader = LinearGradient(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.top.toFloat(), -0x1, rgb, TileMode.CLAMP)

            val mShader = ComposeShader(
                    valShader!!, satShader!!, PorterDuff.Mode.MULTIPLY)
            satValPaint!!.shader = mShader

            satValBackgroundCache!!.canvas!!.drawRect(0f, 0f,
                    satValBackgroundCache!!.bitmap!!.width.toFloat(),
                    satValBackgroundCache!!.bitmap!!.height.toFloat(),
                    satValPaint!!)

            satValBackgroundCache!!.value = hue

        }

        canvas.drawBitmap(satValBackgroundCache!!.bitmap!!, null, rect, null)

        val p = satValToPoint(sat, `val`)

        satValTrackerPaint!!.color = -0x1000000
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), (circleTrackerRadiusPx - DrawingUtils.dpToPx(context, 1f)).toFloat(), satValTrackerPaint!!)

        satValTrackerPaint!!.color = -0x222223
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), circleTrackerRadiusPx.toFloat(), satValTrackerPaint!!)

    }

    private fun drawHuePanel(canvas: Canvas) {
        val rect = hueRect

        borderPaint!!.color = borderColor

        canvas.drawRect((rect!!.left - BORDER_WIDTH_PX).toFloat(),
                (rect.top - BORDER_WIDTH_PX).toFloat(),
                (rect.right + BORDER_WIDTH_PX).toFloat(),
                (rect.bottom + BORDER_WIDTH_PX).toFloat(),
                borderPaint!!)

        if (hueBackgroundCache == null) {
            hueBackgroundCache = BitmapCache()
            hueBackgroundCache!!.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888)
            hueBackgroundCache!!.canvas = Canvas(hueBackgroundCache!!.bitmap!!)

            val hueColors = IntArray((rect.height() + 0.5f).toInt())

            var h = 360f
            for (i in hueColors.indices) {
                hueColors[i] = Color.HSVToColor(floatArrayOf(h, 1f, 1f))
                h -= 360f / hueColors.size
            }

            val linePaint = Paint()
            linePaint.strokeWidth = 0f
            for (i in hueColors.indices) {
                linePaint.color = hueColors[i]
                hueBackgroundCache!!.canvas!!.drawLine(0f, i.toFloat(), hueBackgroundCache!!.bitmap!!.width.toFloat(), i.toFloat(), linePaint)
            }
        }

        canvas.drawBitmap(hueBackgroundCache!!.bitmap!!, null, rect, null)

        val p = hueToPoint(hue)

        val r = RectF()
        r.left = (rect.left - sliderTrackerOffsetPx).toFloat()
        r.right = (rect.right + sliderTrackerOffsetPx).toFloat()
        r.top = (p.y - sliderTrackerSizePx / 2).toFloat()
        r.bottom = (p.y + sliderTrackerSizePx / 2).toFloat()

        canvas.drawRoundRect(r, 2f, 2f, hueAlphaTrackerPaint!!)
    }

    private fun hueToPoint(hue: Float): Point {

        val rect = hueRect
        val height = rect!!.height().toFloat()

        val p = Point()

        p.y = (height - hue * height / 360f + rect.top).toInt()
        p.x = rect.left

        return p
    }

    private fun satValToPoint(sat: Float, `val`: Float): Point {

        val rect = satValRect
        val height = rect!!.height().toFloat()
        val width = rect.width().toFloat()

        val p = Point()

        p.x = (sat * width + rect.left).toInt()
        p.y = ((1f - `val`) * height + rect.top).toInt()

        return p
    }

    private fun pointToSatVal(x: Float, y: Float): FloatArray {
        var x1 = x
        var y1 = y

        val rect = satValRect
        val result = FloatArray(2)

        val width = rect!!.width().toFloat()
        val height = rect.height().toFloat()

        x1 = when {
            x1 < rect.left -> 0f
            x1 > rect.right -> width
            else -> x1 - rect.left
        }

        y1 = when {
            y1 < rect.top -> 0f
            y1 > rect.bottom -> height
            else -> y1 - rect.top
        }

        result[0] = 1f / width * x1
        result[1] = 1f - 1f / height * y1

        return result
    }

    private fun pointToHue(y: Float): Float {
        var y1 = y

        val rect = hueRect

        val height = rect!!.height().toFloat()

        y1 = when {
            y1 < rect.top -> 0f
            y1 > rect.bottom -> height
            else -> y1 - rect.top
        }

        return 360f - y1 * 360f / height
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var update = false

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                startTouchPoint = Point(event.x.toInt(), event.y.toInt())
                update = moveTrackersIfNeeded(event)
            }
            MotionEvent.ACTION_MOVE -> update = moveTrackersIfNeeded(event)
            MotionEvent.ACTION_UP -> {
                startTouchPoint = null
                update = moveTrackersIfNeeded(event)
            }
        }

        if (update) {
            if (onColorChangedListener != null) {
                onColorChangedListener!!.onColorChanged(Color.HSVToColor(alpha, floatArrayOf(hue, sat, `val`)))
            }
            invalidate()
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun moveTrackersIfNeeded(event: MotionEvent): Boolean {
        if (startTouchPoint == null) {
            return false
        }

        var update = false

        val startX = startTouchPoint!!.x
        val startY = startTouchPoint!!.y

        if (hueRect!!.contains(startX, startY)) {
            hue = pointToHue(event.y)

            update = true
        } else if (satValRect!!.contains(startX, startY)) {
            val result = pointToSatVal(event.x, event.y)

            sat = result[0]
            `val` = result[1]

            update = true
        }
        return update
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val finalWidth: Int
        val finalHeight: Int

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)

        val widthAllowed = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val heightAllowed = View.MeasureSpec.getSize(heightMeasureSpec) - paddingBottom - paddingTop

        if (widthMode == View.MeasureSpec.EXACTLY || heightMode == View.MeasureSpec.EXACTLY) {

            if (widthMode == View.MeasureSpec.EXACTLY && heightMode != View.MeasureSpec.EXACTLY) {
                val h = widthAllowed - panelSpacingPx - huePanelWidthPx


                finalHeight = if (h > heightAllowed) {
                    heightAllowed
                } else {
                    h
                }

                finalWidth = widthAllowed

            } else if (heightMode == View.MeasureSpec.EXACTLY && widthMode != View.MeasureSpec.EXACTLY) {

                val w = heightAllowed + panelSpacingPx + huePanelWidthPx

                finalWidth = if (w > widthAllowed) {
                    widthAllowed
                } else {
                    w
                }

                finalHeight = heightAllowed

            } else {
                finalWidth = widthAllowed
                finalHeight = heightAllowed
            }

        } else {
            val widthNeeded = heightAllowed + panelSpacingPx + huePanelWidthPx

            val heightNeeded = widthAllowed - panelSpacingPx - huePanelWidthPx



            var widthOk = false
            var heightOk = false

            if (widthNeeded <= widthAllowed) {
                widthOk = true
            }

            if (heightNeeded <= heightAllowed) {
                heightOk = true
            }

            if (widthOk && heightOk) {
                finalWidth = widthAllowed
                finalHeight = heightNeeded
            } else if (!heightOk && widthOk) {
                finalHeight = heightAllowed
                finalWidth = widthNeeded
            } else if (!widthOk && heightOk) {
                finalHeight = heightNeeded
                finalWidth = widthAllowed
            } else {
                finalHeight = heightAllowed
                finalWidth = widthAllowed
            }

        }

        setMeasuredDimension(finalWidth + paddingLeft + paddingRight,
                finalHeight + paddingTop + paddingBottom)
    }

    override fun getPaddingTop(): Int {
        return Math.max(super.getPaddingTop(), mRequiredPadding)
    }

    override fun getPaddingBottom(): Int {
        return Math.max(super.getPaddingBottom(), mRequiredPadding)
    }

    override fun getPaddingLeft(): Int {
        return Math.max(super.getPaddingLeft(), mRequiredPadding)
    }

    override fun getPaddingRight(): Int {
        return Math.max(super.getPaddingRight(), mRequiredPadding)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        drawingRect = Rect()
        drawingRect!!.left = paddingLeft
        drawingRect!!.right = w - paddingRight
        drawingRect!!.top = paddingTop
        drawingRect!!.bottom = h - paddingBottom

        valShader = null
        satShader = null
        alphaShader = null

        satValBackgroundCache = null
        hueBackgroundCache = null

        setUpSatValRect()
        setUpHueRect()
    }

    private fun setUpSatValRect() {
        val dRect = drawingRect

        val left = dRect!!.left + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX
        val right = dRect.right - BORDER_WIDTH_PX - panelSpacingPx - huePanelWidthPx


        satValRect = Rect(left, top, right, bottom)
    }

    private fun setUpHueRect() {
        val dRect = drawingRect

        val left = dRect!!.right - huePanelWidthPx + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX
        val right = dRect.right - BORDER_WIDTH_PX

        hueRect = Rect(left, top, right, bottom)
    }


    fun setOnColorChangedListener(listener: OnColorChangedListener) {
        onColorChangedListener = listener
    }

    fun setColor(color: Int, callback: Boolean) {

        val alpha = Color.alpha(color)
        val red = Color.red(color)
        val blue = Color.blue(color)
        val green = Color.green(color)

        val hsv = FloatArray(3)

        Color.RGBToHSV(red, green, blue, hsv)

        this.alpha = alpha
        hue = hsv[0]
        sat = hsv[1]
        `val` = hsv[2]

        if (callback && onColorChangedListener != null) {
            onColorChangedListener!!
                    .onColorChanged(Color.HSVToColor(this.alpha, floatArrayOf(hue, sat, `val`)))
        }

        invalidate()
    }

    private inner class BitmapCache {

        var canvas: Canvas? = null
        var bitmap: Bitmap? = null
        var value: Float = 0.toFloat()
    }

    interface OnColorChangedListener {

        fun onColorChanged(newColor: Int)
    }

    companion object {

        private const val DEFAULT_BORDER_COLOR = -0x919192
        private const val DEFAULT_SLIDER_COLOR = -0x424243
        private const val HUE_PANEL_WIDTH_DP = 30
        private const val PANEL_SPACING_DP = 10
        private const val CIRCLE_TRACKER_RADIUS_DP = 5
        private const val SLIDER_TRACKER_SIZE_DP = 4
        private const val SLIDER_TRACKER_OFFSET_DP = 2
        private const val BORDER_WIDTH_PX = 1

    }

}
