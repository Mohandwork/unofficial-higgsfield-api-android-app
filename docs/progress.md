# Progress and verification

## Current checkpoint

The product flows and the conversation-architecture refactor are implemented in the working tree. JVM verification has passed; manual UI verification remains the chosen final check.

## 2026-09-26 architecture refactor checkpoint

The current working tree groups `ConversationUiState` into chat, composer, and generation sections, groups `ConversationUiEvent` by the same responsibilities, and moves workspace observation, foreground polling-job ownership, source-selection persistence, and debounced per-conversation draft writes into focused feature components. Transient sheet/menu state is view-local. Existing rendering call sites use read-only state aliases; mutations target the owning section.

Remaining work is Stage 1 data integrity in `next-enhancements.md`: the refactor moves visible-workspace polling ownership but does **not** make database transitions monotonic or coordinate foreground polling with app-wide recovery.

No commit or push was made for this checkpoint. The staged codebase-review document predates this refactor and was not altered here.

## Completed

- Image and Video workspaces with independent persisted state, including composer prompts, options, and attachment references per conversation.
- Room-backed conversation history with a compact bottom sheet and expanded rail. The sheet scrolls its chat list only after full expansion.
- New chat, rename, explicit chat removal confirmation, and recovery to a valid conversation.
- Creative Brief editing, persistence, and capability-aware prompt/negative-prompt composition.
- Reference selection with direct single-slot CTA behavior; submitted references remain visible when a generation fails. Generated-image edits send the selected output URL to verified image-edit endpoints and reject incompatible models before POST.
- Generation timeline with queued/running/success/failure/cancelled states, progress indicators, retry, and stale-response protection.
- Provider error detail rendered from the response body for all relevant status codes, including authentication and credit failures.
- Static model catalog details from the supplied pricing/spec material; the bottom price chip was removed.
- Neon-lime/cyan light and dark themes plus matching mode-aware splash artwork.
- Loading overlays for chat creation, switching, removal, and other larger transitions, held until the destination conversation and timeline have loaded.
- Separate image-edit and prompt/settings-reuse actions, explicit media load/failure/retry states, selected-image thumbnail, prompt copying, and automatic timeline scrolling to a newly added generation.
- Local-only drafts, faithful retry from the original request snapshot, and credential-safe configuration.

## Verification completed

The following debug checks have passed during the current implementation pass:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- `git diff --check`

The debug APK has also been checked against the 16 KB page-size packaging requirement. Live provider calls remain opt-in and are not required for normal builds or tests.

## Known limitations / next verification steps

- A live provider edit has not yet been exercised in this pass; unit tests verify the request includes the selected image URL and that incompatible models make no generation POST.
- Background recovery is deferred. Opening the app polls accepted requests across chats; it does not schedule polling while closed.
- Multi-image output selection remains pending a review of provider behavior. The current timeline displays the first output only.
- There are no Compose screenshot tests yet; manual compact/expanded device review is still recommended.
- Live pricing/credit estimates are intentionally absent. They require an authenticated provider schema and explicit approval for billable requests; static catalog values remain available.
- Provider-side conversation deletion is out of scope. “Chat removal” deletes the local conversation and its related local records.

## How to reproduce the checks

From the repository root:

```text
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
git diff --check
```

For a live API check, configure local secrets from `secrets.properties.example` and submit a request intentionally. Do not place credentials in source control or share unsanitized HTTP logs.
