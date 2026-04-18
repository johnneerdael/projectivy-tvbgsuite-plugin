package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.traktlistbackdrops.tv.BuildConfig
import tv.projectivy.plugin.wallpaperprovider.sample.PreferencesManager

sealed class TraktPollResult {
    data object Pending : TraktPollResult()
    data object InvalidDeviceCode : TraktPollResult()
    data object AlreadyUsed : TraktPollResult()
    data object Expired : TraktPollResult()
    data object Denied : TraktPollResult()
    data object SlowDown : TraktPollResult()
    data class Approved(val accessToken: String) : TraktPollResult()
    data class Failed(val message: String) : TraktPollResult()
}

class TraktDeviceAuthRepository(
    private val api: TraktLocalApi,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000L }
) {
    suspend fun start(): Result<TraktDeviceCodeResponse> {
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        if (clientId.isBlank()) return Result.failure(IllegalStateException("TRAKT_CLIENT_ID is missing"))
        val response = api.requestDeviceCode(clientId, TraktDeviceCodeRequest(clientId))
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            return Result.failure(IllegalStateException("Trakt device code failed: HTTP ${response.code()}"))
        }
        PreferencesManager.traktDeviceCode = body.deviceCode
        PreferencesManager.traktUserCode = body.userCode
        PreferencesManager.traktVerificationUrl = body.verificationUrl
        return Result.success(body)
    }

    suspend fun poll(): TraktPollResult {
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        val clientSecret = BuildConfig.TRAKT_CLIENT_SECRET.trim()
        val deviceCode = PreferencesManager.traktDeviceCode
        if (clientId.isBlank() || clientSecret.isBlank()) return TraktPollResult.Failed("Trakt credentials are missing")
        if (deviceCode.isBlank()) return TraktPollResult.InvalidDeviceCode

        val response = api.requestDeviceToken(clientId, TraktDeviceTokenRequest(deviceCode, clientId, clientSecret))
        if (!response.isSuccessful) return mapPollStatus(response.code(), null)
        val body = response.body() ?: return TraktPollResult.Failed("Trakt token response was empty")
        saveToken(body)
        return TraktPollResult.Approved(body.accessToken)
    }

    suspend fun authorizationHeader(): String? {
        if (isTokenUsable(nowSeconds(), PreferencesManager.traktExpiresAt) && PreferencesManager.traktAccessToken.isNotBlank()) {
            return "Bearer ${PreferencesManager.traktAccessToken}"
        }
        val refreshToken = PreferencesManager.traktRefreshToken
        if (refreshToken.isBlank()) return null
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        val response = api.refreshToken(
            clientId,
            TraktRefreshTokenRequest(
                refreshToken = refreshToken,
                clientId = clientId,
                clientSecret = BuildConfig.TRAKT_CLIENT_SECRET.trim(),
                redirectUri = BuildConfig.TRAKT_REDIRECT_URI.ifBlank { "urn:ietf:wg:oauth:2.0:oob" }
            )
        )
        val body = response.body()
        if (!response.isSuccessful || body == null) return null
        saveToken(body)
        return "Bearer ${body.accessToken}"
    }

    private fun saveToken(body: TraktTokenResponse) {
        PreferencesManager.traktAccessToken = body.accessToken
        PreferencesManager.traktRefreshToken = body.refreshToken
        PreferencesManager.traktExpiresAt = nowSeconds() + body.expiresIn
        PreferencesManager.traktDeviceCode = ""
        PreferencesManager.traktUserCode = ""
    }

    companion object {
        fun mapPollStatus(statusCode: Int, message: String?): TraktPollResult = when (statusCode) {
            400 -> TraktPollResult.Pending
            404 -> TraktPollResult.InvalidDeviceCode
            409 -> TraktPollResult.AlreadyUsed
            410 -> TraktPollResult.Expired
            418 -> TraktPollResult.Denied
            429 -> TraktPollResult.SlowDown
            else -> TraktPollResult.Failed(message ?: "HTTP $statusCode")
        }

        fun isTokenUsable(nowSeconds: Long, expiresAtSeconds: Long): Boolean =
            expiresAtSeconds - 60L > nowSeconds
    }
}
