package pro.eugw.lessoncountdown.util.color

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import pro.eugw.lessoncountdown.R

/**
 * This class draws a panel which which will be filled with a mColor which can be set. It can be used to show the
 * currently selected mColor which you will get from the [ColorPickerView].
 */
class ColorPanelView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private var alphaPattern: Drawable? = null
    private var borderPaint: Paint? = null
    private var colorPaint: Paint? = null
    private var alphaPaint: Paint? = null
    private var originalPaint: Paint? = null
    private var drawingRect: Rect? = null
    private var colorRect: Rect? = null
    private var centerRect = RectF()
    private var showOldColor: Boolean = false

    /* The width in pixels of the border surrounding the mColor panel. */
    private var borderWidthPx: Int = 0
    private var mBorderColor = DEFAULT_BORDER_COLOR
    private var borderBlended: Boolean = false
    var mColor = Color.BLACK
    private var shape: Int = 0

    init {
        init(context, attrs)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val state = Bundle()
        state.putParcelable("instanceState", super.onSaveInstanceState())
        state.putInt("color", mColor)
        return state
    }

    public override fun onRestoreInstanceState(state: Parcelable?) {
        var newState = state
        if (state is Bundle) {
            val bundle = state as Bundle?
            mColor = bundle!!.getInt("color")
            newState = bundle.getParcelable("instanceState")
        }
        super.onRestoreInstanceState(newState)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPanelView)
        shape = a.getInt(R.styleable.ColorPanelView_cpv_colorShape, ColorShape.CIRCLE)
        showOldColor = a.getBoolean(R.styleable.ColorPanelView_cpv_showOldColor, false)
        if (showOldColor && shape != ColorShape.CIRCLE) {
            throw IllegalStateException("Color preview is only available in circle mode")
        }
        mBorderColor = a.getColor(R.styleable.ColorPanelView_cpv_borderColor, DEFAULT_BORDER_COLOR)
        a.recycle()
        if (mBorderColor == DEFAULT_BORDER_COLOR) {
            // If no specific border mColor has been set we take the default secondary text mColor as border/slider mColor.
            // Thus it will adopt to theme changes automatically.
            val value = TypedValue()
            val typedArray = context.obtainStyledAttributes(value.data, intArrayOf(android.R.attr.textColorSecondary))
            mBorderColor = typedArray.getColor(0, mBorderColor)
            typedArray.recycle()
        }
        borderWidthPx = DrawingUtils.dpToPx(context, 1f)
        borderPaint = Paint()
        borderPaint!!.isAntiAlias = true
        colorPaint = Paint()
        colorPaint!!.isAntiAlias = true
        if (showOldColor) {
            originalPaint = Paint()
        }
        if (shape == ColorShape.CIRCLE) {
            val bitmap = ((resources.getDrawable(R.drawable.cpv_alpha, resources.newTheme())) as BitmapDrawable).bitmap
            alphaPaint = Paint()
            alphaPaint!!.isAntiAlias = true
            val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            alphaPaint!!.shader = shader
        }
    }

    override fun onDraw(canvas: Canvas) {
        borderPaint!!.color = mBorderColor
        colorPaint!!.color = mColor
        if (shape == ColorShape.SQUARE) {
            if (borderWidthPx > 0) {
                canvas.drawRect(drawingRect!!, borderPaint!!)
            }
            if (alphaPattern != null) {
                alphaPattern!!.draw(canvas)
            }
            canvas.drawRect(colorRect!!, colorPaint!!)
        } else if (shape == ColorShape.CIRCLE) {
            val outerRadius = measuredWidth / 2
            if (borderWidthPx > 0) {
                if (this.borderBlended) {
                    canvas.drawCircle((measuredWidth / 2).toFloat(),
                            (measuredHeight / 2).toFloat(),
                            outerRadius - borderWidthPx / 2.0f,
                            colorPaint!!)
                }

                canvas.drawCircle((measuredWidth / 2).toFloat(),
                        (measuredHeight / 2).toFloat(),
                        outerRadius.toFloat(),
                        borderPaint!!)
            }
            if (Color.alpha(mColor) < 255) {
                canvas.drawCircle((measuredWidth / 2).toFloat(),
                        (measuredHeight / 2).toFloat(),
                        (outerRadius - borderWidthPx).toFloat(), alphaPaint!!)
            }
            if (showOldColor) {
                canvas.drawArc(centerRect, 90f, 180f, true, originalPaint!!)
                canvas.drawArc(centerRect, 270f, 180f, true, colorPaint!!)
            } else {
                canvas.drawCircle((measuredWidth / 2).toFloat(),
                        (measuredHeight / 2).toFloat(),
                        (outerRadius - borderWidthPx).toFloat(),
                        colorPaint!!)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        when (shape) {
            ColorShape.SQUARE -> {
                val width = View.MeasureSpec.getSize(widthMeasureSpec)
                val height = View.MeasureSpec.getSize(heightMeasureSpec)
                setMeasuredDimension(width, height)
            }
            ColorShape.CIRCLE -> {
                super.onMeasure(widthMeasureSpec, widthMeasureSpec)
                setMeasuredDimension(measuredWidth, measuredWidth)
            }
            else -> super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (shape == ColorShape.SQUARE || showOldColor) {
            drawingRect = Rect()
            drawingRect!!.left = paddingLeft
            drawingRect!!.right = w - paddingRight
            drawingRect!!.top = paddingTop
            drawingRect!!.bottom = h - paddingBottom
            if (showOldColor) {
                setUpCenterRect()
            } else {
                setUpColorRect()
            }
        }
    }

    private fun setUpCenterRect() {
        val dRect = drawingRect
        val left = dRect!!.left + borderWidthPx
        val top = dRect.top + borderWidthPx
        val bottom = dRect.bottom - borderWidthPx
        val right = dRect.right - borderWidthPx
        centerRect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    private fun setUpColorRect() {
        val dRect = drawingRect
        val left = dRect!!.left + borderWidthPx
        val top = dRect.top + borderWidthPx
        val bottom = dRect.bottom - borderWidthPx
        val right = dRect.right - borderWidthPx
        colorRect = Rect(left, top, right, bottom)
        alphaPattern = AlphaPatternDrawable(DrawingUtils.dpToPx(context, 4f))
        alphaPattern!!.setBounds(Math.round(colorRect!!.left.toFloat()),
                Math.round(colorRect!!.top.toFloat()),
                Math.round(colorRect!!.right.toFloat()),
                Math.round(colorRect!!.bottom.toFloat()))
    }

    /**
     * Set the mColor that should be shown by this view.
     *
     * @param color
     * the mColor value
     */
    fun setColor(color: Int) {
        this.mColor = color
        invalidate()
    }

    companion object {

        private const val DEFAULT_BORDER_COLOR = -0x919192
    }

}
