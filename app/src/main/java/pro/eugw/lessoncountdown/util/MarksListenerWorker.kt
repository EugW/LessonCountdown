package pro.eugw.lessoncountdown.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.network.JsArRe
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MarksListenerWorker(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    private lateinit var queue: RequestQueue
    private lateinit var mNotificationManager: NotificationManager
    private val savedMarks = File(context.filesDir, "savedMarks.json")
    private var oldMarksArray = JsonArray()
    private lateinit var prefs: SharedPreferences
    private var finished = false

    override fun onStopped() {
        super.onStopped()
        val it = Thread.currentThread().stackTrace
        val fl = File(context.filesDir, "crashStackTrace.trace")
        val str = StringBuilder()
        it.forEach {
            str.append("${it.lineNumber}: [${it.methodName}, ${it.className}]")
        }
        fl.writeText(str.toString())
        println("Stopped")
    }

    override fun doWork(): Result {
        if (!::queue.isInitialized)
            queue = Volley.newRequestQueue(applicationContext)!!
        if (!::mNotificationManager.isInitialized)
            mNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!mNotificationManager.notificationChannels.contains(NotificationChannel(WORKER_CHANNEL_ID, WORKER_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)))
            mNotificationManager.createNotificationChannel(NotificationChannel(WORKER_CHANNEL_ID, WORKER_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT))
        if (savedMarks.exists())
            oldMarksArray = JsonParser.parseString(savedMarks.readText()).asJsonArray
        if (!::prefs.isInitialized)
            prefs = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        if (!prefs.contains(KUNDELIK_TOKEN) || !SUPPORTED_KUNDELIK_ROLES.contains(prefs.getString(KUNDELIK_ROLE, "")))
            return Result.success()
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
                                                        prefs.edit {
                                                            putLong(LAST_CHECK, System.currentTimeMillis())
                                                        }
                                                        finished = true
                                                    },
                                                    Response.ErrorListener {
                                                        finished = true
                                                    }
                                            ))
                                        },
                                        Response.ErrorListener {
                                            finished = true
                                        }
                                ))
                            },
                            Response.ErrorListener {
                                finished = true
                            }
                    ))
                },
                Response.ErrorListener {
                    finished = true
                }
        ))
        while (!finished)
            Thread.sleep(1000)
        return Result.success()
    }

}