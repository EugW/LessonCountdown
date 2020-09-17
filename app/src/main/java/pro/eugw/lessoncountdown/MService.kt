package pro.eugw.lessoncountdown

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.IBinder
import android.os.PowerManager
import android.view.View
import android.widget.RemoteViews
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var flags: Int? = null
    private var running: Boolean = true
    private lateinit var runnable: () -> Unit

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationLayout = RemoteViews(packageName, R.layout.notification_small)
        val prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CUSTOM_COLOR, false)) {
            defaultColors(notificationLayout, prefs)
        }
        registerReceivers(notificationLayout, prefs)
        mNotificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW))
        val mNotification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("LessonCountdown")
                .setContentText("Time notification")
                .setSmallIcon(R.drawable.ic_oti)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_oti))
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this,
                        MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                .setCustomContentView(notificationLayout)
                .setGroup(TIME_GROUP).build()
        runnable = {
            running = true
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(baseContext.packageName + SERVICE_STATE).putExtra("isRun", running))
            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LCEndlessService::lock").apply {
                    acquire(24 * 3600 * 1000)
                }
            }
            startForeground(TIME_NOTIFICATION_ID, mNotification)
            val schedule = try {
                JsonParser.parseReader(FileReader(File(filesDir, SCHEDULE_FILE))).asJsonObject
            } catch (e: Exception) {
                JsonObject()
            }
            var dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
            if (isEvenWeek(LocalDate.now()))
                dow += "e"
            val lessonArray = ArrayList<LessonTime>()
            try {
                schedule[dow].asJsonArray.forEach { jsonElement ->
                    val s = jsonElement.asJsonObject["time"].asString.split("-")
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    val yrr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    lessonArray.add(LessonTime(sdf.parse(yrr.format(Date()) + " " + s[0])!!.time,
                            sdf.parse(yrr.format(Date()) + " " + s[1])!!.time, jsonElement.asJsonObject["lesson"].asString,
                            jsonElement.asJsonObject["cabinet"].asString))
                }
            } catch (e: Exception) {
                lessonArray.clear()
            }
            if (lessonArray.isNotEmpty())
                while (running && System.currentTimeMillis() <= lessonArray.last().end) {
                    var l1 = ""
                    var l2 = ""
                    var text = ""
                    var cabinet = ""
                    lessonArray.forEachIndexed { index, lessonTime ->
                        val start = lessonTime.start
                        val end = lessonTime.end
                        val current = System.currentTimeMillis()
                        if (current in start..end) {
                            l1 = lessonTime.lesson
                            if (index < lessonArray.lastIndex) {
                                l2 = lessonArray[index + 1].lesson
                                cabinet = lessonArray[index + 1].cabinet
                            }
                            text = appendable(end, current)
                        } else if (current < lessonArray.first().start && index == 0) {
                            val nolS = lessonArray.first().start
                            text = appendable(nolS, current)
                            l2 = lessonArray.first().lesson
                            cabinet = lessonArray.first().cabinet
                        } else if (index < lessonArray.lastIndex) {
                            val nexS = lessonArray[index + 1].start
                            if (current in (end + 1) until nexS) {
                                text = appendable(nexS, current)
                                l2 = lessonArray[index + 1].lesson
                                cabinet = lessonArray[index + 1].cabinet
                            }
                        }
                    }
                    if (l2.isNotBlank())
                        notificationLayout.setViewVisibility(R.id.imageViewNextArrow, View.VISIBLE)
                    else
                        notificationLayout.setViewVisibility(R.id.imageViewNextArrow, View.GONE)
                    notificationLayout.setTextViewText(R.id.textViewCurrent, l1)
                    notificationLayout.setTextViewText(R.id.textViewNext, l2)
                    notificationLayout.setTextViewText(R.id.textViewText, text)
                    notificationLayout.setTextViewText(R.id.textViewCabinet, cabinet)
                    mNotificationManager.notify(TIME_NOTIFICATION_ID, mNotification)
                    Thread.sleep(5000)
                }
            running = false
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(baseContext.packageName + SERVICE_STATE).putExtra("isRun", running))
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            flags?.let { stopForeground(it) }
            stopForeground(true)
        }
        thread(true, block = runnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.flags = flags
        return START_STICKY
    }

    private fun defaultColors(notificationLayout: RemoteViews, prefs: SharedPreferences) {
        notificationLayout.setTextColor(R.id.textViewCurrent, prefs.getInt(TITLE_COLOR,
                Color.parseColor("#000000")))
        notificationLayout.setInt(R.id.imageViewNextArrow, "setColorFilter",
                prefs.getInt(TITLE_COLOR, Color.parseColor("#000000")))
        notificationLayout.setTextColor(R.id.textViewNext, prefs.getInt(TITLE_COLOR,
                Color.parseColor("#000000")))
        notificationLayout.setTextColor(R.id.textViewText, prefs.getInt(TIME_COLOR,
                Color.parseColor("#999999")))
        notificationLayout.setTextColor(R.id.textViewCabinet, prefs.getInt(LESSONS_COLOR,
                Color.parseColor("#999999")))
        notificationLayout.setInt(R.id.layoutNotification, "setBackgroundColor",
                prefs.getInt(BACKGROUND_COLOR, Color.parseColor("#ffffff")))
    }

    private fun appendable(vararg arg: Long): String =
            "${(arg[0] - arg[1]) / 60000} ${getString(R.string.min)} ${(arg[0] - arg[1]) / 1000 % 60} ${getString(R.string.sec)}"

    private fun registerReceivers(notificationLayout: RemoteViews, prefs: SharedPreferences) {
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent) {
                if (p1.getBooleanExtra("cVal", false)) {
                    defaultColors(notificationLayout, prefs)
                } else {
                    LocalBroadcastManager.getInstance(this@MService).sendBroadcast(Intent(baseContext.packageName + PEND_SERVICE_RESTART))
                    running = false
                }
            }
        }, IntentFilter(baseContext.packageName + NOTIFICATION_COLOR_UPDATE))
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                LocalBroadcastManager.getInstance(this@MService).sendBroadcast(Intent(baseContext.packageName + PEND_SERVICE_RESTART))
                running = false
            }
        }, IntentFilter(baseContext.packageName + NOTIFICATION_STYLE_UPDATE))
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent) {
                if (p1.getBooleanExtra("SIG", false)) {
                    running = false
                } else {
                    thread(true, block = runnable)
                }
            }
        }, IntentFilter(baseContext.packageName + SERVICE_SIGNAL))
    }

}
