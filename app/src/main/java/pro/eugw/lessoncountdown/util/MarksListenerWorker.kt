package pro.eugw.lessoncountdown.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.network.JsArRe
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MarksListenerWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    private lateinit var queue: RequestQueue
    private lateinit var mNotificationManager: NotificationManager
    private val savedMarks = File(context.filesDir, "savedMarks.json")
    private val logFile = File(context.filesDir, "marksLog.json")
    private var oldMarksArray = JsonArray()
    private lateinit var prefs: SharedPreferences

    override fun doWork(): Result {
        val logEntry = JsonObject()
        logEntry.addProperty("timeStart", System.currentTimeMillis())
        var i = 0
        logEntry.addProperty("iStart", i)
        if (!::queue.isInitialized)
            queue = Volley.newRequestQueue(applicationContext)!!
        if (!::mNotificationManager.isInitialized)
            mNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !mNotificationManager.notificationChannels.contains(NotificationChannel(WORKER_CHANNEL_ID, WORKER_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)))
            mNotificationManager.createNotificationChannel(NotificationChannel(WORKER_CHANNEL_ID, WORKER_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT))
        if (savedMarks.exists())
            oldMarksArray = JsonParser().parse(savedMarks.readText()).asJsonArray
        if (!::prefs.isInitialized)
            prefs = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        if (!prefs.contains(KUNDELIK_TOKEN) || !SUPPORTED_KUNDELIK_ROLES.contains(prefs.getString(KUNDELIK_ROLE, "")))
            return Result.failure()
        val token = prefs.getString(KUNDELIK_TOKEN, "")
        logEntry.addProperty("omBefore", oldMarksArray.size())
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
                                                if (!oldMarksArray.contains(it))
                                                    newMarks.add(it)
                                            }
                                            oldMarksArray.addAll(newMarks)
                                            savedMarks.writeText(oldMarksArray.toString())
                                            val subjNames = JsonArray()
                                            newMarks.forEach {
                                                subjNames.add(it.asJsonObject["lesson"].asString)
                                            }
                                            queue.add(JsArRe(Request.Method.POST, "https://api.kundelik.kz/v1/lessons/many?access_token=$token", subjNames,
                                                    Response.Listener { response3 ->
                                                        logEntry.addProperty("omAfter", oldMarksArray.size())
                                                        i++
                                                        logEntry.addProperty("iEnd", i)
                                                        logEntry.add("newMarks", newMarks)
                                                        logEntry.addProperty("diffSize", newMarks.size())
                                                        var mmr = 0
                                                        newMarks.forEach {
                                                            mmr++
                                                            val name = response3.find { el -> el.asJsonObject["id"].asString == it.asJsonObject["lesson"].asString}
                                                            if (name != null) {
                                                                val mBuilder = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
                                                                        .setContentTitle(applicationContext.getString(R.string.newMark) + ": " + name.asJsonObject["subject"].asJsonObject["name"].asString)
                                                                        .setContentText(it.asJsonObject["value"].asString + " " + applicationContext.getString(R.string.onMarkDate) + " "  + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.asJsonObject["date"].asString.split("T")[0])!!))
                                                                        .setSmallIcon(R.drawable.ic_oti)
                                                                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                                                        .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                                                                mNotificationManager.notify(Random.nextInt(), mBuilder.build())
                                                            }
                                                        }
                                                        logEntry.addProperty("notSent", mmr)
                                                        logEntry.addProperty("timeEnd", System.currentTimeMillis())
                                                        if (!logFile.exists())
                                                            logFile.writeText("[]")
                                                        val logArr = JsonParser().parse(logFile.readText()).asJsonArray
                                                        logArr.add(logEntry)
                                                        logFile.writeText(logArr.toString())
                                                        println("End reached")
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
        return Result.success()
    }

}