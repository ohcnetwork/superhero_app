package kerala.superhero.app.data

import java.io.Serializable

sealed class ResultWrapper<out T> {
    data class Success<out T>(val value: T) : ResultWrapper<T>()
    data class GenericError(
        val code: Int = 400,
        val error: ErrorResponse = defaultErrorResponse
    ) : ResultWrapper<Nothing>()

    object NetworkError : ResultWrapper<Nothing>()
}

object AuthAPIResponse {
    data class Result(val status: Int, val data: AccessTokenResponse)
    data class AccessTokenResponse(val access_token: String)
}

object ServiceRequestsAPIResponse {
    data class Result(val status: Int, val data: List<ServiceRequest>)
}

object CurrentServiceAPIResponse {
    data class Result(val status: Int, val data: ServiceRequest)
}

object ProfileAPIResponse {
    data class Result(val status: Int, val data: UserProfile)
}

object GroupsListAPIResponse {
    data class Result(val status: Int, val data: List<AssetGroup>)
}

object DistrictsListAPIResponse {
    data class Result(val status: Int, val data: List<District>)
}

object PanchayathsListAPIResponse {
    data class Result(val status: Int, val data: List<Panchayath>)
}

object StatesListAPIResponse {
    data class Result(val status: Int, val data: List<States>)
}

object LocationUpdateResponse {
    data class Result(
        val status: Int,
        val data: List<Int>,
        val current_service: String?
    )
}

object GenericUpdateResponse {
    data class Result(
        val status: Int,
        val data: List<Int>
    )
}

object OTPRequestResponse {
    /**
     * modify as per the otp request response
     */
    data class Result(
        val meta: OTPMeta,
        val data: OTPData
    )

    data class OTPMeta(val success: Boolean, val message: String)
    data class OTPData(val phoneNumber: String, val token: String)
}

//part of on boarding
object OTPVerifyResponse {
    /**
     * modify as per the otp verify response
     */
    data class Result(
        val meta: OTPMeta,
        val data: OTPData
    )

    data class OTPMeta(val success: Boolean, val message: String)
    data class OTPData(val phoneNumber: String, val verified: Boolean)
}


object ResetPasswordResponse {
    data class Result(
        val status: Int,
        val message: String
    )
}

val defaultErrorResponse = ErrorResponse(400, "Unknown Error")

data class ErrorResponse(
    val status: Int = 400,
    val error: String = "Please try again later",
    val message: String? = null
) : Serializable