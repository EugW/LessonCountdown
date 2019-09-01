package pro.eugw.lessoncountdown.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import pro.eugw.lessoncountdown.util.network.KundelikMarks

class MarksListenerAlarmService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        KundelikMarks.check(applicationContext)
        return super.onStartCommand(intent, flags, startId)
    }
}