package kerala.superhero.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import kerala.superhero.app.data.UserProfile
import kerala.superhero.app.data.ServiceRequest

val prefs: App.Prefs by lazy {
    App.prefs!!
}

var isHomeActivityOpen = false

var isHomeActivityOrRequestDetailsActivityOpen = false

var userProfile: UserProfile? = null

var lastLocationUpdate = MutableLiveData<String>()

val serviceRequests = MutableLiveData<List<ServiceRequest>>()

var activeServiceRequest: ServiceRequest? = null

class App: Application() {

    companion object {
        var prefs: Prefs? = null
    }

    override fun onCreate() {
        prefs =
            Prefs(applicationContext)
        serviceRequests.value = emptyList()
        lastLocationUpdate.value = prefs?.lastLocationUpdate
        super.onCreate()
    }

    class Prefs(context: Context) {
        fun clear(retainToken: Boolean = false) {
            val token = if (retainToken) accessToken else null
            val localeString = locale
            prefs.edit().clear().apply()
            accessToken = token
            locale = localeString
        }

        private val prefsFileName = "me.riafy.cc.watcher.prefs"
        private val keyAccessToken = "access_token"
        private val keyLocale = "locale"
        private val keyLastLocation = "last_location"
        private val keyActiveRideID = "active_ride"
        private val keyCurrentNotification = "current_notification"
        private val prefs: SharedPreferences = context.getSharedPreferences(prefsFileName, 0)

        var accessToken: String?
            get() = prefs.getString(keyAccessToken, null)
            set(value) = prefs.edit().putString(keyAccessToken, value).apply()

        var locale: String
            get() = prefs.getString(keyLocale, "English") ?: "English"
            set(value) = prefs.edit().putString(keyLocale, value).apply()

        var lastLocationUpdate: String
            get() = prefs.getString(keyLastLocation, "Never") ?: "Never"
            set(value) = prefs.edit().putString(keyLastLocation, value).apply()

        var activeRideID: String?
            get() = prefs.getString(keyActiveRideID, null)
            set(value) = prefs.edit().putString(keyActiveRideID, value).apply()

        var currentNotification: String
            get() = prefs.getString(keyCurrentNotification, "not_tracking") ?: "not_tracking"
            set(value) = prefs.edit().putString(keyCurrentNotification, value).apply()

    }
}
