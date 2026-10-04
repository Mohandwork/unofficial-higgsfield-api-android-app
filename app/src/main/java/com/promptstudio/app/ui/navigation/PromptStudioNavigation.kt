package com.promptstudio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.feature.conversation.ConversationRoute
import kotlinx.serialization.Serializable

@Serializable
data class ConversationKey(val mediaKind: MediaKind, val conversationId: String? = null) : NavKey

@Composable
fun PromptStudioApp() {
    val backStack = rememberNavBackStack(ConversationKey(MediaKind.IMAGE))
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ConversationKey> { route ->
                ConversationRoute(
                    mediaKind = route.mediaKind,
                    conversationId = route.conversationId,
                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                    onSelectMediaKind = { kind ->
                        if (kind != route.mediaKind) {
                            backStack.removeLastOrNull()
                            backStack.add(ConversationKey(kind))
                        }
                    },
                    onSelectConversation = { id, kind ->
                        backStack.removeLastOrNull()
                        backStack.add(ConversationKey(kind, id))
                    },
                )
            }
        },
    )
}
