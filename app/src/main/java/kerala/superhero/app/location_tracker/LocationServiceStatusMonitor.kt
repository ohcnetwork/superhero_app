package kerala.superhero.app.location_tracker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.datatransport.BuildConfig
import kerala.superhero.app.Constants
import kerala.superhero.app.R
import kerala.superhero.app.home.HomeActivity
import kerala.superhero.app.location_tracker.LocationUpdatesService.Companion.FOREGROUND_NOTIFICATION_CHANNEL_ID
import kerala.superhero.app.location_tracker.LocationUpdatesService.Companion.FOREGROUND_NOTIFICATION_ID
import kerala.superhero.app.location_tracker.LocationUpdatesService.Companion.createNotificationChannel
import kerala.superhero.app.location_tracker.LocationUpdatesService.Companion.getTrackerRunningNotification
import kerala.superhero.app.prefs
import kerala.superhero.app.utils.LAST_LOCATION_UPDATE_STRING_FORMAT
import kerala.superhero.app.utils.LAST_LOCATION_UPDATE_STRING_TIME_ZONE
import kerala.superhero.app.utils.logd
import java.text.SimpleDateFormat
import java.util.*

class LocationServiceStatusMonitor(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        val MAX_DIFF = (if (BuildConfig.DEBUG) 5 else 60) * 60 * 1000 // Max is 1 hour
    }

    override fun doWork(): Result = try {
        val lastUpdate = Calendar.getInstance(
            TimeZone.getTimeZone(
                LAST_LOCATION_UPDATE_STRING_TIME_ZONE
            )
        )
        lastUpdate.time = SimpleDateFormat(
            LAST_LOCATION_UPDATE_STRING_FORMAT,
            Locale.ENGLISH
        ).parse(prefs.lastLocationUpdate) ?: lastUpdate.time
        val currentInstance = Calendar.getInstance(
            TimeZone.getTimeZone(
                LAST_LOCATION_UPDATE_STRING_TIME_ZONE
            )
        )
        val diff = currentInstance.timeInMillis - lastUpdate.timeInMillis

        if (diff > MAX_DIFF) {
            sendLocationTrackerFailedNotification()
            "Location Tracker Status: Not Running".logd("${Constants.TAG}_LS_SM_NR")
        } else {
            if (prefs.currentNotification == "not_tracking"){
                with(NotificationManagerCompat.from(context)) {
                    notify(FOREGROUND_NOTIFICATION_ID, getTrackerRunningNotification(context))
                }
                prefs.currentNotification = "tracking"
            }
            "Location Tracker Status: Normal".logd("${Constants.TAG}_LS_SM_OK")
        }
        Result.success()
    } catch (e: Exception) {
        e.message.logd("${Constants.TAG}_LS_SM_E")
        Result.failure()
    }

    private fun sendLocationTrackerFailedNotification() {
        createNotificationChannel(context)
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("location_tracker_failed", true)
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, FOREGROUND_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(context.getString(R.string.tracker_inactive))
            .setContentText(context.getString(R.string.location_not_received_notification_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.location_tacker_not_running))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) {
            notify(FOREGROUND_NOTIFICATION_ID, builder.build())
        }
        prefs.currentNotification = "not_tracking"
    }

}