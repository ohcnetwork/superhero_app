package kerala.superhero.app.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.core.view.isVisible
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.dialog_delivery_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kerala.superhero.app.R
import kerala.superhero.app.activeServiceRequest
import kerala.superhero.app.data.DeliveryDestination
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.network.markDeliveryComplete
import kerala.superhero.app.utils.*
import kerala.superhero.app.views.ProgressDialog
import org.jetbrains.anko.alert
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast

class DestinationDetailsDialog(
    private val activity: Activity,
    private val deliveryDestination: DeliveryDestination,
    private val refreshList: () -> Unit
) : Dialog(activity) {

    private var progressDialog: ProgressDialog? = null

    override fun dismiss() {
        progressDialog?.dismiss()
        super.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(R.layout.dialog_delivery_details)

        progressDialog = ProgressDialog(context)

        completeTextView.text =
            context.getString(R.string.complete)

        nameTextView.text = deliveryDestination.name
        addressTextView.text = deliveryDestination.address
        quantityTextView.text =
            context.getString(R.string.food_packets, deliveryDestination.quantity)

        completeTextView.setOnClickListener {
            progressDialog?.show()
            completeCurrentDelivery {
                progressDialog?.hide()
                if (!it) {
                    dismiss()
                    refreshList()
                }
            }
        }

        callTextView.isVisible = !deliveryDestination.phone.isNullOrEmpty()
        callButtonImage.isVisible = !deliveryDestination.phone.isNullOrEmpty()
        callButtonImage.setOnClickListener {
            callRequester(deliveryDestination.phone)
        }
        callTextView.setOnClickListener {
            callRequester(deliveryDestination.phone)
        }

    }

    private fun callRequester(supportContact: String?) {
        if (activity.hasCallPermission()) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$supportContact")).let {
                activity.startActivity(it)
            }
        } else {
            activity.requestCallPhonePermission()
            activity.toast(context.getString(R.string.call_phone_permission_required))
        }
    }

    private fun completeCurrentDelivery(callback: (error: Boolean) -> Unit) {
        fetchLocation {
            if (it == null) {
                callback(true)
                return@fetchLocation
            }
            GlobalScope.launch {
                when (
                    val response = withContext(Dispatchers.Default) {
                        markDeliveryComplete(
                            deliveryDestination.id,
                            "${it.latitude},${it.longitude}"
                        )
                    }
                ) {
                    is ResultWrapper.Success -> {
                        context.runOnUiThread {
                            activeServiceRequest = response.value.data
                            callback(false)
                        }
                    }
                    is ResultWrapper.GenericError -> {
                        if (response.error.error == "You do not have any active services.") {
                            context.runOnUiThread {
                                callback(false)
                            }
                        } else {
                            activity.handleGenericError(response)
                            context.runOnUiThread {
                                callback(true)
                            }
                        }
                    }
                    is ResultWrapper.NetworkError -> {
                        context.handleNetworkError()
                        context.runOnUiThread {
                            callback(true)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation(callback: (location: Location?) -> Unit) {

        if (!activity.checkGpsStatus()) {
            activity.toast(R.string.unable_to_fetch_location)
            activity.requestLocationServices()
            callback(null)
            return
        }
        activity.checkPermissionsAndDo(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            activity.getString(R.string.permission_required)
        ) {
            LocationServices.getFusedLocationProviderClient(activity).requestLocationUpdates(
                LocationRequest.create().apply {
                    this.numUpdates = 1
                }, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)
                        if (locationResult != null) {
                            callback(locationResult.locations[0])
                        } else {
                            callback(null)
                            showLocationNotAvailableDialog()
                        }
                    }
                    override fun onLocationAvailability(locationAvailability: LocationAvailability?) {
                        super.onLocationAvailability(locationAvailability)
                        if (locationAvailability?.isLocationAvailable == false) {
                            callback(null)
                            showLocationNotAvailableDialog()
                        }
                    }
                }, null
            )
        }
    }

    private fun showLocationNotAvailableDialog() {
        context.alert {
            message = context.getString(R.string.location_not_availale_description)
            title = context.getString(R.string.location_not_availale)
            isCancelable = false
            positiveButton(R.string.close) {
                it.dismiss()
            }
        }.show()
    }

}