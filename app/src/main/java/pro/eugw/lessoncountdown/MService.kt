package pro.eugw.lessoncountdown

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MService : Service() {

    var running: Boolean = true
    private lateinit var runnable: () -> Unit
    private lateinit var instance: LocalBroadcastManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = LocalBroadcastManager.getInstance(this)
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationLayout = RemoteViews(packageName, R.layout.notification_small)
        val prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CUSTOM_COLOR, false)) {
            defaultColors(notificationLayout, prefs)
        }
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LessonCountdown")
                .setContentText("Time notification")
                .setSmallIcon(R.drawable.ic_oti)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_oti))
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                .setCustomContentView(notificationLayout)
                .setGroup(TIME_GROUP).build()
        startForeground(TIME_NOTIFICATION_ID, mBuilder)
        mNotificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW))
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent) {
                if (p1.getBooleanExtra("cVal", false)) {
                    defaultColors(notificationLayout, prefs)
                } else {
                    instance.sendBroadcast(Intent(baseContext.packageName + PEND_SERVICE_RESTART))
                    running = false
                }
            }
        }, IntentFilter(baseContext.packageName + NOTIFICATION_COLOR_UPDATE))
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                instance.sendBroadcast(Intent(baseContext.packageName + PEND_SERVICE_RESTART))
                running = false
            }
        }, IntentFilter(baseContext.packageName + NOTIFICATION_STYLE_UPDATE))
        runnable = {
            val schedule = try {
                JsonParser.parseReader(FileReader(File(filesDir, SCHEDULE_FILE))).asJsonObject
            } catch (e: Exception) {
                JsonObject()
            }
            var dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
            if (prefs.getBoolean(EVEN_ODD_WEEKS, false) || schedule.has("${dow}e")) {
                val preEven = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2 == 0
                val even = if (!prefs.getBoolean(INVERSE_EVEN_ODD_WEEKS, false)) preEven else !preEven
                if (even)
                    dow += "e"
            }
            val lessonArray = ArrayList<LessonTime>()
            try {
                schedule[dow].asJsonArray.forEach { jsonElement ->
                    val s = jsonElement.asJsonObject["time"].asString.split("-")
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    val yrr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    lessonArray.add(LessonTime(sdf.parse(yrr.format(Date()) + " " + s[0])!!.time, sdf.parse(yrr.format(Date()) + " " + s[1])!!.time, jsonElement.asJsonObject["lesson"].asString, jsonElement.asJsonObject["cabinet"].asString))
                }
            } catch (e: Exception) {
                lessonArray.clear()
            }
            running = true
            instance.sendBroadcast(Intent(baseContext.packageName + SERVICE_STATE).putExtra("isRun", running))
            if (lessonArray.isNotEmpty())
                while (running && System.currentTimeMillis() <= lessonArray.last().end) {
                    var l1 = ""
                    var l2 = ""
                    val text = StringBuilder()
                    val lessons = StringBuilder()
                    lessonArray.forEachIndexed { index, lessonTime ->
                        val start = lessonTime.start
                        val end = lessonTime.end
                        val current = System.currentTimeMillis()
                        if (current in start..end) {
                            l1 = lessonTime.lesson
                            if (index < lessonArray.size - 1) {
                                l2 = lessonArray[index + 1].lesson
                            }
                            text.append(appendable(end, current))
                            lessons.append("${getString(R.string.lessonsRemaining)}: ${lessonArray.lastIndex - index}")
                        } else if (current < lessonArray.first().start && index == 0) {
                            val nolS = lessonArray.first().start
                            text.append(appendable(nolS, current))
                            l2 = lessonArray.first().lesson
                            lessons.append("${getString(R.string.lessonsRemaining)}: ${lessonArray.size - index}")
                        } else if (index < lessonArray.lastIndex) {
                            val nexS = lessonArray[index + 1].start
                            if (current in (end + 1) until nexS) {
                                text.append(appendable(nexS, current))
                                l2 = lessonArray[index + 1].lesson
                                lessons.append("${getString(R.string.lessonsRemaining)}: ${lessonArray.lastIndex - index}")
                            }
                        }
                    }
                    notificationLayout.setTextViewText(R.id.textViewCurrent, l1)
                    notificationLayout.setTextViewText(R.id.textViewNext, l2)
                    notificationLayout.setTextViewText(R.id.textViewText, text)
                    notificationLayout.setTextViewText(R.id.textViewLessons, lessons)
                    mNotificationManager.notify(TIME_NOTIFICATION_ID, mBuilder)
                    Thread.sleep(5000)
                }
            running = false
            instance.sendBroadcast(Intent(baseContext.packageName + SERVICE_STATE).putExtra("isRun", running))
            mNotificationManager.cancel(TIME_NOTIFICATION_ID)
            stopSelf()
        }
        thread(true, block = runnable)
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent) {
                if (p1.getBooleanExtra("START", false) && !running) {
                    thread(true, block = runnable)
                }
                if (p1.getBooleanExtra("STOP", false)) {
                    running = false
                }
            }
        }, IntentFilter(baseContext.packageName + SERVICE_SIGNAL))
    }

    private fun defaultColors(notificationLayout: RemoteViews, prefs: SharedPreferences) {
        notificationLayout.setTextColor(R.id.textViewCurrent, prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
        notificationLayout.setInt(R.id.imageViewNextArrow, "setColorFilter", prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
        notificationLayout.setTextColor(R.id.textViewNext, prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
        notificationLayout.setTextColor(R.id.textViewText, prefs.getInt(TIME_COLOR, Color.parseColor("#999999")))
        notificationLayout.setTextColor(R.id.textViewLessons, prefs.getInt(LESSONS_COLOR, Color.parseColor("#999999")))
        notificationLayout.setInt(R.id.layoutNotification, "setBackgroundColor", prefs.getInt(BACKGROUND_COLOR, Color.parseColor("#ffffff")))
    }

    private fun appendable(vararg arg: Long): String = "${(arg[0] - arg[1]) / 60000} ${getString(R.string.min)} ${(arg[0] - arg[1]) / 1000 % 60} ${getString(R.string.sec)}"

}
