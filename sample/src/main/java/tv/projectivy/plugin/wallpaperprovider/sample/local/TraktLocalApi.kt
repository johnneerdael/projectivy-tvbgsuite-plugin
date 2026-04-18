package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class TraktDeviceCodeRequest(@SerializedName("client_id") val clientId: String)

data class TraktDeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("verification_url") val verificationUrl: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val interval: Int
)

data class TraktDeviceTokenRequest(
    val code: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_secret") val clientSecret: String
)

data class TraktRefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_secret") val clientSecret: String,
    @SerializedName("redirect_uri") val redirectUri: String,
    @SerializedName("grant_type") val grantType: String = "refresh_token"
)

data class TraktTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("created_at") val createdAt: Long
)

interface TraktLocalApi {
    @POST("oauth/device/code")
    suspend fun requestDeviceCode(@Body body: TraktDeviceCodeRequest): Response<TraktDeviceCodeResponse>

    @POST("oauth/device/token")
    suspend fun requestDeviceToken(@Body body: TraktDeviceTokenRequest): Response<TraktTokenResponse>

    @POST("oauth/token")
    suspend fun refreshToken(@Body body: TraktRefreshTokenRequest): Response<TraktTokenResponse>

    @GET("movies/anticipated")
    suspend fun anticipatedMovies(@Header("Authorization") authorization: String, @Query("limit") limit: Int = 20): Response<List<TraktAnticipatedItem>>

    @GET("shows/anticipated")
    suspend fun anticipatedShows(@Header("Authorization") authorization: String, @Query("limit") limit: Int = 20): Response<List<TraktAnticipatedItem>>
}
