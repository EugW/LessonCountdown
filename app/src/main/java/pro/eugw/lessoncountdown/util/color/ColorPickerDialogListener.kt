package pro.eugw.lessoncountdown.util.color

import androidx.annotation.ColorInt

/**
 * Callback used for getting the selected color from a color picker dialog.
 */
interface ColorPickerDialogListener {

    /**
     * Callback that is invoked when a color is selected from the color picker dialog.
     *
     * @param dialogId
     * The dialog id used to create the dialog instance.
     * @param color
     * The selected color
     */
    fun onColorSelected(dialogId: Int, @ColorInt color: Int)

    //fun onColorLongPressed(dialogId: Int, @ColorInt color: Int)

    /**
     * Callback that is invoked when the color picker dialog was dismissed.
     *
     * @param dialogId
     * The dialog id used to create the dialog instance.
     */
    fun onDialogDismissed(dialogId: Int)

}
