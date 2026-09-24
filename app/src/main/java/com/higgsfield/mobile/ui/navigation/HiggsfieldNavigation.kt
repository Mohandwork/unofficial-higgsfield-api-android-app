package com.higgsfield.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.feature.conversation.ConversationRoute
import com.higgsfield.mobile.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data class ConversationKey(val mediaKind: MediaKind) : NavKey

@Composable
fun HiggsfieldApp() {
    val backStack = rememberNavBackStack(HomeKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeScreen(
                    onOpenImages = dropUnlessResumed { backStack.add(ConversationKey(MediaKind.IMAGE)) },
                    onOpenVideos = dropUnlessResumed { backStack.add(ConversationKey(MediaKind.VIDEO)) },
                )
            }
            entry<ConversationKey> { route ->
                ConversationRoute(
                    mediaKind = route.mediaKind,
                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
