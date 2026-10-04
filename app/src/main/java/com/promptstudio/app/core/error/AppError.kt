package com.promptstudio.app.core.error

import androidx.annotation.StringRes
import com.promptstudio.app.R
import com.promptstudio.app.core.network.UploadTooLargeException
import java.io.IOException
import java.util.Locale
import retrofit2.HttpException

data class AppError(
    val code: String,
    @param:StringRes val messageResId: Int,
    val retryable: Boolean,
    val diagnosticMessage: String? = null,
    val userMessage: String? = null,
)

object ErrorMapper {
    fun messageResIdFor(code: String?): Int? = when (code) {
        CODE_INSTRUCTION_REQUIRED -> R.string.error_instruction_required
        CODE_REFERENCE_IMAGE_REQUIRED -> R.string.error_reference_image_required
        CODE_REFERENCE_VIDEO_REQUIRED -> R.string.error_reference_video_required
        CODE_ATTACHMENT_UNREADABLE -> R.string.error_attachment_unreadable
        CODE_ATTACHMENT_TYPE_UNSUPPORTED -> R.string.error_attachment_type_unsupported
        CODE_ATTACHMENT_TOO_LARGE -> R.string.error_attachment_too_large
        CODE_REFERENCE_IMAGE_LIMIT -> R.string.error_reference_image_limit
        CODE_IMAGE_EDIT_UNSUPPORTED -> R.string.error_image_edit_unsupported
        CODE_ACTIVE_IMAGE_UNAVAILABLE -> R.string.error_active_image_unavailable
        CODE_CREDENTIALS_REJECTED -> R.string.error_credentials_rejected
        CODE_REQUEST_NOT_FOUND -> R.string.error_request_not_found
        CODE_REMOTE_PROTOCOL -> R.string.error_remote_protocol
        CODE_REMOTE_FAILED -> R.string.error_generation_failed
        CODE_MODERATED -> R.string.error_generation_moderated
        CODE_NOT_ENOUGH_CREDITS -> R.string.error_not_enough_credits
        CODE_NETWORK -> R.string.error_network_unavailable
        CODE_HTTP, CODE_UNKNOWN -> R.string.error_unknown
        else -> null
    }

    fun isRetryable(code: String?): Boolean = code in setOf(CODE_NETWORK, CODE_HTTP)

    fun instructionRequired() = AppError(
        code = CODE_INSTRUCTION_REQUIRED,
        messageResId = R.string.error_instruction_required,
        retryable = false,
    )

    fun referenceImageRequired() = AppError(
        code = CODE_REFERENCE_IMAGE_REQUIRED,
        messageResId = R.string.error_reference_image_required,
        retryable = false,
    )

    fun referenceVideoRequired() = AppError(
        code = CODE_REFERENCE_VIDEO_REQUIRED,
        messageResId = R.string.error_reference_video_required,
        retryable = false,
    )

    fun attachmentUnreadable(diagnosticMessage: String? = null) = AppError(
        code = CODE_ATTACHMENT_UNREADABLE,
        messageResId = R.string.error_attachment_unreadable,
        retryable = false,
        diagnosticMessage = diagnosticMessage,
    )

    fun attachmentTypeUnsupported(diagnosticMessage: String? = null) = AppError(
        code = CODE_ATTACHMENT_TYPE_UNSUPPORTED,
        messageResId = R.string.error_attachment_type_unsupported,
        retryable = false,
        diagnosticMessage = diagnosticMessage,
    )

    fun attachmentTooLarge(responseBody: String? = null): AppError {
        val maximumBytes = responseBody?.maximumUploadBytes()
        val maximumMiB = maximumBytes?.let { String.format(Locale.ROOT, "%.1f", it / 1048576.0) }
        return AppError(
            code = CODE_ATTACHMENT_TOO_LARGE,
            messageResId = R.string.error_attachment_too_large,
            retryable = false,
            userMessage = maximumMiB?.let { "This file is too large. The maximum upload size is $it MiB. Choose a smaller file." },
        )
    }

    fun referenceImageLimit() = AppError(
        code = CODE_REFERENCE_IMAGE_LIMIT,
        messageResId = R.string.error_reference_image_limit,
        retryable = false,
    )

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

    fun imageEditUnsupported() = AppError(
        code = CODE_IMAGE_EDIT_UNSUPPORTED,
        messageResId = R.string.error_image_edit_unsupported,
        retryable = false,
    )

    fun activeImageUnavailable() = AppError(
        code = CODE_ACTIVE_IMAGE_UNAVAILABLE,
        messageResId = R.string.error_active_image_unavailable,
        retryable = false,
    )

    fun notEnoughCredits() = AppError(
        code = CODE_NOT_ENOUGH_CREDITS,
        messageResId = R.string.error_not_enough_credits,
        retryable = false,
    )

    fun from(throwable: Throwable): AppError = when (throwable) {
        is UploadTooLargeException -> attachmentTooLarge(throwable.responseBody)
        is IOException -> AppError(CODE_NETWORK, R.string.error_network_unavailable, retryable = true)
        is HttpException -> httpError(throwable)
        else -> AppError(CODE_UNKNOWN, R.string.error_unknown, retryable = false, diagnosticMessage = throwable.message)
    }

    private fun httpError(error: HttpException): AppError {
        val responseBody = error.response()?.errorBody()?.string()
        if (error.code() == HTTP_CONTENT_TOO_LARGE) return attachmentTooLarge(responseBody)
        val remoteDetail = responseBody?.extractRemoteDetail()
        val fallback = when (error.code()) {
                HTTP_UNAUTHORIZED -> credentialsRejected()
                HTTP_NOT_FOUND -> requestNotFound()
                else -> AppError(CODE_HTTP, R.string.error_unknown, error.code() >= HTTP_SERVER_ERROR, error.message())
        }
        return remoteDetail?.let { fallback.copy(diagnosticMessage = it, userMessage = it) } ?: fallback
    }

    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_CONTENT_TOO_LARGE = 413
    private const val HTTP_SERVER_ERROR = 500
    private const val CODE_INSTRUCTION_REQUIRED = "instruction_required"
    private const val CODE_REFERENCE_IMAGE_REQUIRED = "reference_image_required"
    private const val CODE_REFERENCE_VIDEO_REQUIRED = "reference_video_required"
    private const val CODE_ATTACHMENT_UNREADABLE = "attachment_unreadable"
    private const val CODE_ATTACHMENT_TYPE_UNSUPPORTED = "attachment_type_unsupported"
    private const val CODE_ATTACHMENT_TOO_LARGE = "attachment_too_large"
    private const val CODE_REFERENCE_IMAGE_LIMIT = "reference_image_limit"
    private const val CODE_IMAGE_EDIT_UNSUPPORTED = "image_edit_unsupported"
    private const val CODE_ACTIVE_IMAGE_UNAVAILABLE = "active_image_unavailable"
    private const val CODE_CREDENTIALS_REJECTED = "credentials_rejected"
    private const val CODE_REQUEST_NOT_FOUND = "request_not_found"
    private const val CODE_REMOTE_PROTOCOL = "remote_protocol"
    private const val CODE_REMOTE_FAILED = "remote_failed"
    private const val CODE_MODERATED = "moderated"
    private const val CODE_NOT_ENOUGH_CREDITS = "not_enough_credits"
    private const val CODE_NETWORK = "network"
    private const val CODE_HTTP = "http"
    private const val CODE_UNKNOWN = "unknown"
    private const val REMOTE_NOT_ENOUGH_CREDITS = "not_enough_credits"
    private const val REMOTE_INSUFFICIENT_CREDITS = "insufficient_credits"
}

private fun String.maximumUploadBytes(): Long? = sequenceOf(
    Regex("<MaxSizeAllowed>\\s*(\\d+)\\s*</MaxSizeAllowed>", RegexOption.IGNORE_CASE),
    Regex("\"(?:max_size_bytes|maximum_size_bytes)\"\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE),
).mapNotNull { it.find(this)?.groupValues?.getOrNull(1)?.toLongOrNull() }
    .firstOrNull { it > 0L }

private fun String.extractRemoteDetail(): String? =
    Regex("\\\"(?:detail|code)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        .find(this)?.groupValues?.getOrNull(1)?.take(MAX_REMOTE_MESSAGE_LENGTH)

private const val MAX_REMOTE_MESSAGE_LENGTH = 240
