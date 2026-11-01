package moe.antimony.hoshi.features.sync

/**
 * Base URLs for the Google Drive and OAuth APIs. Setting [serverUrl] points sync at a
 * self-hosted Google Drive compatible server, which then serves the OAuth device flow
 * (`/device/code`, `/token`) and the Drive API (`/drive/v3`, `/upload/drive/v3`) from
 * one origin. `null` uses Google.
 */
internal object DriveEndpoints {
    private const val GoogleApiBase = "https://www.googleapis.com"
    private const val GoogleOAuthBase = "https://oauth2.googleapis.com"

    /** Loaded and updated by [DeviceCodeDriveAuthorizer], which owns the setting. */
    @Volatile
    var serverUrl: String? = null

    val usesCustomServer: Boolean get() = serverUrl != null
    val deviceCodeUrl: String get() = "${serverUrl ?: GoogleOAuthBase}/device/code"
    val tokenUrl: String get() = "${serverUrl ?: GoogleOAuthBase}/token"

    fun drive(path: String): String = "${serverUrl ?: GoogleApiBase}/drive/v3/$path"

    fun upload(path: String): String = "${serverUrl ?: GoogleApiBase}/upload/drive/v3/$path"

    fun normalize(url: String?): String? = url?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
}
