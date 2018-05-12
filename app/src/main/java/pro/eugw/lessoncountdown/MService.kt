package pro.eugw.lessoncountdown

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.RemoteViews
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.activity.ServiceActivity
import pro.eugw.lessoncountdown.util.LessonTime
import java.io.File
import java.io.FileReader
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class MService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private var running: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        startActivity(Intent(this, ServiceActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onCreate() {
        super.onCreate()
        if (running)
            return
        running = true
        val localIntent = Intent(baseContext.packageName + ".SERVICE_STATE").putExtra("isRun", true)
        val instance = LocalBroadcastManager.getInstance(this)
        instance.sendBroadcast(localIntent)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "LessonChannel"
        val notificationLayout = RemoteViews(packageName, R.layout.notification_small)
        val prefs = getSharedPreferences("newPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("CustomColor", false)) {
            notificationLayout.setTextColor(R.id.textViewTitle, prefs.getInt("titleColor", Color.parseColor("#000000")))
            notificationLayout.setTextColor(R.id.textViewText, prefs.getInt("timeColor", Color.parseColor("#999999")))
            notificationLayout.setTextColor(R.id.textViewLessons, prefs.getInt("lessonsColor", Color.parseColor("#999999")))
            notificationLayout.setInt(R.id.layoutNotification, "setBackgroundColor", prefs.getInt("backgroundColor", Color.parseColor("#ffffff")))
        }
        val filter = IntentFilter(baseContext.packageName + ".NOTIFICATION_COLOR_UPDATE")
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                notificationLayout.setTextColor(R.id.textViewTitle, prefs.getInt("titleColor", Color.parseColor("#000000")))
                notificationLayout.setTextColor(R.id.textViewText, prefs.getInt("timeColor", Color.parseColor("#999999")))
                notificationLayout.setTextColor(R.id.textViewLessons, prefs.getInt("lessonsColor", Color.parseColor("#999999")))
                notificationLayout.setInt(R.id.layoutNotification, "setBackgroundColor", prefs.getInt("backgroundColor", Color.parseColor("#ffffff")))
            }
        }, filter)
        val mBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle("LessonCountdown")
                .setContentText("LessonCountdown")
                .setSmallIcon(R.drawable.ic_oti)
                .setCustomContentView(notificationLayout)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(Intent(this, MainActivity::class.java))
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(resultPendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mNotificationManager!!.createNotificationChannel(NotificationChannel(channelId, "LessonChannel", NotificationManager.IMPORTANCE_LOW))
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
        val schedule = try {
            JsonParser().parse(FileReader(File(filesDir, "schedule.json"))).asJsonObject[dayOfWeek].asJsonArray
        } catch (e: Exception) {
            e.printStackTrace()
            JsonArray()
        }
        val bells = try {
            JsonParser().parse(FileReader(File(filesDir, "bells.json"))).asJsonObject[dayOfWeek].asJsonArray
        } catch (e: Exception) {
            e.printStackTrace()
            JsonArray()
        }
        val lessonArray = ArrayList<LessonTime>()
        if (bells.size() < schedule.size())
            return
        schedule.forEachIndexed { index, jsonElement ->
            val s = bells[index].asString.split("-")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val yrr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            lessonArray.add(LessonTime(sdf.parse(yrr.format(Date()) + " " + s[0]).time, sdf.parse(yrr.format(Date()) + " " + s[1]).time, jsonElement.asString))
        }
        if (lessonArray.size < 1)
            running = false
        thread(true, true) {
            while (running && System.currentTimeMillis() <= lessonArray.last().end) {
                val title = StringBuilder()
                val text = StringBuilder()
                val lessons = StringBuilder()
                lessonArray.forEachIndexed { index, lessonTime ->
                    val start = lessonTime.start
                    val end = lessonTime.end
                    val current = System.currentTimeMillis()
                    if (current in start..end) {
                        title.append(getString(R.string.curr) + ": ").append(lessonTime.lesson)
                        if (index < lessonArray.size - 1)
                            title.append(" - " + getString(R.string.next) + ": ").append(lessonArray[index + 1].lesson)
                        text.append(appendable(end, current))
                        lessons.append("${getString(R.string.lessonsLeft)}: ${lessonArray.lastIndex - index}")
                    } else if (index < lessonArray.lastIndex) {
                        val nexS = lessonArray[index + 1].start
                        if (current in (end + 1)..(nexS - 1)) {
                            text.append(appendable(nexS, current))
                            title.append(getString(R.string.next) + ": ").append(lessonArray[index + 1].lesson)
                            lessons.append("${getString(R.string.lessonsLeft)}: ${lessonArray.lastIndex - index}")
                        }
                        if (current < lessonArray.first().start && index == 0) {
                            val nolS = lessonArray.first().start
                            text.append(appendable(nolS, current))
                            title.append(getString(R.string.next) + ": ").append(lessonArray.first().lesson)
                            lessons.append("${getString(R.string.lessonsLeft)}: ${lessonArray.size - index}")
                        }
                    }
                }
                notificationLayout.setTextViewText(R.id.textViewTitle, title)
                notificationLayout.setTextViewText(R.id.textViewText, text)
                notificationLayout.setTextViewText(R.id.textViewLessons, lessons)
                mNotificationManager!!.notify(0, mBuilder.build())
                Thread.sleep(1000)
            }
            stopSelf()
        }
    }

    private fun appendable(vararg arg: Long): String = "${(arg[0] - arg[1]) / 60000} ${getString(R.string.min)} ${DecimalFormat("##").format((arg[0] - arg[1]) / 1000 % 60)} ${getString(R.string.sec)}"

    override fun onDestroy() {
        super.onDestroy()
        running = false
        val localIntent = Intent(baseContext.packageName + ".SERVICE_STATE").putExtra("isRun", false)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
        mNotificationManager!!.cancel(0)
    }

}
