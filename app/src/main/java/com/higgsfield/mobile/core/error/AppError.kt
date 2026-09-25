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
    fun messageResIdFor(code: String?): Int? = when (code) {
        CODE_INSTRUCTION_REQUIRED -> R.string.error_instruction_required
        CODE_REFERENCE_IMAGE_REQUIRED -> R.string.error_reference_image_required
        CODE_REFERENCE_VIDEO_REQUIRED -> R.string.error_reference_video_required
        CODE_ATTACHMENT_UNREADABLE -> R.string.error_attachment_unreadable
        CODE_ATTACHMENT_TYPE_UNSUPPORTED -> R.string.error_attachment_type_unsupported
        CODE_REFERENCE_IMAGE_LIMIT -> R.string.error_reference_image_limit
        CODE_CREDENTIALS_REJECTED -> R.string.error_credentials_rejected
        CODE_REQUEST_NOT_FOUND -> R.string.error_request_not_found
        CODE_REMOTE_PROTOCOL -> R.string.error_remote_protocol
        CODE_REMOTE_FAILED -> R.string.error_generation_failed
        CODE_MODERATED -> R.string.error_generation_moderated
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
    private const val CODE_INSTRUCTION_REQUIRED = "instruction_required"
    private const val CODE_REFERENCE_IMAGE_REQUIRED = "reference_image_required"
    private const val CODE_REFERENCE_VIDEO_REQUIRED = "reference_video_required"
    private const val CODE_ATTACHMENT_UNREADABLE = "attachment_unreadable"
    private const val CODE_ATTACHMENT_TYPE_UNSUPPORTED = "attachment_type_unsupported"
    private const val CODE_REFERENCE_IMAGE_LIMIT = "reference_image_limit"
    private const val CODE_CREDENTIALS_REJECTED = "credentials_rejected"
    private const val CODE_REQUEST_NOT_FOUND = "request_not_found"
    private const val CODE_REMOTE_PROTOCOL = "remote_protocol"
    private const val CODE_REMOTE_FAILED = "remote_failed"
    private const val CODE_MODERATED = "moderated"
    private const val CODE_NETWORK = "network"
    private const val CODE_HTTP = "http"
    private const val CODE_UNKNOWN = "unknown"
}
