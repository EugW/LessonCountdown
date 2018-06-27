package pro.eugw.lessoncountdown

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.gson.JsonArray
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
    private lateinit var instance: androidx.localbroadcastmanager.content.LocalBroadcastManager
    private var mBinder = MBinder()

    inner class MBinder : Binder() {
        internal val service: MService
            get() = this@MService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        instance = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationLayout = RemoteViews(packageName, R.layout.notification_small)
        val prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CUSTOM_COLOR, false)) {
            notificationLayout.setInt(R.id.imageView, "setColorFilter", prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
            notificationLayout.setTextColor(R.id.textViewCurrent, prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
            notificationLayout.setTextColor(R.id.textViewNext, prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
            notificationLayout.setTextColor(R.id.textViewText, prefs.getInt(TIME_COLOR, Color.parseColor("#999999")))
            notificationLayout.setTextColor(R.id.textViewLessons, prefs.getInt(LESSONS_COLOR, Color.parseColor("#999999")))
            notificationLayout.setInt(R.id.layoutNotification, "setBackgroundColor", prefs.getInt(BACKGROUND_COLOR, Color.parseColor("#ffffff")))
        }
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(CHANNEL_ID)
                .setContentText(CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_oti)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_oti))
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                .setCustomContentView(notificationLayout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mNotificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW))
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent) {
                if (p1.getBooleanExtra("cVal", false)) {
                    notificationLayout.setInt(R.id.imageView, "setColorFilter", prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
                    notificationLayout.setTextColor(R.id.textViewCurrent, prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
                    notificationLayout.setTextColor(R.id.textViewNext, prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
                    notificationLayout.setTextColor(R.id.textViewText, prefs.getInt(TIME_COLOR, Color.parseColor("#999999")))
                    notificationLayout.setTextColor(R.id.textViewLessons, prefs.getInt(LESSONS_COLOR, Color.parseColor("#999999")))
                    notificationLayout.setInt(R.id.layoutNotification, "setBackgroundColor", prefs.getInt(BACKGROUND_COLOR, Color.parseColor("#ffffff")))
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
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
            val schedule = try {
                JsonParser().parse(FileReader(File(filesDir, SCHEDULE_FILE))).asJsonObject[dayOfWeek].asJsonArray
            } catch (e: Exception) {
                JsonArray()
            }
            val bells = try {
                JsonParser().parse(FileReader(File(filesDir, BELLS_FILE))).asJsonObject[dayOfWeek].asJsonArray
            } catch (e: Exception) {
                JsonArray()
            }
            val lessonArray = ArrayList<LessonTime>()
            try {
                schedule.forEachIndexed { index, jsonElement ->
                    val s = bells[index].asString.split("-")
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    val yrr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    lessonArray.add(LessonTime(sdf.parse(yrr.format(Date()) + " " + s[0]).time, sdf.parse(yrr.format(Date()) + " " + s[1]).time, jsonElement.asString))
                }
            } catch (e: Exception) {
                lessonArray.clear()
            }
            running = true
            instance.sendBroadcast(Intent(baseContext.packageName + SERVICE_STATE).putExtra("isRun", running))
            if (lessonArray.isNotEmpty() && File(filesDir, SERVICE_PID).exists())
                while (running && System.currentTimeMillis() <= lessonArray.last().end) {
                    val title = StringBuilder()
                    var l1 = ""
                    var l2 = ""
                    val text = StringBuilder()
                    val lessons = StringBuilder()
                    lessonArray.forEachIndexed { index, lessonTime ->
                        val start = lessonTime.start
                        val end = lessonTime.end
                        val current = System.currentTimeMillis()
                        if (current in start..end) {
                            title.append(getString(R.string.curr) + ": ").append(lessonTime.lesson)
                            l1 = lessonTime.lesson
                            if (index < lessonArray.size - 1) {
                                title.append(" - " + getString(R.string.next) + ": ").append(lessonArray[index + 1].lesson)
                                l2 = lessonArray[index + 1].lesson
                            }
                            text.append(appendable(end, current))
                            lessons.append("${getString(R.string.lessonsRemaining)}: ${lessonArray.lastIndex - index}")
                        } else if (index < lessonArray.lastIndex) {
                            val nexS = lessonArray[index + 1].start
                            if (current in (end + 1)..(nexS - 1)) {
                                text.append(appendable(nexS, current))
                                title.append(getString(R.string.next) + ": ").append(lessonArray[index + 1].lesson)
                                l2 = lessonArray[index + 1].lesson
                                lessons.append("${getString(R.string.lessonsRemaining)}: ${lessonArray.lastIndex - index}")
                            }
                            if (current < lessonArray.first().start && index == 0) {
                                val nolS = lessonArray.first().start
                                text.append(appendable(nolS, current))
                                title.append(getString(R.string.next) + ": ").append(lessonArray.first().lesson)
                                l2 = lessonArray.first().lesson
                                lessons.append("${getString(R.string.lessonsRemaining)}: ${lessonArray.size - index}")
                            }
                        }
                    }
                    notificationLayout.setTextViewText(R.id.textViewCurrent, l1)
                    notificationLayout.setTextViewText(R.id.textViewNext, l2)
                    notificationLayout.setTextViewText(R.id.textViewText, text)
                    notificationLayout.setTextViewText(R.id.textViewLessons, lessons)
                    mNotificationManager.notify(0, mBuilder.build())
                    Thread.sleep(1000)
                }
            running = false
            instance.sendBroadcast(Intent(baseContext.packageName + SERVICE_STATE).putExtra("isRun", running))
            mNotificationManager.cancel(0)
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

    private fun appendable(vararg arg: Long): String = "${(arg[0] - arg[1]) / 60000} ${getString(R.string.min)} ${(arg[0] - arg[1]) / 1000 % 60} ${getString(R.string.sec)}"

}
