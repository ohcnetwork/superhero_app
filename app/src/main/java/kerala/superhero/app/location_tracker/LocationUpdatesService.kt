package kerala.superhero.app.location_tracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.*
import kerala.superhero.app.Constants
import kerala.superhero.app.R
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.home.HomeActivity
import kerala.superhero.app.home.service_request.FCMService
import kerala.superhero.app.isHomeActivityOrRequestDetailsActivityOpen
import kerala.superhero.app.network.updateLocationAsync
import kerala.superhero.app.prefs
import kerala.superhero.app.signup.SignUpActivity
import kerala.superhero.app.utils.logd
import kerala.superhero.app.utils.updateLastLocationUpdatedTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.runOnUiThread
import kotlin.math.roundToInt


class LocationUpdatesService: Service() {

    companion object {

        val TAG = LocationUpdatesService::class.java.simpleName

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        const val UPDATE_INTERVAL = Constants.LOCATION_UPDATE_INTERVAL * 60 * 1000L

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value, but they may be less frequent.
         */
        const val FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2

        /**
         * The max time before batched results are delivered by location services. Results may be
         * delivered sooner than this interval.
         */
        const val MAX_WAIT_TIME = UPDATE_INTERVAL * 2

        const val FOREGROUND_NOTIFICATION_ID = 1337
        const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "location_tracker"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.location_tracker_channel)
                val descriptionText = context.getString(R.string.location_tracker_channel_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(FOREGROUND_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                with(NotificationManagerCompat.from(context)){
                    createNotificationChannel(channel)
                }
            }
        }

        fun getTrackerRunningNotification(context: Context): Notification {
            val intent = Intent(context, SignUpActivity::class.java).apply {
                putExtra("location_tracker_running", false)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            return NotificationCompat.Builder(context, FOREGROUND_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(context.getString(R.string.location_tracker_active))
                .setContentText(context.getString(R.string.app_is_tracking_location))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.location_tracking_notification_big_text))
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
        }
    }

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private var mChangingConfiguration = false

    /**
     * Contains parameters used by [com.google.android.gms.location.FusedLocationProviderApi].
     */
    private var mLocationRequest: LocationRequest? = null

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Callback for changes in location.
     */
    private var mLocationCallback: LocationCallback? = null

    private var mServiceHandler: Handler? = null

    /**
     * The current location.
     */
    private var mLocation: Location? = null

    private var mNotificationManager: NotificationManager? = null

    private val mBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        // Called when HomeActivity comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        // Called when a client HomeActivity returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    private fun getLastLocation() {
        try {
            mFusedLocationClient?.lastLocation
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        "Failed to get location.".logd()
                    }
                }
        } catch (unlikely: SecurityException) {
            "Lost location permission.$unlikely".logd()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        "Last client unbound from service".logd()

        // Called when the HomeActivity unbinds from this
        // service. If this method is called due to a configuration change in HomeActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration) {
            "Starting foreground service".logd()
            liftToForeground()
        }
        return true // Ensures onRebind() is called when a client re-binds.
    }

    override fun onDestroy() {
        mServiceHandler?.removeCallbacksAndMessages(null)
    }

    /**
     * Makes a request for location updates.
     */
    fun requestLocationUpdates() {
        "Requesting location updates".logd()
        startService(Intent(applicationContext, LocationUpdatesService::class.java))
        try {
            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            "Lost location permission. Could not request updates. $unlikely".logd()
        }
    }

    private fun onNewLocation(location: Location) {
        "New location: $location".logd("${Constants.TAG}_NL")
        mLocation = location
        sendLocationToServer(location)
    }


    private fun sendCancelledNotification(context: Context) {
        val notificationId = Math.random().roundToInt()
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("cancelled", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, FCMService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(context.getString(R.string.ride_cancelled))
            .setContentText(context.getString(R.string.ride_canelled_notification_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.ride_cancelled_message))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }

    private fun sendLocationToServer(location: Location) {
        GlobalScope.launch {
            val response = withContext(Dispatchers.Default) {
                updateLocationAsync(
                    listOf(
                        location.latitude,
                        location.longitude
                    )
                )
            }
            runOnUiThread {
                updateLastLocationUpdatedTime()
            }
            response.logd("${Constants.TAG}_LUR")
            if (response is ResultWrapper.Success && response.value.current_service.isNullOrEmpty() && prefs.activeRideID != null) {
                if (isHomeActivityOrRequestDetailsActivityOpen) {
                    Intent("kerala.superhero.app.watcher.location_tracker.action.RIDE_CANCELLED").let(
                        ::sendBroadcast
                    )
                } else {
                    sendCancelledNotification(this@LocationUpdatesService)
                }
            }
        }
    }

    /**
     * Sets the location request parameters.
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval =
            UPDATE_INTERVAL
        mLocationRequest?.fastestInterval =
            FASTEST_UPDATE_INTERVAL
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest?.maxWaitTime =
            MAX_WAIT_TIME
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): LocationUpdatesService {
            return this@LocationUpdatesService
        }
    }

    override fun onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }
        createLocationRequest()
        getLastLocation()
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        "Location Service started".logd()
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY
    }

    private fun liftToForeground() {
        prefs.currentNotification = "tracker"
        startForeground(FOREGROUND_NOTIFICATION_ID, getTrackerRunningNotification(this))
    }

}