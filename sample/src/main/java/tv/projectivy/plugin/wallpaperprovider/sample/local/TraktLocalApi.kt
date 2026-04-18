package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
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
    @Headers("trakt-api-version: 2")
    @POST("oauth/device/code")
    suspend fun requestDeviceCode(
        @Header("trakt-api-key") apiKey: String,
        @Body body: TraktDeviceCodeRequest
    ): Response<TraktDeviceCodeResponse>

    @Headers("trakt-api-version: 2")
    @POST("oauth/device/token")
    suspend fun requestDeviceToken(
        @Header("trakt-api-key") apiKey: String,
        @Body body: TraktDeviceTokenRequest
    ): Response<TraktTokenResponse>

    @Headers("trakt-api-version: 2")
    @POST("oauth/token")
    suspend fun refreshToken(
        @Header("trakt-api-key") apiKey: String,
        @Body body: TraktRefreshTokenRequest
    ): Response<TraktTokenResponse>

    @Headers("trakt-api-version: 2")
    @GET("movies/anticipated")
    suspend fun anticipatedMovies(
        @Header("trakt-api-key") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 20
    ): Response<List<TraktAnticipatedItem>>

    @Headers("trakt-api-version: 2")
    @GET("shows/anticipated")
    suspend fun anticipatedShows(
        @Header("trakt-api-key") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 20
    ): Response<List<TraktAnticipatedItem>>
}
