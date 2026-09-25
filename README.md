# UNOFFICIAL Higgsfield API Mobile App

An Android client for exploring Higgsfield’s image and video generation models through a native conversational interface.

The app is intended for personal, local use while the Higgsfield API integration is validated. It provides a focused workspace for writing prompts, refining results, and comparing creative iterations across model families.

## Vision

Higgsfield Mobile will make model-based image and video generation feel like an intuitive creative conversation:

- Choose an Image or Video workspace.
- Select the model or workflow that matches the task.
- Write a prompt and receive an asynchronous generation result.
- Continue refining the latest successful result with a clear active-source preview.
- Preserve the creative brief, settings, attachments, and generation history.
- Save completed media to a folder selected by the user.

The API is stateless, so the app will explicitly compose context from the conversation’s creative brief, prompt history, selected source media, and compatible model options. It will never silently assume that the server remembers previous messages.

## Current capabilities

### Image generation

- SOUL model family
- Marketing Studio image workflows
- Qwen Image 3 generation and editing
- Reference-image and image-editing workflows where supported
- Reusable prompts, settings, and source images where the selected workflow supports them

### Video generation

- Seedance workflows
- Kling workflows, including motion-control variants where supported
- Cinema Studio
- Wan workflows
- Asynchronous progress, cancellation, retry, and result recovery

### Creative sessions

- Full-screen chat-style generation workspace
- Persistent creative brief for subject, style, mood, camera direction, requirements, exclusions, and output goal
- Active-source card showing which image will be used for the next edit
- Versions, comparisons, branches, and parent-child generation lineage
- Capability-driven settings and static estimate metadata beside model selection
- Compatibility checks for attachments, parameters, and editing workflows

### Local media and resilience

- Render remote images with Coil and remote video/audio with Media3
- Save generated images, videos, and audio to a user-selected location
- Remember folder access using Android’s Storage Access Framework
- Keep generation history and metadata locally
- Show offline state without crashing or automatically submitting drafts
- Resume polling for accepted requests after connectivity returns or the app restarts
- Explain authentication, validation, moderation, quota, upload, download, and connectivity failures in the UI

## Implemented product state

Slices 1–9 and Slice 10.1–10.6 are implemented. The app currently includes:

- Room-backed generation records with draft, attachment, output, lineage, and lifecycle restoration.
- Real endpoint-specific request schemas behind a shared adapter for the catalogued model families.
- Secure local-media upload handling, authenticated API requests, status polling, cancellation, WorkManager recovery, and sanitized debug HTTP logging.
- Queued, generating, completed, failed, moderated, canceled, and ambiguous-submission UI states with retry/cancel actions.
- Actual output rendering, Media3 playback, system save-document downloads, temporary remote-output labeling, and persisted local download URIs.
- A compact workspace redesign with model-aware controls, estimate/details sheet, capability-gated composer actions, reduced-motion behavior, and accessibility semantics.

Slice 10.7 is ready but intentionally not run yet. It requires one explicitly approved real-device/API request using the local credentials, followed by status-lifecycle, restart recovery, media download, and 16 KB compatibility verification.

## Architecture direction

The project uses Kotlin, Jetpack Compose, Material 3, and a feature-oriented Android architecture.

- Unidirectional data flow with immutable UI state and ViewModels
- Hilt for dependency injection
- Navigation 3 for app navigation
- Room for conversations, requests, outputs, attachments, and generation lineage
- DataStore for user preferences such as theme and download-folder access
- Retrofit/OkHttp for the shared HTTP lifecycle
- WorkManager for recovering pending requests
- Typed model-family adapters so each Higgsfield endpoint can have its own request DTO and validation rules
- Shared authentication, upload, polling, cancellation, persistence, and error mapping infrastructure

The model catalog is centralized so model IDs and display names are not repeated as hardcoded literals throughout the UI. User-facing Compose text belongs in Android string resources.

See the project documents for the detailed plan:

- [`docs/implementation-plan.md`](docs/implementation-plan.md)
- [`docs/architecture.md`](docs/architecture.md)
- [`docs/progress.md`](docs/progress.md)

## Security and API usage

Higgsfield credentials are local-only configuration. Do not commit real credentials, paste them anywhere, or distribute builds containing them.

Use the ignored `app/secrets/secrets.properties` file locally with the same `HF_KEY_ID` and `HF_KEY_SECRET` entries shown in [`secrets.properties.example`](secrets.properties.example). The app is not intended to be a production-distributed client while credentials are used directly from the mobile application, because values embedded in an APK can be extracted.

## Development status

The project is at the final pre-live verification stage. Automated verification currently passes with 38 unit tests, zero lint errors, and a successful debug APK build. New Compose/UI tests are intentionally deferred under the temporary Slice 10 test rule; affected non-UI tests continue to be maintained.

Current delivery progress is tracked in [`docs/progress.md`](docs/progress.md).
