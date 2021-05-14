package kerala.superhero.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.text.isDigitsOnly
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kerala.superhero.app.BuildConfig
import org.jetbrains.anko.alert
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import kerala.superhero.app.Constants
import kerala.superhero.app.R
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.data.ServiceRequest
import kerala.superhero.app.onboarding.OnBoardingActivity
import kerala.superhero.app.prefs

fun Any?.logd(key: String = Constants.TAG) =
    if (BuildConfig.DEBUG) Log.d(key, this.toString()) else Log.d(
        key,
        "Logging is prohibited in production"
    )

fun Editable.isAValidIndianPhoneNumber() = isDigitsOnly() && length == 10

fun Activity.checkGpsStatus(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}


@SuppressLint("MissingPermission")
fun Activity.callRequester(supportContact: String?) {
    if (hasCallPermission()) {
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$supportContact")))
    } else {
        requestCallPhonePermission()
        toast(getString(R.string.call_phone_permission_required))
    }
}

fun Activity.launchMapsIntent(serviceRequest: ServiceRequest, ignoreDestination: Boolean = false) {
    try {
        val destinationAddress =
            if (ignoreDestination || serviceRequest.picked_up) serviceRequest.address_1 else serviceRequest.address_0
        val destinationLocation =
            if (ignoreDestination || serviceRequest.picked_up) serviceRequest.location_1 else serviceRequest.location_0
        val gmmIntentUri: Uri =
            Uri.parse("geo:0,0?q=${if (destinationLocation != null) "${destinationLocation.coordinates[0]},${destinationLocation.coordinates[1]}" else "0,0"}(${destinationAddress})")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    } catch (e: ActivityNotFoundException) {
        toast(getString(R.string.navigation_failed_reason))
    }
}

fun Activity.requestLocationServices() {
    alert {
        title = getString(R.string.location_off)
        message = getString(R.string.location_off_description)
        isCancelable = false
        positiveButton("Turn On Location") {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).let(::startActivity)
        }
    }.show()
}

fun Context?.handleNetworkError(callback: (() -> Unit)? = null) {
    this?.runOnUiThread {
        try {
            alert {
                title = getString(R.string.error)
                message = getString(R.string.network_error)
                isCancelable = false
                positiveButton(if (callback != null) getString(R.string.retry) else getString(R.string.ok)) {
                    it.dismiss()
                    callback?.invoke()
                }
            }.show()
        } catch (e: WindowManager.BadTokenException) {
            // Ignored since source of error is known and handled
        }
    }
}

fun Activity.requestCallPhonePermission() {
    val permissionsArray = mutableListOf(Manifest.permission.CALL_PHONE)
    val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.CALL_PHONE
    )

    if (!shouldProvideRationale) {
        ActivityCompat.requestPermissions(
            this,
            permissionsArray.toTypedArray(),
            299
        )
    }
}

fun Activity.hasCallPermission(): Boolean {
    val permissionState = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.CALL_PHONE
    )
    return permissionState == PackageManager.PERMISSION_GRANTED
}

fun Activity?.handleGenericError(
    error: ResultWrapper.GenericError,
    ignore401: Boolean = false,
    callback: (() -> Unit)? = null
) {

    if (error.code == 401 && !ignore401) {
        handleSessionExpiredResponse()
        return
    }

    error.logd()

    this?.runOnUiThread {
        try {
            alert {
                title = getString(R.string.error)
                message = error.error.error
                isCancelable = false
                positiveButton(if (callback != null) getString(R.string.retry) else getString(R.string.ok)) {
                    it.dismiss()
                    callback?.invoke()
                }
            }.show()
        } catch (e: WindowManager.BadTokenException) {
            // Ignored since source of error is known and handled
        }
    }
}

fun Activity?.handleSessionExpiredResponse() {
    this?.runOnUiThread {
        try {
            alert {
                also {
                    setTheme(android.R.style.ThemeOverlay_Material_Dark)
                }
                message = getString(R.string.session_expired)
                isCancelable = false
                positiveButton(getString(R.string.ok)) {
                    it.dismiss()
                    logout()
                }
            }.show()
        } catch (e: WindowManager.BadTokenException) {
            // Ignored since source of error is known and handled
        }
    }
}

fun Activity.logout() {
    prefs.clear()
    Intent(this, OnBoardingActivity::class.java).let(::startActivity)
    finishAffinity()
}


inline fun Activity.checkPermissionsAndDo(
    permissions: List<String>,
    rationale: String,
    crossinline callback: (message: String) -> Unit
) {
    Dexter.withActivity(this).withPermissions(permissions)
        .withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                report?.run {
                    if (areAllPermissionsGranted()) {
                        callback("")
                    } else {
                        val alert = AlertDialog.Builder(this@checkPermissionsAndDo)
                        alert.setTitle(R.string.app_name)
                            .setMessage(rationale)
                            .setCancelable(false)
                            .setPositiveButton(R.string.grant) { dialog, _ ->
                                finishAffinity()
                                openAppSettings()
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                    }
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                MaterialAlertDialogBuilder(this@checkPermissionsAndDo).setTitle(getString(R.string.app_name))
                    .setMessage(rationale)
                    .setPositiveButton(getString(R.string.grant)) { dialog, _ ->
//                        if (token == null) {
                        openAppSettings()
//                        } else {
//                            token.continuePermissionRequest()
//                        }
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()

            }
        }).check()
}

fun Activity.openAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts(
        "package",
        BuildConfig.APPLICATION_ID, null
    )
    intent.data = uri
//    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}