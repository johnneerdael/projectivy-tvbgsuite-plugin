package tv.projectivy.plugin.wallpaperprovider.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import com.butch708.projectivy.tvbgsuite.BuildConfig
import com.butch708.projectivy.tvbgsuite.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktDeviceAuthRepository
import tv.projectivy.plugin.wallpaperprovider.sample.local.TraktLocalApi

class SettingsFragment : GuidedStepSupportFragment() {

    companion object {
        private const val ACTION_ID_TRAKT_STATUS = 1L
        private const val ACTION_ID_TRAKT_START = 2L
        private const val ACTION_ID_TRAKT_POLL = 3L
        private const val ACTION_ID_CATALOGS = 4L
        private const val ACTION_ID_EVENT_IDLE = 5L
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        return Guidance(
            getString(R.string.plugin_name),
            "Local Trakt + TMDB wallpaper generation",
            getString(R.string.settings),
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_plugin)
        )
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        PreferencesManager.init(requireContext())

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_TRAKT_STATUS)
                .title("Trakt")
                .description(if (PreferencesManager.traktAccessToken.isNotBlank()) "Connected" else "Not connected")
                .enabled(false)
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_TRAKT_START)
                .title("Start Trakt OAuth")
                .description("Get a device code for trakt.tv/activate")
                .build()
        )

        val activation = if (PreferencesManager.traktUserCode.isNotBlank()) {
            "${PreferencesManager.traktVerificationUrl}/${PreferencesManager.traktUserCode}"
        } else {
            "Start OAuth first"
        }
        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_TRAKT_POLL)
                .title("Check Trakt OAuth")
                .description(activation)
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_CATALOGS)
                .title("Catalogs")
                .description(PreferencesManager.selectedCatalogs)
                .subActions(createCatalogActions())
                .build()
        )

        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_ID_EVENT_IDLE)
                .title("Refresh on idle exit")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(PreferencesManager.refreshOnIdleExit)
                .build()
        )
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id == 401L || action.id == 402L) {
            val key = if (action.id == 401L) "anticipated_movies" else "anticipated_shows"
            val selected = PreferencesManager.selectedCatalogs
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (selected.contains(key)) selected.remove(key) else selected.add(key)
            PreferencesManager.selectedCatalogs = selected.joinToString(",")
            reloadActions()
            notifySettingsChanged()
            return true
        }
        return super.onSubGuidedActionClicked(action)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_ID_TRAKT_START -> startTraktOAuth()
            ACTION_ID_TRAKT_POLL -> pollTraktOAuth()
            ACTION_ID_CATALOGS -> {
                action.subActions = createCatalogActions()
                notifyActionChanged(findActionPositionById(ACTION_ID_CATALOGS))
            }
            ACTION_ID_EVENT_IDLE -> {
                val newState = !PreferencesManager.refreshOnIdleExit
                PreferencesManager.refreshOnIdleExit = newState
                action.isChecked = newState
                notifyActionChanged(findActionPositionById(ACTION_ID_EVENT_IDLE))
                notifySettingsChanged()
            }
        }
    }

    private fun startTraktOAuth() {
        MainScope().launch {
            val result = TraktDeviceAuthRepository(traktApi()).start()
            val message = result.fold(
                onSuccess = { "${it.verificationUrl}/${it.userCode}" },
                onFailure = { it.message ?: "Failed to start OAuth" }
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            notifySettingsChanged()
            reloadActions()
        }
    }

    private fun pollTraktOAuth() {
        MainScope().launch {
            val result = TraktDeviceAuthRepository(traktApi()).poll()
            Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show()
            notifySettingsChanged()
            reloadActions()
        }
    }

    private fun traktApi(): TraktLocalApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktLocalApi::class.java)

    private fun createCatalogActions(): List<GuidedAction> {
        val selected = PreferencesManager.selectedCatalogs
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        return listOf(
            GuidedAction.Builder(context)
                .id(401L)
                .title("Anticipated Movies")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked("anticipated_movies" in selected)
                .build(),
            GuidedAction.Builder(context)
                .id(402L)
                .title("Anticipated Shows")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked("anticipated_shows" in selected)
                .build()
        )
    }

    private fun reloadActions() {
        actions.clear()
        onCreateActions(actions, null)
        actions.indices.forEach(::notifyActionChanged)
    }

    private fun notifySettingsChanged() {
        (requireActivity() as? SettingsActivity)?.requestWallpaperUpdate()
    }
}
