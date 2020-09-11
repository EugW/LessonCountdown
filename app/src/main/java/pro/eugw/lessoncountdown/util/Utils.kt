package pro.eugw.lessoncountdown.util

import android.app.Activity
import android.widget.Toast
import com.google.gson.JsonArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

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

fun isEvenWeek(date: LocalDate): Boolean {
    val september1st: LocalDate = LocalDate.of(date.year, Month.SEPTEMBER, 1)
    val adjuster: TemporalAdjuster = TemporalAdjusters.ofDateAdjuster { d ->
        september1st.minusYears(if (d.isBefore(september1st)) 1 else 0)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    return date.with(adjuster).until(date, ChronoUnit.WEEKS).toInt() % 2 != 0
}