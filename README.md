# Higgsfield Mobile

Higgsfield Mobile is a focused Android client for image and video creation workflows. It is designed for personal use with an explicit, opt-in API boundary: drafts, chat history, model metadata, and UI state work locally; a generation request is sent only when the user submits it.

## Product state

The refined product now includes:

- Image and video workspaces with model/workflow selection.
- Persistent, section-scoped conversations with a compact history drawer and an expanded rail.
- New-chat, rename, and chat-removal flows with confirmation and transition feedback.
- A Creative Brief for reusable intent, style, exclusions, and output guidance.
- Reference-image selection, direct single-reference CTA behavior, and multi-reference support where a workflow allows it.
- Static model capabilities, pricing, duration, and resolution metadata sourced from the supplied catalog material. Live estimates are intentionally not fabricated.
- Generation lifecycle cards for queued, running, succeeded, failed, and cancelled requests, including server-provided error details.
- Loading indicators for generation and larger workspace/history transitions.
- Light and dark themes, including mode-aware splash background and brand artwork.
- Offline-safe drafts and local persistence through Room.

## Architecture

The app is a Kotlin/Compose client organized around a presentation ViewModel, a Room-backed conversation/generation repository, and a small HTTP adapter for the Higgsfield endpoint. API calls are isolated behind the repository boundary so UI state can be exercised without a network request.

Important behavior is documented in [docs/architecture.md](docs/architecture.md). The implementation plan is now an acceptance checklist rather than a list of unfinished product slices: [docs/implementation-plan.md](docs/implementation-plan.md). Current verification and known limitations are recorded in [docs/progress.md](docs/progress.md).

## Safety and operational notes

- Do not commit credentials. `secrets.properties` and local API keys are ignored; use `secrets.properties.example` as the template.
- The debug build logs request/response diagnostics only for local troubleshooting. Sanitize logs before sharing them.
- Failed POSTs are not silently retried. Ambiguous outcomes remain visible so a user can decide whether to retry.
- Chat removal is explicit and cascades local conversation data; it does not attempt to delete provider-side history.
- Live generation and any billable operation remain user-triggered and require an authenticated environment.

## Verification

The current debug build has passing unit tests, Kotlin compilation, and APK assembly. Device visual QA and screenshot coverage are still useful follow-ups, but they are verification work rather than missing product functionality. See [docs/progress.md](docs/progress.md) for the exact commands and remaining limitations.


App Images and functionality demo:

<img width="720" height="1546" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM (1)" src="https://github.com/user-attachments/assets/91cc3bf2-00d0-49c7-84bd-c7fd3d4ba4a8" />          <img width="720" height="1485" alt="WhatsApp Image 2026-09-26 at 3 37 19 PM" src="https://github.com/user-attachments/assets/abd59999-3fb3-40c5-9e01-2b9cdce06aef" />



<img width="720" height="1504" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM" src="https://github.com/user-attachments/assets/152ae76d-0600-4ca5-b1f4-8b249b9662cd" />                     <img width="720" height="1494" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM (6)" src="https://github.com/user-attachments/assets/acd5de22-742d-4e82-8757-786fc56b9857" />



<img width="720" height="1531" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM (5)" src="https://github.com/user-attachments/assets/64a7a74b-b20c-48bd-9e4e-06a6930ce9d6" />              <img width="720" height="1547" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM (4)" src="https://github.com/user-attachments/assets/84e53a9e-a1ec-4945-af56-58422c2dc8e3" />



<img width="720" height="1544" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM (3)" src="https://github.com/user-attachments/assets/6ce03327-30ee-47e9-a8f4-75ebdd834980" />               <img width="720" height="1544" alt="WhatsApp Image 2026-09-26 at 3 37 20 PM (2)" src="https://github.com/user-attachments/assets/25bf8ee1-c879-4ce0-84b4-bf08665a3842" />
