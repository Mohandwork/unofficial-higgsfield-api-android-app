# Prompt Studio

Prompt Studio is an unofficial Android client for the Higgsfield API. It is free and open source, with no ads, subscriptions, or in-app purchases. Drafts, chat history, model metadata, and UI state work locally; a generation request is sent only when the user submits it. The app is independently developed and is not affiliated with or endorsed by the API provider. API usage may still incur charges under a user's own provider account.

## Product state

The refined product now includes:

- Image and video workspaces with model/workflow selection.
- Persistent, section-scoped conversations with a compact history bottom sheet and an expanded rail.
- New-chat, rename, and chat-removal flows with confirmation and transition feedback.
- A Creative Brief for reusable intent, style, exclusions, and output guidance.
- Reference-image selection, direct single-reference CTA behavior, and multi-reference support where a workflow allows it.
- Static model capabilities, pricing, duration, and resolution metadata sourced from the supplied catalog material. Live estimates are intentionally not fabricated.
- Generation lifecycle cards for queued, running, succeeded, failed, and cancelled requests, including server-provided error details.
- Loading indicators for generation and larger workspace/history transitions.
- Light and dark themes, including mode-aware splash background and brand artwork.
- Offline-safe drafts and local persistence through Room.

## Architecture

The app is a Kotlin/Compose client organized around a presentation ViewModel, a Room-backed conversation/generation repository, and a small HTTP adapter for the Higgsfield API. API calls are isolated behind the repository boundary so UI state can be exercised without a network request.

Important behavior is documented in [docs/architecture.md](docs/architecture.md). The implementation plan is now an acceptance checklist rather than a list of unfinished product slices: [docs/implementation-plan.md](docs/implementation-plan.md). Current verification and known limitations are recorded in [docs/progress.md](docs/progress.md).

## Local setup

Build with `./gradlew :app:assembleDebug` (or `gradlew.bat :app:assembleDebug` on Windows). To use generation, copy `secrets.properties.example` to `app/secrets/secrets.properties` and set `HF_KEY_ID` and `HF_KEY_SECRET` with credentials from your own API account. The secrets file is ignored by Git. A build without credentials can run locally, but generation requests will fail authentication.

## Safety and operational notes

- Do not commit credentials. `secrets.properties` and local API keys are ignored; use `secrets.properties.example` as the template. Never distribute an APK built with your own API keys: BuildConfig values can be extracted from it.
- The debug build logs request/response diagnostics only for local troubleshooting. Sanitize logs before sharing them.
- Failed POSTs are not silently retried. Ambiguous outcomes remain visible so a user can decide whether to retry.
- Chat removal is explicit and cascades local conversation data; it does not attempt to delete provider-side history.
- Live generation and any billable operation remain user-triggered and require an authenticated environment.

## License

The source code is licensed under [Apache License 2.0](LICENSE). This license permits commercial reuse of the code; the project itself has no monetization feature. Model names and API documentation belong to their respective owners. The license does not grant rights to third-party trademarks or artwork.

## Verification

The current debug build has passing unit tests, Kotlin compilation, and APK assembly. Device visual QA and screenshot coverage are still useful follow-ups, but they are verification work rather than missing product functionality. See [docs/progress.md](docs/progress.md) for the exact commands and remaining limitations.


## App screenshots

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/91cc3bf2-00d0-49c7-84bd-c7fd3d4ba4a8" alt="App screenshot 1" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/abd59999-3fb3-40c5-9e01-2b9cdce06aef" alt="App screenshot 2" width="320" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/152ae76d-0600-4ca5-b1f4-8b249b9662cd" alt="App screenshot 3" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/acd5de22-742d-4e82-8757-786fc56b9857" alt="App screenshot 4" width="320" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/64a7a74b-b20c-48bd-9e4e-06a6930ce9d6" alt="App screenshot 5" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/84e53a9e-a1ec-4945-af56-58422c2dc8e3" alt="App screenshot 6" width="320" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/6ce03327-30ee-47e9-a8f4-75ebdd834980" alt="App screenshot 7" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/25bf8ee1-c879-4ce0-84b4-bf08665a3842" alt="App screenshot 8" width="320" /></td>
  </tr>
</table>
