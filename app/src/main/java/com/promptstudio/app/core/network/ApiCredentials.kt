package com.promptstudio.app.core.network

/** Credentials supplied from local BuildConfig values for a user-built app. */
data class ApiCredentials(
    val keyId: String,
    val secret: String,
) {
    val isConfigured: Boolean get() = keyId.isNotBlank() && secret.isNotBlank()

    fun authorizationValue(): String? =
        if (isConfigured) "$AUTHORIZATION_PREFIX$keyId:$secret" else null

    private companion object {
        const val AUTHORIZATION_PREFIX = "Key "
    }
}
