package pro.eugw.lessoncountdown.util.color

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import pro.eugw.lessoncountdown.R

class ColorPickerDialog : DialogFragment(), OnTouchListener, ColorPickerView.OnColorChangedListener, TextWatcher {

    private var colorPickerDialogListener: ColorPickerDialogListener? = null
    internal var color: Int = 0
    private var dialogId: Int = 99

    // -- CUSTOM ---------------------------
    private var colorPicker: ColorPickerView? = null
    private var newColorPanel: ColorPanelView? = null
    private var hexEditText: EditText? = null
    private var fromEditText: Boolean = false

    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (colorPickerDialogListener == null && context is ColorPickerDialogListener) {
            colorPickerDialogListener = context
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        color = savedInstanceState?.getInt(ARG_COLOR) ?: requireArguments().getInt(ARG_COLOR)

        val rootView = FrameLayout(requireContext())
        rootView.addView(createPickerView())

        val selectedButtonStringRes = getString(R.string.cpv_select)

        val builder = AlertDialog.Builder(requireContext()).setView(rootView)

        builder.setPositiveButton(selectedButtonStringRes) { _, _ -> colorPickerDialogListener!!.onColorSelected(dialogId, color) }

        return builder.create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        val targetFragment = targetFragment

        if (targetFragment is ColorPickerDialogListener) {
            (targetFragment as ColorPickerDialogListener).onDialogDismissed(dialogId)
        }

        if (colorPickerDialogListener != null) {
            colorPickerDialogListener!!.onDialogDismissed(dialogId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ARG_COLOR, color)
        super.onSaveInstanceState(outState)
    }


    fun setColorPickerDialogListener(colorPickerDialogListener: ColorPickerDialogListener) {
        this.colorPickerDialogListener = colorPickerDialogListener
    }

    // region Custom Picker

    private fun createPickerView(): View {
        val contentView = View.inflate(activity, R.layout.cpv_dialog_color_picker, null)
        colorPicker = contentView.findViewById<View>(R.id.cpv_color_picker_view) as ColorPickerView
        val oldColorPanel = contentView.findViewById<View>(R.id.cpv_color_panel_old) as ColorPanelView
        newColorPanel = contentView.findViewById<View>(R.id.cpv_color_panel_new) as ColorPanelView
        val arrowRight = contentView.findViewById<View>(R.id.cpv_arrow_right) as ImageView
        hexEditText = contentView.findViewById<View>(R.id.cpv_hex) as EditText

        try {
            val value = TypedValue()
            val typedArray = requireActivity().obtainStyledAttributes(value.data, intArrayOf(android.R.attr.textColorPrimary))
            val arrowColor = typedArray.getColor(0, Color.BLACK)
            typedArray.recycle()
            arrowRight.setColorFilter(arrowColor)
        } catch (ignored: Exception) {
        }

        oldColorPanel.mColor = requireArguments().getInt(ARG_COLOR)
        colorPicker!!.setColor(color, true)
        newColorPanel!!.mColor = color
        setHex(color)
        hexEditText!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(6))
        newColorPanel!!.setOnClickListener {
            if (newColorPanel!!.mColor == color) {
                colorPickerDialogListener!!.onColorSelected(dialogId, color)
                dismiss()
            }
        }
        contentView.setOnTouchListener(this)
        colorPicker!!.setOnColorChangedListener(this)
        hexEditText!!.addTextChangedListener(this)

        hexEditText!!.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(hexEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        return contentView
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v != hexEditText && hexEditText!!.hasFocus()) {
            hexEditText!!.clearFocus()
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(hexEditText!!.windowToken, 0)
            hexEditText!!.clearFocus()
            return true
        }
        return false
    }

    override fun onColorChanged(newColor: Int) {
        color = newColor
        newColorPanel!!.setColor(newColor)
        if (!fromEditText) {
            setHex(newColor)
            if (hexEditText!!.hasFocus()) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(hexEditText!!.windowToken, 0)
                hexEditText!!.clearFocus()
            }
        }
        fromEditText = false
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable) {
        if (hexEditText!!.isFocused) {
            val color = parseColorString(s.toString())
            if (color != colorPicker!!.color) {
                fromEditText = true
                colorPicker!!.setColor(color, true)
            }
        }
    }

    private fun setHex(color: Int) {
            hexEditText!!.setText(String.format("%06X", 0xFFFFFF and color))
    }

    @Throws(NumberFormatException::class)
    private fun parseColorString(colorHex: String): Int {
        var colorString = colorHex
        val a: Int
        var r: Int
        val g: Int
        var b = 0
        if (colorString.startsWith("#")) {
            colorString = colorString.substring(1)
        }
        when {
            colorString.isEmpty() -> {
                r = 0
                a = 255
                g = 0
            }
            colorString.length <= 2 -> {
                a = 255
                r = 0
                b = Integer.parseInt(colorString, 16)
                g = 0
            }
            colorString.length == 3 -> {
                a = 255
                r = Integer.parseInt(colorString.substring(0, 1), 16)
                g = Integer.parseInt(colorString.substring(1, 2), 16)
                b = Integer.parseInt(colorString.substring(2, 3), 16)
            }
            colorString.length == 4 -> {
                a = 255
                r = Integer.parseInt(colorString.substring(0, 2), 16)
                g = r
                r = 0
                b = Integer.parseInt(colorString.substring(2, 4), 16)
            }
            colorString.length == 5 -> {
                a = 255
                r = Integer.parseInt(colorString.substring(0, 1), 16)
                g = Integer.parseInt(colorString.substring(1, 3), 16)
                b = Integer.parseInt(colorString.substring(3, 5), 16)
            }
            colorString.length == 6 -> {
                a = 255
                r = Integer.parseInt(colorString.substring(0, 2), 16)
                g = Integer.parseInt(colorString.substring(2, 4), 16)
                b = Integer.parseInt(colorString.substring(4, 6), 16)
            }
            colorString.length == 7 -> {
                a = Integer.parseInt(colorString.substring(0, 1), 16)
                r = Integer.parseInt(colorString.substring(1, 3), 16)
                g = Integer.parseInt(colorString.substring(3, 5), 16)
                b = Integer.parseInt(colorString.substring(5, 7), 16)
            }
            colorString.length == 8 -> {
                a = Integer.parseInt(colorString.substring(0, 2), 16)
                r = Integer.parseInt(colorString.substring(2, 4), 16)
                g = Integer.parseInt(colorString.substring(4, 6), 16)
                b = Integer.parseInt(colorString.substring(6, 8), 16)
            }
            else -> {
                b = -1
                g = -1
                r = -1
                a = -1
            }
        }
        return Color.argb(a, r, g, b)
    }

    // -- endregion --

    // region Builder

    class Builder internal constructor() {
        internal var color = Color.TRANSPARENT



        /**
         * Set the original color
         *
         * @param color
         * The default color for the color picker
         * @return This builder object for chaining method calls
         */
        fun setColor(color: Int): Builder {
            this.color = color
            return this
        }

        /**
         * Create the [ColorPickerDialog] instance.
         *
         * @return A new [ColorPickerDialog].
         * @see .show
         */
        fun create(): ColorPickerDialog {
            val dialog = ColorPickerDialog()
            val args = Bundle()
            args.putInt(ARG_COLOR, color)
            dialog.arguments = args
            return dialog
        }

    }

    companion object {

        private const val ARG_COLOR = "color"

        fun newBuilder(): Builder {
            return Builder()
        }
    }

    // endregion

}
