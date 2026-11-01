package moe.antimony.hoshi.features.sync

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base URLs for the Google Drive and OAuth APIs. A configured [serverUrl] points sync at a
 * self-hosted Google Drive compatible server, which then serves the OAuth device flow
 * (`/device/code`, `/token`) and the Drive API (`/drive/v3`, `/upload/drive/v3`) from one
 * origin. Without one, requests go to Google.
 */
@Singleton
class DriveEndpoints internal constructor(
    serverUrl: String?,
    private val persist: (String?) -> Unit,
) {
    @Inject constructor(@ApplicationContext context: Context) :
        this(context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE))

    private constructor(preferences: SharedPreferences) : this(
        preferences.getString(ServerUrlKey, null),
        { preferences.edit().putString(ServerUrlKey, it.orEmpty()).apply() },
    )

    /** Read per request, so a changed server applies without restarting. */
    @Volatile
    var serverUrl: String? = normalize(serverUrl)
        private set

    val usesCustomServer: Boolean get() = serverUrl != null
    val deviceCodeUrl: String get() = "${serverUrl ?: GoogleOAuthBase}/device/code"
    val tokenUrl: String get() = "${serverUrl ?: GoogleOAuthBase}/token"

    fun drive(path: String): String = "${serverUrl ?: GoogleApiBase}/drive/v3/$path"

    fun upload(path: String): String = "${serverUrl ?: GoogleApiBase}/upload/drive/v3/$path"

    fun driveUrl(endpoint: String, queryParameters: Map<String, String>): String {
        val query = queryParameters.entries.joinToString("&") { (name, value) ->
            "${name.urlQueryComponent()}=${value.urlQueryComponent()}"
        }
        return "${drive(endpoint)}?$query"
    }

    /** Stores [serverUrl], blank meaning Google. Returns whether the server changed. */
    fun save(serverUrl: String?): Boolean {
        val normalized = normalize(serverUrl)
        if (normalized == this.serverUrl) return false
        persist(normalized)
        this.serverUrl = normalized
        return true
    }

    companion object {
        private const val GoogleApiBase = "https://www.googleapis.com"
        private const val GoogleOAuthBase = "https://oauth2.googleapis.com"

        // Stored with the device code credentials, which a custom server always uses.
        private const val PreferencesName = "google-drive-device-code-auth"
        private const val ServerUrlKey = "serverUrl"

        /** Endpoints that always use Google. */
        fun google(): DriveEndpoints = DriveEndpoints(null) {}

        fun normalize(url: String?): String? = url?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
    }
}
