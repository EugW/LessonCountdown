package pro.eugw.lessoncountdown.util

import com.google.gson.JsonArray

fun JsonArray.contains(string: String): Boolean {
    forEach {
        if (it.asString == string)
            return true
    }
    return false
}