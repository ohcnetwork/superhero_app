package kerala.superhero.app.network

import kerala.superhero.app.Constants
import kerala.superhero.app.data.*
import kerala.superhero.app.utils.logd
import retrofit2.HttpException
import retrofit2.http.Body
import java.io.IOException

suspend fun login(phone: String, password: String): ResultWrapper<AuthAPIResponse.Result> {
    return safeApiCall {
        APIService.create().loginAsync(phone, password).await()
    }
}

suspend fun updateFCMToken(token: String): ResultWrapper<GenericUpdateResponse.Result> {
    return safeApiCall {
        APIService.create().updateFCMTokenAsync(token).await()
    }
}

/**
 * @author Nitheesh Ag
 */
suspend fun acceptARequest(
    requestId: String,
    location: String
): ResultWrapper<GenericUpdateResponse.Result> {
    return safeApiCall {
        APIService.create().acceptARequestAsync(requestId, location).await()
    }
}

suspend fun pickUpCurrentTrip(location: String): ResultWrapper<GenericUpdateResponse.Result> {
    return safeApiCall {
        APIService.create().pickUpCurrentTripAsync(location).await()
    }
}

suspend fun endCurrentTrip(location: String): ResultWrapper<GenericUpdateResponse.Result> {
    return safeApiCall {
        APIService.create().endCurrentTripAsync(location).await()
    }
}

suspend fun getAllAssetGroups(): ResultWrapper<GroupsListAPIResponse.Result> {
    return safeApiCall {
        APIService.create().getAllAssetGroupsAsync().await()
    }
}

suspend fun getAllDistricts(): ResultWrapper<DistrictsListAPIResponse.Result> {
    return safeApiCall {
        APIService.create().getAllDistrictsAsync().await()
    }
}

suspend fun getAllPanchayathsInDistrict(district: String): ResultWrapper<PanchayathsListAPIResponse.Result> {
    return safeApiCall {
        APIService.create().getAllPanchayathsAsync(district).await()
    }
}

suspend fun getAllStates(): ResultWrapper<StatesListAPIResponse.Result> {
    return safeApiCall {
        APIService.create().getAllStatesAsync().await()
    }
}

//suspend fun signUp(
//    name: String,
//    phone: String,
//    email: String,
//    password: String,
//    reg_no: String,
//    asset_category: Int,
//    asset_group: Int,
//    state: String,
//    secondary_phone: String?,
//    category: Int? = null,
//    role: String = "Ambulance Driver"
//): ResultWrapper<AuthAPIResponse.Result> {
//    return safeApiCall {
//        APIService.create().signUpAsync(
//            name,
//            phone,
//            email,
//            password,
//            reg_no,
//            asset_category,
//            asset_group,
//            state,
//            secondary_phone,
//            category,
//            role
//        ).await()
//    }
//}


suspend fun getUserProfile(): ResultWrapper<ProfileAPIResponse.Result> {
    return safeApiCall {
        APIService.create().getUserProfileAsync().await()
    }
}

suspend fun updateLocationAsync(location: List<Double>): ResultWrapper<LocationUpdateResponse.Result> {
    return safeApiCall {
        APIService.create().updateLocationAsync(location).await()
    }
}

suspend fun getAllServiceRequests(): ResultWrapper<ServiceRequestsAPIResponse.Result> {
    return safeApiCall {
        APIService.create().getAllRequestsAsync().await()
    }
}

suspend fun markDeliveryComplete(
    id: String,
    location: String
): ResultWrapper<CurrentServiceAPIResponse.Result> {
    return safeApiCall {
        APIService.create().markDeliveryCompleteAsync(id, location).await()
    }
}

suspend fun sendOTP(phone: String): ResultWrapper<OTPRequestResponse.Result> {
    return safeApiCall {
        APIService.create(Constants.OTP_SERVER_URL).requestOTPAsync(phone).await()
    }
}

//part of on boarding
suspend fun verifyOTP(
    phone: String,
    otp: String,
    token: String
): ResultWrapper<OTPVerifyResponse.Result> {
    return safeApiCall {
        APIService.create(Constants.OTP_SERVER_URL).requestOTPVerifyAsync(phone, otp, token).await()
    }
}

suspend fun resetPassword(
    phone: String,
    password: String,
    otp: String,
    token: String
): ResultWrapper<ResetPasswordResponse.Result> {
    return safeApiCall {
        APIService.create().resetPasswordAsync(phone, password, otp, token).await()
    }
}

suspend fun signUp(
    name: String,
    phone: String,
    email: String,
    password: String,
    reg_no: String,
    asset_category: Int,
    asset_group: Int,
    state: String,
    district: String,
    plb: String,
    ward: String,
    secondary_phone: String?,
    category: Int? = null,
    role: String = "Ambulance Driver"
): ResultWrapper<AuthAPIResponse.Result> {
    return safeApiCall {
        APIService.create().signUpAsync(
            name,
            phone,
            email,
            password,
            reg_no,
            asset_category,
            asset_group,
            state,
            district,
            plb,
            ward,
            secondary_phone,
            category,
            role
        ).await()
    }
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ResultWrapper<T> {
    return try {
        ResultWrapper.Success(apiCall.invoke())
    } catch (throwable: Throwable) {
        throwable.stackTrace.logd()
        throwable.printStackTrace()
        when (throwable) {
            is IOException -> ResultWrapper.NetworkError
            is HttpException -> {
                val code = throwable.code()
                val errorResponse = APIService.convertErrorBody(throwable)
                ResultWrapper.GenericError(code, errorResponse ?: defaultErrorResponse)
            }
            else -> ResultWrapper.GenericError()
        }
    }
}