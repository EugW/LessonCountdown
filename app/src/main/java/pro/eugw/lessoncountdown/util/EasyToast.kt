package pro.eugw.lessoncountdown.util

import android.app.Activity
import android.widget.Toast

object EasyToast {

        fun shortShow(resId: Int, activity: Activity) {
            activity.runOnUiThread { Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show() }
        }

        fun shortShow(message: String, activity: Activity) {
            activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
        }

}