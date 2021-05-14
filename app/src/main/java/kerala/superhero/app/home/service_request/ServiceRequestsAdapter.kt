package kerala.superhero.app.home.service_request

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kerala.superhero.app.R
import kerala.superhero.app.data.ServiceRequest
import kerala.superhero.app.userProfile

class ServiceRequestsAdapter(
    private val context: Context,
    private val serviceRequests: List<ServiceRequest>,
    val acceptRequest: (request: ServiceRequest) -> Unit
) : RecyclerView.Adapter<ServiceRequestsAdapter.ViewHolder>() {

    class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

        private val pickupLabelTextView = rootView.findViewById<TextView>(R.id.pickupLabelTextView)
        private val destinationLabelTextView =
            rootView.findViewById<TextView>(R.id.destinationLabelTextView)
        private val pickupAddressTextView =
            rootView.findViewById<TextView>(R.id.pickupAddressTextView)
        private val destinationAddressTextView =
            rootView.findViewById<TextView>(R.id.destinationAddressTextView)
        private val timeTextView = rootView.findViewById<TextView>(R.id.timeTextView)

        private val acceptButton = rootView.findViewById<TextView>(R.id.acceptButton)
        private val greenDotImageView = rootView.findViewById<View>(R.id.greenDotImageView)

        /**
         * Change in Code - Nitheesh.
         * Added new fields
         */
        private val patientNameLabelTextView =
            rootView.findViewById<TextView>(R.id.patientNameLabelTextView)
        private val patientNameTextView = rootView.findViewById<TextView>(R.id.patientNameTextView)
        private val doctorNoteLabelTextView =
            rootView.findViewById<TextView>(R.id.doctorNoteLabelTextView)
        private val doctorNoteTextView = rootView.findViewById<TextView>(R.id.doctorNoteTextView)


        fun bind(
            context: Context,
            serviceRequest: ServiceRequest,
            acceptRequest: (request: ServiceRequest) -> Unit
        ) {
            pickupLabelTextView.text =
                context.getString(if (userProfile?.assetDetails?.categoryDetails?.group == 0) R.string.pickup else R.string.collection_point)
            destinationAddressTextView.isVisible =
                userProfile?.assetDetails?.categoryDetails?.group == 0
            destinationLabelTextView.isVisible =
                userProfile?.assetDetails?.categoryDetails?.group == 0
            pickupAddressTextView.text = serviceRequest.address_0
            destinationAddressTextView.text =
                if (serviceRequest.address_1.isNullOrEmpty()) context.getString(R.string.will_be_informed_on_arrival) else serviceRequest.address_1

            val date = serviceRequest.getParsedDate()

            timeTextView.text = date

            greenDotImageView.isVisible = serviceRequest.active
            acceptButton.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (serviceRequest.active && userProfile?.service_history_ids?.contains(
                            serviceRequest.id
                        ) == false
                    ) R.color.button_primary else R.color.disabled
                )
            )

            if (!serviceRequest.active) {
                acceptButton.text = context.getString(R.string.expired)
                acceptButton.isEnabled = false
                greenDotImageView.visibility = View.INVISIBLE
            } else {
                acceptButton.text = context.getString(R.string.accept)
                acceptButton.isEnabled = true
                greenDotImageView.visibility = View.VISIBLE
                if (userProfile?.service_history_ids?.contains(serviceRequest.id) == true) {
                    acceptButton.text = context.getString(R.string.served_request)
                    acceptButton.isEnabled = false
                    greenDotImageView.visibility = View.INVISIBLE
                }
            }

            acceptButton.setOnClickListener {
                acceptRequest(serviceRequest)
            }

            /**
             * Change in Code - Nitheesh.
             * Added new fields
             */
            if (userProfile?.assetDetails?.categoryDetails?.group == 0) {
                if (!serviceRequest.patient_name.isNullOrEmpty()) {
                    patientNameLabelTextView.isVisible = true
                    patientNameTextView.isVisible = true
                    patientNameTextView.text = serviceRequest.patient_name
                }

                if (!serviceRequest.medical_info.isNullOrEmpty()) {
                    doctorNoteLabelTextView.isVisible = true
                    doctorNoteTextView.isVisible = true
                    doctorNoteTextView.text = serviceRequest.medical_info
                }
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return ViewHolder(
            layoutInflater.inflate(
                R.layout.request_card,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = serviceRequests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(context, serviceRequests[position], acceptRequest)
    }
}