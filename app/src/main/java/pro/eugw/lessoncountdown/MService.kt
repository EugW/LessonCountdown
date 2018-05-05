package pro.eugw.lessoncountdown

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.activity.MainActivity
import java.io.File
import java.io.FileReader
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class MService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private var running: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        if (running)
            return
        running = true
        val localIntent = Intent(baseContext.packageName + ".SERVICE_STATE").putExtra("isRun", true)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "LessonChannel"
        val mBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle("LessonCountdown")
                .setContentText("LessonCountdown")
                .setSmallIcon(R.drawable.ic_oti)
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
        val schedule = try {
            JsonParser().parse(FileReader(File(filesDir, "schedule.json"))).asJsonObject
        } catch (e: Exception) {
            Toast.makeText(this, R.string.configErr, Toast.LENGTH_LONG).show()
            JsonObject()
        }
        val bells = try {
            JsonParser().parse(FileReader(File(filesDir, "bells.json"))).asJsonObject
        } catch (e: Exception) {
            Toast.makeText(this, R.string.configErr, Toast.LENGTH_LONG).show()
            JsonObject()
        }
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
        if (!schedule.has(dayOfWeek) || !bells.has(dayOfWeek))
            return
        val epochArr = JsonArray()
        for (element in bells.get(dayOfWeek).asJsonArray) {
            val s = element.asString.split("-")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val yrr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            epochArr.add(sdf.parse(yrr.format(Date()) + " " + s[0]).time.toString() + "-" + sdf.parse(yrr.format(Date()) + " " + s[1]).time.toString())
        }
        val lessArr = schedule.get(dayOfWeek).asJsonArray
        if (epochArr.size() < 1 || lessArr.size() < 1)
            running = false
        Thread {
            while (running && System.currentTimeMillis() <= epochArr.get(lessArr.indexOf(lessArr.last())).asString.split("-")[1].toLong() && lessArr.size() <= epochArr.size()) {
                val title = StringBuilder()
                val text = StringBuilder()
                for (i in 0 until lessArr.size()) {
                    val element = epochArr.get(i).asString.split("-")
                    val start = element[0].toLong()
                    val end = element[1].toLong()
                    val current = System.currentTimeMillis()
                    if (current in start..end) {
                        title.append(getString(R.string.curr) + ": ").append(lessArr[i].asString)
                        if (i < lessArr.size() - 1)
                            title.append(" - " + getString(R.string.next) + ": ").append(lessArr[i + 1].asString)
                        text.append(appendable(end, current))
                    } else if (i < lessArr.size() - 1) {
                        val nexEl = epochArr.get(i + 1).asString.split("-")
                        val nexS = nexEl[0].toLong()
                        if (current in (end + 1)..(nexS - 1)) {
                            text.append(appendable(nexS, current))
                            title.append(getString(R.string.next) + ": ").append(lessArr[i + 1].asString)
                        } else if (current < epochArr.first().asString.split("-")[0].toLong() && i == 0) {
                            val nolEl = epochArr.first().asString.split("-")
                            val nolS = nolEl[0].toLong()
                            text.append(appendable(nolS, current))
                            title.append(getString(R.string.next) + ": ").append(lessArr.first().asString)
                        }
                    }
                }
                mBuilder.setContentTitle(title)
                mBuilder.setContentText(text)
                mNotificationManager!!.notify(0, mBuilder.build())
                Thread.sleep(1000)
            }
            stopSelf()
        }.start()
        super.onCreate()
    }

    private fun appendable(vararg arg: Long): String = "${(arg[0] - arg[1]) / 60000} ${getString(R.string.min)} ${DecimalFormat("##").format((arg[0] - arg[1]) / 1000 % 60)} ${getString(R.string.sec)}"

    override fun onDestroy() {
        running = false
        val localIntent = Intent(baseContext.packageName + ".SERVICE_STATE").putExtra("isRun", false)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
        mNotificationManager!!.cancel(0)
        super.onDestroy()
    }

}
