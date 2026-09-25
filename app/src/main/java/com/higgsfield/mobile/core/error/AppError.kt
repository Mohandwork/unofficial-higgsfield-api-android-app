package com.higgsfield.mobile.core.error

import androidx.annotation.StringRes
import com.higgsfield.mobile.R
import java.io.IOException
import retrofit2.HttpException

data class AppError(
    val code: String,
    @param:StringRes val messageResId: Int,
    val retryable: Boolean,
    val diagnosticMessage: String? = null,
)

object ErrorMapper {
    fun credentialsRejected() = AppError(
        code = CODE_CREDENTIALS_REJECTED,
        messageResId = R.string.error_credentials_rejected,
        retryable = false,
    )

    fun requestNotFound() = AppError(
        code = CODE_REQUEST_NOT_FOUND,
        messageResId = R.string.error_request_not_found,
        retryable = false,
    )

    fun protocol(message: String) = AppError(
        code = CODE_REMOTE_PROTOCOL,
        messageResId = R.string.error_remote_protocol,
        retryable = false,
        diagnosticMessage = message,
    )

    fun generationFailed(message: String?) = AppError(
        code = CODE_REMOTE_FAILED,
        messageResId = R.string.error_generation_failed,
        retryable = false,
        diagnosticMessage = message,
    )

    fun moderated(message: String?) = AppError(
        code = CODE_MODERATED,
        messageResId = R.string.error_generation_moderated,
        retryable = false,
        diagnosticMessage = message,
    )

    fun from(throwable: Throwable): AppError = when (throwable) {
        is IOException -> AppError(CODE_NETWORK, R.string.error_network_unavailable, retryable = true)
        is HttpException -> when (throwable.code()) {
            HTTP_UNAUTHORIZED -> credentialsRejected()
            HTTP_NOT_FOUND -> requestNotFound()
            else -> AppError(CODE_HTTP, R.string.error_unknown, throwable.code() >= HTTP_SERVER_ERROR, throwable.message())
        }
        else -> AppError(CODE_UNKNOWN, R.string.error_unknown, retryable = false, diagnosticMessage = throwable.message)
    }

    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_SERVER_ERROR = 500
    private const val CODE_CREDENTIALS_REJECTED = "credentials_rejected"
    private const val CODE_REQUEST_NOT_FOUND = "request_not_found"
    private const val CODE_REMOTE_PROTOCOL = "remote_protocol"
    private const val CODE_REMOTE_FAILED = "remote_failed"
    private const val CODE_MODERATED = "moderated"
    private const val CODE_NETWORK = "network"
    private const val CODE_HTTP = "http"
    private const val CODE_UNKNOWN = "unknown"
}
