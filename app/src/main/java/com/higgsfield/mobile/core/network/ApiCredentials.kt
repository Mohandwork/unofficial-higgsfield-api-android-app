package com.higgsfield.mobile.core.network

/** Credentials supplied only from local BuildConfig values for this private build. */
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
