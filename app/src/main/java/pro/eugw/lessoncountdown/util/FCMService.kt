package pro.eugw.lessoncountdown.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class FCMService : FirebaseMessagingService() {

    private lateinit var queue: RequestQueue
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        queue = Volley.newRequestQueue(this)
        prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        val host = prefs.getString(CUSTOM_ADDRESS, getString(R.string.host))
        queue.add(JsonObjectRequest("https://$host/updateFCMToken?token=${prefs.getString(LCAPI_TOKEN, "")}&fcmtoken=$p0", null,
                Response.Listener {},
                Response.ErrorListener {}
        ))

    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        val parsed = JsonObject()
        p0.data.forEach {
            parsed.addProperty(it.key, it.value)
        }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(parsed["date"].asString)
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mBuilder = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
                .setContentTitle(getString(R.string.newMark) + ": " + parsed["subject"].asString)
                .setContentText(parsed["mark"].asString + " " + getString(R.string.onMarkDate) + " "  + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date))
                .setSmallIcon(R.drawable.ic_oti)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !mNotificationManager.notificationChannels.contains(NotificationChannel(FCM_CHANNEL_ID, FCM_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)))
            mNotificationManager.createNotificationChannel(NotificationChannel(FCM_CHANNEL_ID, FCM_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT))
        mNotificationManager.notify(Random.nextInt(), mBuilder.build())
    }
}