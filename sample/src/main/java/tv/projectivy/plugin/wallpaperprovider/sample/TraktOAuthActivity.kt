package tv.projectivy.plugin.wallpaperprovider.sample

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.traktlistbackdrops.tv.BuildConfig
import com.traktlistbackdrops.tv.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.projectivy.plugin.wallpaperprovider.sample.local.QrCodeGenerator
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktDeviceAuthRepository
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktLocalApi
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktPollResult

class TraktOAuthActivity : FragmentActivity() {
    private val scope = MainScope()
    private var pollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trakt_oauth)
        PreferencesManager.init(applicationContext)
        findViewById<Button>(R.id.oauthClose).setOnClickListener { finish() }
        startOrResumeOauth()
    }

    override fun onDestroy() {
        pollJob?.cancel()
        super.onDestroy()
    }

    private fun startOrResumeOauth() {
        scope.launch {
            val repo = TraktDeviceAuthRepository(traktApi())
            if (PreferencesManager.traktDeviceCode.isBlank()) {
                val result = repo.start()
                result.exceptionOrNull()?.let { error ->
                    showStatus(error.message ?: "Unable to start Trakt OAuth")
                    return@launch
                }
            }
            showCode()
            startPolling(repo)
        }
    }

    private fun showCode() {
        val verificationUrl = PreferencesManager.traktVerificationUrl.ifBlank { "https://trakt.tv/activate" }
        val userCode = PreferencesManager.traktUserCode
        val activationUrl = "$verificationUrl/$userCode"
        findViewById<TextView>(R.id.oauthUrl).text = activationUrl
        findViewById<TextView>(R.id.oauthCode).text = userCode
        findViewById<ImageView>(R.id.oauthQr).setImageBitmap(QrCodeGenerator.generate(activationUrl))
        showStatus("Scan the QR code, approve Trakt, then wait here.")
    }

    private fun startPolling(repo: TraktDeviceAuthRepository) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                delay(5000)
                when (val result = repo.poll()) {
                    is TraktPollResult.Approved -> {
                        showStatus("Connected to Trakt.")
                        delay(1000)
                        finish()
                        return@launch
                    }
                    TraktPollResult.Pending -> showStatus("Waiting for Trakt approval...")
                    TraktPollResult.SlowDown -> showStatus("Trakt asked us to slow down. Waiting...")
                    TraktPollResult.AlreadyUsed -> {
                        showStatus("This code was already used. Close and start again.")
                        return@launch
                    }
                    TraktPollResult.Denied -> {
                        showStatus("Trakt authorization denied. Close and start again.")
                        return@launch
                    }
                    TraktPollResult.Expired, TraktPollResult.InvalidDeviceCode -> {
                        PreferencesManager.traktDeviceCode = ""
                        PreferencesManager.traktUserCode = ""
                        showStatus("Code expired. Close and start OAuth again.")
                        return@launch
                    }
                    is TraktPollResult.Failed -> {
                        showStatus(result.message)
                        return@launch
                    }
                }
            }
        }
    }

    private fun showStatus(message: String) {
        findViewById<TextView>(R.id.oauthStatus).text = message
    }

    private fun traktApi(): TraktLocalApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktLocalApi::class.java)
}
