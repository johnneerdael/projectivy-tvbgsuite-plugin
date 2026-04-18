package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import com.butch708.projectivy.tvbgsuite.R

class SettingsFragment : GuidedStepSupportFragment() {

    companion object {
        private const val ACTION_ID_TRAKT_STATUS = 1L
        private const val ACTION_ID_TRAKT_START = 2L
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
                .title("Connect Trakt")
                .description("Show QR code and connect automatically")
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
            ACTION_ID_TRAKT_START -> startActivity(Intent(requireContext(), TraktOAuthActivity::class.java))
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
