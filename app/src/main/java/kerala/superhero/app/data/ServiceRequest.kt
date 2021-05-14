package kerala.superhero.app.data

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

data class DeliveryDestination(
    val id: String,
    val name: String,
    val address: String,
    val landmark_address: String,
    val landmark_location: PointLocation?,
    val phone: String?,
    val quantity: Int,
    val delivered: Boolean
)

data class ServiceRequest(
    val id: String,
    val category: String,
    val address_0: String?,
    val address_1: String?,
    val location_0: PointLocation?,
    val location_1: PointLocation?,
    val support_contact: String?,
    //new fields starts here
    var patient_name: String?,
    var destination_contact: String?,
    var medical_info: String?,
    //new fields ends here
    var picked_up: Boolean,
    val active: Boolean,
    val createdAt: String,
    val destination_addresses: List<DeliveryDestination>?
) {
    fun getParsedDate(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val instantDate = Instant.parse(createdAt)
        val calendar = Calendar.getInstance()
        calendar.time = Date.from(instantDate)
        "${calendar.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            Locale.getDefault()
        )}, ${getFormattedDate(calendar.time)}"
    } else {
        createdAt.split("T")[0].split("-").asReversed().joinToString("/")
    }

    private fun getFormattedDate(date: Date): String? {
        val dateTimeFormat = "'of' MMMM yyyy, hh:mm a"
        val cal = Calendar.getInstance()
        cal.time = date
        //2nd of march 2015
        val day = cal[Calendar.DATE]
        return if (day !in 11..18) when (day % 10) {
            1 -> SimpleDateFormat("d'st' $dateTimeFormat", Locale.getDefault()).format(date)
            2 -> SimpleDateFormat("d'nd' $dateTimeFormat", Locale.getDefault()).format(date)
            3 -> SimpleDateFormat("d'rd' $dateTimeFormat", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("d'th' $dateTimeFormat", Locale.getDefault()).format(date)
        } else SimpleDateFormat("d'th' $dateTimeFormat", Locale.getDefault()).format(date)
    }
}

data class PointLocation(val type: String, val coordinates: List<Double>)

/**
 * Change in Code - Nitheesh.
 * Starts here
 */
//data class Location(val location: List<Double>)