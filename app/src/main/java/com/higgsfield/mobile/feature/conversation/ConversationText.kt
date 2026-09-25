package com.higgsfield.mobile.feature.conversation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface ConversationText {
    data class Resource(@param:StringRes val id: Int, val arguments: List<Any> = emptyList()) : ConversationText
    data class Dynamic(val value: String) : ConversationText
}

@Composable
fun ConversationText.resolve(): String = when (this) {
    is ConversationText.Resource -> stringResource(id, *arguments.toTypedArray())
    is ConversationText.Dynamic -> value
}
