package pro.eugw.lessoncountdown.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import pro.eugw.lessoncountdown.util.network.KundelikMarks

class MarksListenerWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        KundelikMarks.check(applicationContext)
        return Result.success()
    }



}