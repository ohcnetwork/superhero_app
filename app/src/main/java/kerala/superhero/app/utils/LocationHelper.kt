package kerala.superhero.app.utils

import kerala.superhero.app.lastLocationUpdate
import kerala.superhero.app.prefs
import java.text.SimpleDateFormat
import java.util.*

const val LAST_LOCATION_UPDATE_STRING_FORMAT = "dd-MM-yyy hh:mm:ss a"
const val LAST_LOCATION_UPDATE_STRING_TIME_ZONE = "Asia/Kolkata"


fun updateLastLocationUpdatedTime() {
    val date = Calendar.getInstance(TimeZone.getTimeZone(LAST_LOCATION_UPDATE_STRING_TIME_ZONE))
    val format = SimpleDateFormat(LAST_LOCATION_UPDATE_STRING_FORMAT, Locale.ENGLISH)
    val updateTimeString = format.format(date.time)
    prefs.lastLocationUpdate = updateTimeString
    lastLocationUpdate.value = updateTimeString
}