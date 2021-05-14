package kerala.superhero.app.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import kerala.superhero.app.*
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.data.ServiceRequest
import kerala.superhero.app.home.service_request.ServiceRequestsAdapter
import kerala.superhero.app.location_tracker.LocationServiceStatusMonitor
import kerala.superhero.app.location_tracker.LocationUpdatesService
import kerala.superhero.app.location_tracker.LocationUpdatesService.LocalBinder
import kerala.superhero.app.network.*
import kerala.superhero.app.utils.*
import kerala.superhero.app.views.ProgressDialog
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit


class HomeActivity : AppCompatActivity() {

    private val tag0 = "networkIssueD"
    private var progressDialog: ProgressDialog? = null

    // A reference to the service used to get location updates.
    private var mService: LocationUpdatesService? = null

    // Tracks the bound state of the service.
    private var mBound = false

    // Monitors the state of the connection to the service.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocalBinder
            mService = binder.getService()
            requestLocation()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }


    override fun onDestroy() {
        progressDialog?.dismiss()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("cancelled", false) == true) {
            alert {
                title = getString(R.string.ride_cancelled)
                this.message = getString(R.string.ride_cancelled_message)
                isCancelable = false
                positiveButton(R.string.ok) { dialog ->
                    activeServiceRequest = null
                    prefs.activeRideID = null
                    activeRideDialog?.dismiss()
                    ThanksDialog(this@HomeActivity) {
                    }.show()
                    dialog.dismiss()
                }
            }.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful)
                GlobalScope.launch {
                    withContext(Dispatchers.Default) {
                        updateFCMToken(
                            task.result ?: ""
                        )
                    }.logd()
                }
        }

        lastLocationUpdate.observe(this as LifecycleOwner, Observer {
            lastLocationUpdateTextView.text =
                getString(R.string.last_location_update, prefs.lastLocationUpdate)
        })

        refreshImageView.setOnClickListener {
            getOneShotLocation()
        }

        lastLocationUpdateTextView.setOnClickListener {
            getOneShotLocation()
        }

        locationServiceButton.setOnClickListener {
            try {
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).let(::startActivity)
            } catch (e: ActivityNotFoundException) {
                toast(getString(R.string.open_settings_manually))
            }
        }

        cellularServiceButton.setOnClickListener {
            try {
                Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).let(::startActivity)
            } catch (e: ActivityNotFoundException) {
                try {
                    Intent(Settings.ACTION_SETTINGS).let(::startActivity)
                } catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.open_settings_manually))
                }
            }
        }

        progressDialog = ProgressDialog(this).apply {
            message = getString(R.string.loading_requests)
        }
        progressDialog?.show()

        fetchAllRequests {
            progressDialog?.hide()
        }

        serviceRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        serviceRequests.observe(this as LifecycleOwner, { list ->
            if (list.isNotEmpty()) {
                onBoardingInstructionsLayout.visibility = View.GONE
                serviceRequestsRecyclerView.visibility = View.VISIBLE
            }
            if (activeServiceRequest != null) {
                showRequestConfirmedDialog(activeServiceRequest!!)
            }
            serviceRequestsRecyclerView.adapter = ServiceRequestsAdapter(this, list) { request ->
                alert {
                    title = getString(R.string.confirmation_dialog_title)
                    message = getString(R.string.confirmation_dialog_content)
                    positiveButton(getString(R.string.confirm)) {
                        Log.d(tag0, "accepted request button clicked")
                        acceptRequest(request)
                        it.dismiss()
                    }
                    negativeButton(getString(R.string.cancel)) {
                        it.dismiss()
                    }
                }.show()
            }
        })

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )

        val locationUpdateServiceMonitorWR =
            PeriodicWorkRequestBuilder<LocationServiceStatusMonitor>(15, TimeUnit.MINUTES).apply {
                addTag("locationServiceMonitor")
            }.build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "locationServiceMonitor",
            ExistingPeriodicWorkPolicy.KEEP,
            locationUpdateServiceMonitorWR
        )

    }

    @SuppressLint("MissingPermission")
    private fun getOneShotLocation() {
//        if (!PermissionsUtility.hasLocationPermissions(this)) {
//            requestLocationPermissions()
//            return
//        }

        if (!checkGpsStatus()) {
            requestLocationServices()
            return
        }

        checkPermissionsAndDo(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            getString(R.string.permission_required)
        ) {
            val loaderDialog = UpdatingLocationDialog(this)
            loaderDialog.show()

            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
                val locationList = try {
                    listOf(it.latitude, it.longitude)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                    null
                }
                if (locationList.isNullOrEmpty()) {
                    toast(getString(R.string.unable_to_fetch_location))
                    loaderDialog.dismiss()
                    return@addOnSuccessListener
                }
                GlobalScope.launch {
                    when (
                        val response = withContext(Dispatchers.Default) {
                            updateLocationAsync(locationList)
                        }
                    ) {
                        is ResultWrapper.Success -> {
                            runOnUiThread {
                                toast(getString(R.string.updated_location_successfully))
                                updateLastLocationUpdatedTime()
                            }
                        }
                        is ResultWrapper.NetworkError -> {
                            handleNetworkError()
                        }
                        is ResultWrapper.GenericError -> {
                            handleGenericError(response)
                        }
                    }
                    runOnUiThread {
                        loaderDialog.dismiss()
                    }
                }

            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun acceptRequest(request: ServiceRequest) {

//        Log.d(tag0, prefs.accessToken.toString())
        Log.d(tag0, "fired acceptRequest function")

        progressDialog?.message = getString(R.string.confirming_request_status)
        progressDialog?.show()

        //getting current location

        if (!checkGpsStatus()) {
            requestLocationServices()
            return
        }
        checkPermissionsAndDo(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            getString(R.string.permission_required)
        ) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
                val location = try {
                    "${it.latitude},${it.longitude}"
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                    null
                }
                if (location.isNullOrEmpty()) {
                    toast(getString(R.string.unable_to_fetch_location))
                    progressDialog?.hide()
                    return@addOnSuccessListener
                }


                Log.d(tag0, "firing acceptARequest API call")
                GlobalScope.launch {
                    when (val response =
                        withContext(Dispatchers.Default) {
                            acceptARequest(request.id, location)
                        }) {
                        is ResultWrapper.Success -> {
                            Log.d(tag0, "ResultWrapper.Success")
                            Log.d(tag0, "$response")
                            runOnUiThread {
                                progressDialog?.message = getString(R.string.loading_requests)
                                fetchAllRequests {
                                    progressDialog?.hide()
                                    showRequestConfirmedDialog(request)
                                }
                            }
                        }
                        is ResultWrapper.GenericError -> {
                            Log.d(tag0, "ResultWrapper.GenericError")
                            Log.d(tag0, "$response")
                            runOnUiThread {
                                progressDialog?.message = getString(R.string.loading_requests)
                                fetchAllRequests {
                                    progressDialog?.hide()
                                }
                            }
                            handleGenericError(response)
                        }
                        is ResultWrapper.NetworkError -> {
                            Log.d(tag0, "ResultWrapper.NetworkError")
                            Log.d(tag0, "$response")
                            runOnUiThread {
                                progressDialog?.hide()
                            }
                            handleNetworkError()
                        }
                    }
                }
            }
        }

    }

    var isDialogOpen = false
    var activeRideDialog: ActiveRideDialog? = null

    private fun showRequestConfirmedDialog(request: ServiceRequest) {
        activeServiceRequest = request
        if (!isDialogOpen) {
            isDialogOpen = true
            if (userProfile?.assetDetails?.categoryDetails?.group == 1) {
                Intent(this, RequestDetailsActivity::class.java).let(::startActivity)
                finish()
            } else {
                activeRideDialog = ActiveRideDialog(this, request)
                activeRideDialog?.show()
            }
        }
    }

    fun fetchAllRequests(callback: () -> Unit) {
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) { getUserProfile() }
            ) {
                is ResultWrapper.Success -> {
                    userProfile = response.value.data
                    activeServiceRequest = userProfile?.activeServiceDetails
                }
                is ResultWrapper.GenericError -> handleGenericError(response)
                is ResultWrapper.NetworkError -> handleNetworkError()
            }
            when (
                val response = withContext(Dispatchers.Default) { getAllServiceRequests() }
            ) {
                is ResultWrapper.Success -> runOnUiThread {
                    serviceRequests.value = response.value.data
                }
                is ResultWrapper.GenericError -> handleGenericError(response)
                is ResultWrapper.NetworkError -> handleNetworkError()
            }
            runOnUiThread {
                callback()
            }
        }
    }

    fun requestLocation() {
        if (!checkGpsStatus()) {
            requestLocationServices()
            return
        }
        checkPermissionsAndDo(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            getString(R.string.permission_required)
        ) {
            mService?.requestLocationUpdates()
        }
    }

    override fun onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }

    private val rideCancelledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            alert {
                title = getString(R.string.ride_cancelled)
                this.message = getString(R.string.ride_cancelled_message)
                isCancelable = false
                positiveButton(R.string.ok) { dialog ->
                    activeServiceRequest = null
                    prefs.activeRideID = null
                    activeRideDialog?.dismiss()
                    ThanksDialog(this@HomeActivity) {
                        progressDialog?.show()
                        progressDialog?.message = getString(R.string.loading_requests)
                        fetchAllRequests {
                            progressDialog?.hide()
                        }
                    }.show()
                    dialog.dismiss()
                }
            }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (intent.getBooleanExtra("cancelled", false) && activeRideDialog?.isShowing == true) {
            onNewIntent(intent)
        }
        isHomeActivityOpen = true
        isHomeActivityOrRequestDetailsActivityOpen = true
        if (!checkGpsStatus()) {
            requestLocationServices()
        } else {
            // Check if the user revoked runtime permissions.
            checkPermissionsAndDo(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                getString(R.string.permission_required)
            ) {

            }
        }
        registerReceiver(
            rideCancelledReceiver,
            IntentFilter("kerala.superhero.app.watcher.location_tracker.action.RIDE_CANCELLED")
        )
    }

    override fun onPause() {
        super.onPause()
        isHomeActivityOpen = false
        isHomeActivityOrRequestDetailsActivityOpen = false
        unregisterReceiver(rideCancelledReceiver)
    }


}
