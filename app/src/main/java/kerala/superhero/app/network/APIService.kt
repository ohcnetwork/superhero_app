package kerala.superhero.app.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Deferred
import kerala.superhero.app.Constants
import kerala.superhero.app.data.*
import kerala.superhero.app.prefs
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface APIService {

    @FormUrlEncoded
    @POST("asset/manager/login")
    fun loginAsync(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): Deferred<AuthAPIResponse.Result>

    @FormUrlEncoded
    @PUT("asset/manager/fcm")
    fun updateFCMTokenAsync(
        @Field("token") phone: String,
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<GenericUpdateResponse.Result>

    @FormUrlEncoded
    @PUT("asset/update/location")
    fun updateLocationAsync(
        @Field("location") location: List<Double>,
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<LocationUpdateResponse.Result>

    @GET("asset/manager/request/active?show_expired=true")
    fun getAllRequestsAsync(
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<ServiceRequestsAPIResponse.Result>

//    @GET("/asset/manager/request/accept/{id}")
//    fun acceptARequestAsync(
//        @Path("id") requestId: String,
//        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
//    ): Deferred<GenericUpdateResponse.Result>

//    @GET("/asset/manager/request/end/current")
//    fun endCurrentTripAsync(
//        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
//    ): Deferred<GenericUpdateResponse.Result>

//    @GET("/asset/manager/request/pickup")
//    fun pickUpCurrentTripAsync(
//        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
//    ): Deferred<GenericUpdateResponse.Result>


    @GET("/asset/manager/request/accept/{id}")
    fun acceptARequestAsync(
        @Path("id") requestId: String,
        @Query("location") location: String,
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<GenericUpdateResponse.Result>

    @GET("/asset/manager/request/pickup")
    fun pickUpCurrentTripAsync(
        @Query("location") location: String,
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<GenericUpdateResponse.Result>

    @GET("/asset/manager/request/end/current")
    fun endCurrentTripAsync(
        @Query("location") location: String,
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<GenericUpdateResponse.Result>

    @GET("/asset/manager/profile")
    fun getUserProfileAsync(
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<ProfileAPIResponse.Result>

    @GET("asset/list/group")
    fun getAllAssetGroupsAsync(): Deferred<GroupsListAPIResponse.Result>

    @GET("asset/list/plb")
    fun getAllPanchayathsAsync(@Query("district") district: String): Deferred<PanchayathsListAPIResponse.Result>

    @GET("asset/list/district")
    fun getAllDistrictsAsync(): Deferred<DistrictsListAPIResponse.Result>

    @GET("asset/list/state")
    fun getAllStatesAsync(): Deferred<StatesListAPIResponse.Result>

//    @FormUrlEncoded
//    @POST("asset/manager/signup")
//    fun signUpAsync(
//        @Field("name") name: String,
//        @Field("phone") phone: String,
//        @Field("email") email: String,
//        @Field("password") password: String,
//        @Field("reg_no") reg_no: String,
//        @Field("asset_category") asset_category: Int,
//        @Field("asset_group") asset_group: Int,
//        @Field("state") state: String,
//        @Field("secondary_phone") secondary_phone: String? = null,
//        @Field("category") category: Int?,
//        @Field("role") role: String
//    ): Deferred<AuthAPIResponse.Result>

    @GET("meal/request/mark/delivered/{id}")
    fun markDeliveryCompleteAsync(
        @Path("id") id: String,
        @Query("location") location: String,
        @Header("x-access-token") access_token: String = prefs.accessToken ?: ""
    ): Deferred<CurrentServiceAPIResponse.Result>

    /**
     * state the otp end point here
     */
    @GET("")
    fun requestOTPAsync(
        @Query("phoneNumber") phoneNumber: String,
        @Query("action") action: String = "sendotp"
    ): Deferred<OTPRequestResponse.Result>

    //part of on boarding
    @GET("")
    fun requestOTPVerifyAsync(
        @Query("phoneNumber") phoneNumber: String,
        @Query("otp") otp: String,
        @Query("token") token: String,
        @Query("action") action: String = "verifyotp"
    ): Deferred<OTPVerifyResponse.Result>

    @FormUrlEncoded
    @PUT("asset/manager/reset/password")
    fun resetPasswordAsync(
        @Field("phone") phone: String,
        @Field("password") password: String,
        @Field("otp") otp: String,
        @Field("token") token: String
    ): Deferred<ResetPasswordResponse.Result>

    @FormUrlEncoded
    @POST("asset/manager/signup")
    fun signUpAsync(
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("reg_no") reg_no: String,
        @Field("asset_category") asset_category: Int,
        @Field("asset_group") asset_group: Int,
        @Field("state") state: String,
        @Field("district") district: String,
        @Field("plb") plb: String,
        @Field("ward_no") ward: String,
        @Field("secondary_phone") secondary_phone: String? = null,
        @Field("category") category: Int?,
        @Field("role") role: String
    ): Deferred<AuthAPIResponse.Result>

    companion object {

        fun convertErrorBody(throwable: HttpException): ErrorResponse? {
            return try {
                throwable.response()?.errorBody()?.source()?.let {
                    val moshiAdapter = Moshi.Builder().build().adapter(ErrorResponse::class.java)
                    moshiAdapter.fromJson(it)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                defaultErrorResponse
            }
        }

        fun create(baseUrl: String = Constants.API_URL): APIService {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()
            return Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .baseUrl(baseUrl)
                .client(client)
                .build().create(APIService::class.java)
        }
    }
}