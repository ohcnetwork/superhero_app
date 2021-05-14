package kerala.superhero.app.home.service_request

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kerala.superhero.app.*
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.home.HomeActivity
import kerala.superhero.app.network.getAllServiceRequests
import kerala.superhero.app.network.updateFCMToken
import kerala.superhero.app.utils.logd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.runOnUiThread
import java.util.*
import kotlin.math.roundToInt

class FCMService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "service_requests"
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        if (prefs.accessToken.isNullOrEmpty()) {
            return
        }
        GlobalScope.launch {
            val response = withContext(Dispatchers.Default) { updateFCMToken(newToken) }
            response.logd("${Constants.TAG}_LU")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        message.data.logd("${Constants.TAG}_NR")
        if (message.data["cancelled"] == "true") {
            if (isHomeActivityOrRequestDetailsActivityOpen) {
                Intent("me.riafy.cc.watcher.location_tracker.action.RIDE_CANCELLED").let(::sendBroadcast)
            } else {
                sendCancelledNotification()
            }
            return
        }
        if (prefs.accessToken.isNullOrEmpty() || message.data["group"].isNullOrEmpty()) {
            return
        }
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) { getAllServiceRequests() }
                ) {
                is ResultWrapper.Success -> runOnUiThread {
                    if (!isHomeActivityOpen) {
                        showNotification(message)
                    }
                    playNotificationSound()
                    serviceRequests.value = response.value.data
                }
                else -> response.logd("${Constants.TAG}_RF")
            }
        }
    }

    private fun sendCancelledNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.warning_notifications)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("warning_notifications", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            with(NotificationManagerCompat.from(this)) {
                createNotificationChannel(channel)
            }
        }
        val notificationId = Math.random().roundToInt()
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("cancelled", true)
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, "warning_notifications")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(getString(R.string.ride_cancelled))
            .setContentText(getString(R.string.ride_canelled_notification_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.ride_cancelled_message))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        playNotificationSound()
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: RemoteMessage) {
        createNotificationChannel()
        val fullScreenIntent = Intent(this, HomeActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0,
            fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationId = Math.random().roundToInt()
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val address = message.data["address_0"]
        val units = message.data["requested_unit_count"]
        val requestFor = when (message.data["group"]) {
            "0" -> "ambulance"
            "1" -> "food delivery"
            "2" -> "medicine delivery"
            else -> "package delivery"
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("${requestFor.capitalize()} Request")
            .setOngoing(true)
            .setContentText("New request for $requestFor received")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A service request for $units $requestFor received from $address")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }


    private fun playNotificationSound() {
        val player = MediaPlayer.create(this, R.raw.notification).apply {
            isLooping = true
        }
        player.start()
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    player.stop()
                }
            }
        }, 3000)
    }

//    private fun playNotificationSound() {
//
//        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        val originalVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//        audioManager.setStreamVolume(
//            AudioManager.STREAM_MUSIC,
//            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
//            0
//        )
//        val player = MediaPlayer.create(this, R.raw.notification).apply {
//            isLooping = true
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            val attributes =
//                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
//            player.setAudioAttributes(attributes)
//        } else {
//            player.setAudioStreamType(AudioManager.STREAM_MUSIC)
//        }
//
//        player.start()
//        Timer().schedule(object : TimerTask() {
//            override fun run() {
//                runOnUiThread {
//                    player.stop()
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
//                }
//            }
//        }, 4000)
//    }

}