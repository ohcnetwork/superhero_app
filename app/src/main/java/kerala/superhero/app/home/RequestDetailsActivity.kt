package kerala.superhero.app.home

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_request_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kerala.superhero.app.R
import kerala.superhero.app.activeServiceRequest
import kerala.superhero.app.data.DeliveryDestination
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.isHomeActivityOrRequestDetailsActivityOpen
import kerala.superhero.app.location_tracker.LocationUpdatesService
import kerala.superhero.app.network.endCurrentTrip
import kerala.superhero.app.prefs
import kerala.superhero.app.utils.*
import kerala.superhero.app.views.ProgressDialog
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast

class RequestDetailsActivity : AppCompatActivity() {

    class DestinationAddressViewHolder(
        rootView: View,
        private val context: RequestDetailsActivity
    ) : RecyclerView.ViewHolder(rootView) {
        private val nameTextView = rootView.findViewById<TextView>(R.id.nameTextView)
        private val addressTextView = rootView.findViewById<TextView>(R.id.addressTextView)
        private val deliveryDetailsButton =
            rootView.findViewById<TextView>(R.id.destinationActionButton)

        fun bind(destination: DeliveryDestination) {
            nameTextView.text = destination.name
            addressTextView.text = destination.address
            deliveryDetailsButton.text =
                context.getString(if (destination.delivered) R.string.delivered else R.string.delivery_details)
            deliveryDetailsButton.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (destination.delivered) R.color.disabled else R.color.button_primary
                )
            )
            deliveryDetailsButton.setOnClickListener {
                if (!destination.delivered) {
                    DestinationDetailsDialog(context, destination) {
                        context.refreshList()
                    }.show()
                }
            }
        }

    }

    class LandmarkListViewHolder(rootView: View, private val context: RequestDetailsActivity) :
        RecyclerView.ViewHolder(rootView) {
        private val destinationAddressRecyclerView =
            rootView.findViewById<RecyclerView>(R.id.destinationsRecyclerView)
        private val navigateButton = rootView.findViewById<TextView>(R.id.navigateTextView)
        private val navigateButtonImage = rootView.findViewById<ImageView>(R.id.navigateButtonImage)
        private val landmarkTextView = rootView.findViewById<TextView>(R.id.landmarkTextView)

        fun bind(groupList: List<DeliveryDestination>) {
            navigateButton.setOnClickListener {
                launchMapsIntent(groupList[0])
            }
            navigateButtonImage.setOnClickListener {
                launchMapsIntent(groupList[0])
            }
            landmarkTextView.text = groupList[0].landmark_address

            destinationAddressRecyclerView.layoutManager = LinearLayoutManager(context)
            destinationAddressRecyclerView.adapter =
                object : RecyclerView.Adapter<DestinationAddressViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): DestinationAddressViewHolder {
                        val layoutInflater: LayoutInflater =
                            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        return DestinationAddressViewHolder(
                            layoutInflater.inflate(
                                R.layout.destinations_list_item,
                                parent,
                                false
                            ),
                            context
                        )
                    }

                    override fun getItemCount(): Int = groupList.size

                    override fun onBindViewHolder(
                        holder: DestinationAddressViewHolder,
                        position: Int
                    ) {
                        holder.bind(groupList[position])
                    }

                }

        }

        private fun launchMapsIntent(deliveryDestination: DeliveryDestination) {
            try {
                val gmmIntentUri: Uri =
                    Uri.parse("geo:0,0?q=${if (deliveryDestination.landmark_location != null) "${deliveryDestination.landmark_location.coordinates[0]},${deliveryDestination.landmark_location.coordinates[1]}" else "0,0"}}(${deliveryDestination.landmark_address})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                context.startActivity(mapIntent)
            } catch (e: ActivityNotFoundException) {
                context.toast(context.getString(R.string.navigation_failed_reason))
            }
        }

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
                    ThanksDialog(this@RequestDetailsActivity) {
                        finish()
                    }.show()
                    dialog.dismiss()
                }
            }.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_details)

        collectionPointTextView.text =
            getString(R.string.pickup_point, activeServiceRequest?.address_0)
        callButtonImage.isVisible = !activeServiceRequest?.support_contact.isNullOrEmpty()
        callTextView.isVisible = !activeServiceRequest?.support_contact.isNullOrEmpty()

        callTextView.setOnClickListener {
            callRequester(activeServiceRequest?.support_contact)
        }
        callButtonImage.setOnClickListener {
            callRequester(activeServiceRequest?.support_contact)
        }

        navigateButtonImage.setOnClickListener {
            activeServiceRequest?.run {
                launchMapsIntent(
                    this
                )
            }
        }
        navigateTextView.setOnClickListener {
            activeServiceRequest?.run {
                launchMapsIntent(
                    this
                )
            }
        }

        refreshList()

    }

    private fun refreshList() {

        if (activeServiceRequest?.destination_addresses?.filter { it.delivered }?.size == activeServiceRequest?.destination_addresses?.size) {
            completeThisDeliverySeries()
        }

        val groupedList =
            activeServiceRequest?.destination_addresses?.groupBy { it.landmark_address }
                ?.map { it.value }

        if (!groupedList.isNullOrEmpty()) {
            landmarksRecyclerView.layoutManager = LinearLayoutManager(this)
            landmarksRecyclerView.adapter =
                object : RecyclerView.Adapter<LandmarkListViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): LandmarkListViewHolder {
                        val layoutInflater: LayoutInflater =
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        return LandmarkListViewHolder(
                            layoutInflater.inflate(
                                R.layout.landmark_list_item,
                                parent,
                                false
                            ),
                            this@RequestDetailsActivity
                        )
                    }

                    override fun getItemCount(): Int = groupedList.size

                    override fun onBindViewHolder(holder: LandmarkListViewHolder, position: Int) {
                        holder.bind(groupedList[position])
                    }

                }
        } else {
            toast("No delivery addresses found")
        }
    }

    @SuppressLint("MissingPermission")
    private fun completeThisDeliverySeries() {

        if (!checkGpsStatus()) {
            requestLocationServices()
            return
        }
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
            val location = try {
                "${it.latitude},${it.longitude}"
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                null
            }
            if (location.isNullOrEmpty()) {
                toast(getString(R.string.unable_to_fetch_location))
                return@addOnSuccessListener
            }
            val progressDialog = ProgressDialog(this@RequestDetailsActivity)
            progressDialog.show()
            GlobalScope.launch {
                when (val response = withContext(Dispatchers.Default) { endCurrentTrip(location) }) {
                    is ResultWrapper.Success -> runOnUiThread {
                        progressDialog.dismiss()
                        ThanksDialog(this@RequestDetailsActivity) {
                            Intent(
                                this@RequestDetailsActivity,
                                HomeActivity::class.java
                            ).let(::startActivity)
                            finishAffinity()
                        }.show()
                        activeServiceRequest = null
                    }
                    is ResultWrapper.GenericError -> this@RequestDetailsActivity.handleGenericError(
                        response
                    )
                    is ResultWrapper.NetworkError -> handleNetworkError()
                }
                runOnUiThread {
                    progressDialog.dismiss()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isHomeActivityOrRequestDetailsActivityOpen = true
        Intent(this, LocationUpdatesService::class.java).let(::stopService)
        registerReceiver(
            rideCancelledReceiver,
            IntentFilter("kerala.superhero.app.watcher.location_tracker.action.RIDE_CANCELLED")
        )
    }

    override fun onPause() {
        super.onPause()
        isHomeActivityOrRequestDetailsActivityOpen = false
        val serviceIntent = Intent(this, LocationUpdatesService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        unregisterReceiver(rideCancelledReceiver)
    }

}
