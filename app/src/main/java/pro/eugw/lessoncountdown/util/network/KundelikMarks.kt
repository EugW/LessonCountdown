package pro.eugw.lessoncountdown.util.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class KundelikMarks {
    companion object {
        fun check(applicationContext: Context) {
            val queue = Volley.newRequestQueue(applicationContext)!!
            var jsonArray = JsonArray()
            val mNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !mNotificationManager.notificationChannels.contains(NotificationChannel(WORKER_CHANNEL_ID, WORKER_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)))
                mNotificationManager.createNotificationChannel(NotificationChannel(WORKER_CHANNEL_ID, WORKER_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT))
            val file = File(applicationContext.filesDir, "savedMarks.json")
            if (file.exists())
                jsonArray = JsonParser().parse(file.readText()).asJsonArray

            val prefs = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
            if (!prefs.contains(KUNDELIK_TOKEN) || !SUPPORTED_KUNDELIK_ROLES.contains(prefs.getString(KUNDELIK_ROLE, ""))) {
                return
            }
            val token = prefs.getString(KUNDELIK_TOKEN, "")
            queue.add(JsObRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me?access_token=$token",
                    Response.Listener { response ->
                        val personId = response["personId"].asString
                        queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me/schools?access_token=$token",
                                Response.Listener { response1 ->
                                    val schoolId = response1[0].asString
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val calendar = Calendar.getInstance()
                                    val to = sdf.format(calendar.time)
                                    calendar.add(Calendar.MONTH, -1)
                                    val from = sdf.format(calendar.time)
                                    queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/persons/$personId/schools/$schoolId/marks/$from/$to?access_token=$token",
                                            Response.Listener { response2 ->
                                                prefs.edit {
                                                    putLong(LAST_CHECK, System.currentTimeMillis())
                                                }
                                                val newMarks = JsonArray()
                                                response2.forEach {
                                                    if (!jsonArray.contains(it))
                                                        newMarks.add(it)
                                                }
                                                jsonArray.addAll(newMarks)
                                                file.writeText(jsonArray.toString())
                                                val jArr = JsonArray()
                                                newMarks.forEach {
                                                    jArr.add(it.asJsonObject["lesson"].asString)
                                                }
                                                queue.add(JsArRe(Request.Method.POST, "https://api.kundelik.kz/v1/lessons/many?access_token=$token", jArr,
                                                        Response.Listener { response3 ->
                                                            val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                                                            newMarks.sortedBy { sdf2.parse(it.asJsonObject["date"].asString).time }.forEach {
                                                                var name = "null"
                                                                response3.forEach { el ->
                                                                    if (el.asJsonObject["id"].asString == it.asJsonObject["lesson"].asString)
                                                                        name = el.asJsonObject["subject"].asJsonObject["name"].asString
                                                                }
                                                                val mBuilder = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
                                                                        .setContentTitle(applicationContext.getString(R.string.newMark) + ": " + name)
                                                                        .setContentText(it.asJsonObject["value"].asString + " " + applicationContext.getString(R.string.onMarkDate) + " "  + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.asJsonObject["date"].asString.split("T")[0])))
                                                                        .setSmallIcon(R.drawable.ic_oti)
                                                                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                                                        .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                                                                mNotificationManager.notify(Random.nextInt(), mBuilder.build())
                                                            }
                                                            queue.stop()
                                                        },
                                                        Response.ErrorListener {
                                                        }
                                                ))
                                            },
                                            Response.ErrorListener {
                                            }
                                    ))
                                },
                                Response.ErrorListener {
                                }
                        ))
                    },
                    Response.ErrorListener {
                    }
            ))
        }
    }
}