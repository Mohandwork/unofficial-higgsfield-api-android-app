# Progress and verification

## Current checkpoint

The refined product implementation is complete in the working tree. The remaining work is verification polish and provider-dependent integration, not an unfinished core UX flow.

## Completed

- Image and Video workspaces with independent persisted state.
- Room-backed conversation history with compact drawer and expanded rail.
- New chat, rename, explicit chat removal confirmation, and recovery to a valid conversation.
- Creative Brief editing, persistence, and capability-aware prompt/negative-prompt composition.
- Reference selection with direct single-slot CTA behavior; references remain visible when a generation fails.
- Generation timeline with queued/running/success/failure/cancelled states, progress indicators, retry, and stale-response protection.
- Provider error detail rendered from the response body for all relevant status codes, including authentication and credit failures.
- Static model catalog details from the supplied pricing/spec material; the bottom price chip was removed.
- Neon-lime/cyan light and dark themes plus matching mode-aware splash artwork.
- Loading overlays for chat creation, switching, removal, and other larger transitions.
- Local-only drafts and credential-safe configuration.

## Verification completed

The following debug checks have passed during the current implementation pass:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- `git diff --check`

The debug APK has also been checked against the 16 KB page-size packaging requirement. Live provider calls remain opt-in and are not required for normal builds or tests.

## Known limitations / next verification steps

- Image editing is not complete yet: selecting an active generated image changes the prompt context, but the next submission does not currently include that output as an image/reference payload. The provider therefore receives text such as `Camera: Eagle view\nMake it realistic` without the source image.
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
