package kerala.superhero.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import pub.devrel.easypermissions.EasyPermissions
import kerala.superhero.app.Constants.Companion.REQUEST_PERMISSIONS_REQUEST_CODE

object PermissionsUtility {

    fun hasLocationPermissions(context: Context) =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
}

fun Activity.requestLocationPermissions() {
    if (PermissionsUtility.hasLocationPermissions(this)) {
        return
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

        EasyPermissions.requestPermissions(
            this,
            "You need to accept location permissions to use this app.",
            REQUEST_PERMISSIONS_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        EasyPermissions.requestPermissions(
            this,
            "You need to accept location permissions to use this app.",
            REQUEST_PERMISSIONS_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

    }
}