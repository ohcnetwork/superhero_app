package kerala.superhero.app.home

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.core.view.isVisible
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.dialog_active_ride.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kerala.superhero.app.Constants
import kerala.superhero.app.R
import kerala.superhero.app.activeServiceRequest
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.data.ServiceRequest
import kerala.superhero.app.network.endCurrentTrip
import kerala.superhero.app.network.pickUpCurrentTrip
import kerala.superhero.app.network.updateLocationAsync
import kerala.superhero.app.prefs
import kerala.superhero.app.utils.*
import kerala.superhero.app.views.ProgressDialog
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast

class ActiveRideDialog(
    private val activity: HomeActivity,
    private val serviceRequest: ServiceRequest
) : Dialog(activity) {

    private var progressDialog: ProgressDialog? = null

    override fun dismiss() {
        activity.isDialogOpen = false
        progressDialog?.dismiss()
        super.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(R.layout.dialog_active_ride)

        progressDialog = ProgressDialog(context)

        activeServiceRequest = serviceRequest

        pickupAddressTextView.text = serviceRequest.address_0
        destinationAddressTextView.text =
            if (serviceRequest.address_1.isNullOrEmpty()) context.getString(R.string.will_be_informed_on_arrival) else serviceRequest.address_1
        timeTextView.text = serviceRequest.getParsedDate()

        completeTextView.text =
            context.getString(if (serviceRequest.picked_up) R.string.complete else R.string.complete_pickup)
        Log.d("debugdialogtest", " serviceRequest.picked_up: ${serviceRequest.picked_up}")

        if (serviceRequest.picked_up) {
            navigateTextView.isVisible = activeServiceRequest?.location_1 != null
            navigateButtonImage.isVisible = activeServiceRequest?.location_1 != null

            /**
             * Change in Code - Nitheesh.
             * Added new fields
             */
            //show destination number and support number and call = dial destination number, call visibility
            if (!serviceRequest.destination_contact.isNullOrEmpty()) {
                callTextView.isVisible = true
                callButtonImage.isVisible = true
                destinationNumberLabelTextView.isVisible = true
                destinationCallButtonImage.isVisible = true
                destinationNumberTextView.isVisible = true
                destinationNumberTextView.text = serviceRequest.destination_contact
                destinationCallButtonImage.setOnClickListener {
                    activity.callRequester(serviceRequest.destination_contact)
                }
                destinationNumberTextView.setOnClickListener {
                    activity.callRequester(serviceRequest.destination_contact)
                }
            }

            if (!serviceRequest.support_contact.isNullOrEmpty()) {
                supportNumberLabelTextView.isVisible = true
                supportCallButtonImage.isVisible = true
                supportNumberTextView.isVisible = true
                supportNumberTextView.text = serviceRequest.support_contact
                supportCallButtonImage.setOnClickListener {
                    activity.callRequester(serviceRequest.support_contact)
                }
                supportNumberTextView.setOnClickListener {
                    activity.callRequester(serviceRequest.support_contact)
                }
            }

        } else {
            navigateTextView.isVisible = activeServiceRequest?.location_0 != null
            navigateButtonImage.isVisible = activeServiceRequest?.location_0 != null
        }

        /**
         * Change in Code - Nitheesh.
         * Added new fields
         */
        if (!serviceRequest.patient_name.isNullOrEmpty()) {
            patientNameLabelTextView.isVisible = true
            patientNameTextView.isVisible = true
            patientNameTextView.text = serviceRequest.patient_name!!.trim()
        }

        if (!serviceRequest.medical_info.isNullOrEmpty()) {
            doctorNoteLabelTextView.isVisible = true
            doctorNoteTextView.isVisible = true
            doctorNoteTextView.text = serviceRequest.medical_info!!.trim()
        }


        completeTextView.setOnClickListener {
            progressDialog?.show()
            serviceRequest.logd()
            if (completeTextView.text == context.getString(R.string.complete)) {
                completeCurrentTrip { error ->
                    progressDialog?.hide()
                    if (!error) {
                        activeServiceRequest = null
                        prefs.activeRideID = null
                        dismiss()
                        ThanksDialog(activity) {}.show()
                    } else {
                        dismiss()
                        activity.fetchAllRequests { }
                    }
                }
            } else {
                completePickUp { error ->
                    progressDialog?.hide()
                    if (!error) {
                        activeServiceRequest?.picked_up = true
                        completeTextView.text = context.getString(R.string.complete)
                        navigateTextView.isVisible = activeServiceRequest?.location_1 != null
                        navigateButtonImage.isVisible = activeServiceRequest?.location_1 != null

                        /**
                         * Change in Code - Nitheesh.
                         * Added new fields
                         */
                        //show destination number and support number and call = dial destination number, call visibility
                        if (!serviceRequest.destination_contact.isNullOrEmpty() && activeServiceRequest!!.picked_up) {
                            callTextView.isVisible = true
                            callButtonImage.isVisible = true
                            destinationNumberLabelTextView.isVisible = true
                            destinationCallButtonImage.isVisible = true
                            destinationNumberTextView.isVisible = true
                            destinationNumberTextView.text = serviceRequest.destination_contact
                            destinationCallButtonImage.setOnClickListener {
                                activity.callRequester(serviceRequest.destination_contact)
                            }
                            destinationNumberTextView.setOnClickListener {
                                activity.callRequester(serviceRequest.destination_contact)
                            }
                        }

                        if (!serviceRequest.support_contact.isNullOrEmpty() && activeServiceRequest!!.picked_up) {
                            supportNumberLabelTextView.isVisible = true
                            supportCallButtonImage.isVisible = true
                            supportNumberTextView.isVisible = true
                            supportNumberTextView.text = serviceRequest.support_contact
                            supportCallButtonImage.setOnClickListener {
                                activity.callRequester(serviceRequest.support_contact)
                            }
                            supportNumberTextView.setOnClickListener {
                                activity.callRequester(serviceRequest.support_contact)
                            }
                        }
                    } else {
                        dismiss()
                        activity.fetchAllRequests { }
                    }
                }
            }
        }

        navigateButtonImage.setOnClickListener {
            activity.launchMapsIntent(
                activeServiceRequest ?: serviceRequest
            )
        }
        navigateTextView.setOnClickListener {
            activity.launchMapsIntent(
                activeServiceRequest ?: serviceRequest
            )
        }


        callTextView.isVisible = !serviceRequest.support_contact.isNullOrEmpty()
        callButtonImage.isVisible = !serviceRequest.support_contact.isNullOrEmpty()
        callButtonImage.setOnClickListener {
            if (activeServiceRequest!!.picked_up && !serviceRequest.destination_contact.isNullOrEmpty())
                activity.callRequester(serviceRequest.destination_contact)
            else
                activity.callRequester(serviceRequest.support_contact)

        }
        callTextView.setOnClickListener {
            if (activeServiceRequest!!.picked_up && !serviceRequest.destination_contact.isNullOrEmpty())
                activity.callRequester(serviceRequest.destination_contact)
            else
                activity.callRequester(serviceRequest.support_contact)
        }

    }

    @SuppressLint("MissingPermission")
    private fun completeCurrentTrip(callback: (error: Boolean) -> Unit) {

        //getting current location
        if (!activity.checkGpsStatus()) {
            activity.requestLocationServices()
            return
        }

        LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener {
            val location = try {
                "${it.latitude},${it.longitude}"
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                null
            }
            if (location.isNullOrEmpty()) {
                activity.toast(activity.getString(R.string.unable_to_fetch_location))
                return@addOnSuccessListener
            }

            GlobalScope.launch {
                when (
                    val response = withContext(Dispatchers.Default) { endCurrentTrip(location) }
                    ) {
                    is ResultWrapper.Success -> {
                        context.runOnUiThread {
                            callback(false)
                            response.logd("${Constants.TAG}_PCS")
                        }
                    }
                    is ResultWrapper.GenericError -> {
                        if (response.error.error == "You do not have any active services.") {
                            context.runOnUiThread {
                                callback(false)
                            }
                            response.logd("${Constants.TAG}_PCS2")
                        } else {
                            activity.handleGenericError(response)
                            context.runOnUiThread {
                                callback(true)
                            }
                            response.logd("${Constants.TAG}_PCF1")
                        }
                    }
                    is ResultWrapper.NetworkError -> {
                        context.handleNetworkError()
                        context.runOnUiThread {
                            callback(true)
                        }
                        response.logd("${Constants.TAG}_PCF2")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun completePickUp(callback: (error: Boolean) -> Unit) {

        //getting current location
        if (!activity.checkGpsStatus()) {
            activity.requestLocationServices()
            return
        }

        LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener {
            val location = try {
                "${it.latitude},${it.longitude}"
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                null
            }
            if (location.isNullOrEmpty()) {
                activity.toast(activity.getString(R.string.unable_to_fetch_location))
                return@addOnSuccessListener
            }
            GlobalScope.launch {
                when (
                    val response = withContext(Dispatchers.Default) { pickUpCurrentTrip(location) }
                    ) {
                    is ResultWrapper.Success -> {
                        context.runOnUiThread {
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

}