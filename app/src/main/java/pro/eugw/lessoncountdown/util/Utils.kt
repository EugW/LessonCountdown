package pro.eugw.lessoncountdown.util

import android.app.Activity
import android.widget.Toast
import com.google.gson.JsonArray

fun JsonArray.contains(string: String): Boolean {
    forEach {
        if (it.asString == string)
            return true
    }
    return false
}

fun shortShow(resId: Int, activity: Activity) {
    activity.runOnUiThread { Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show() }
}

fun shortShow(message: String, activity: Activity) {
    activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
}