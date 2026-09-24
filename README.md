# Higgsfield Mobile App

An Android client for exploring Higgsfield’s image and video generation models through a native conversational interface.

The app is intended for personal, local use while the Higgsfield API integration is developed. It will provide a focused workspace for writing prompts, refining results, reusing generated media, and comparing creative iterations across model families.

## Vision

Higgsfield Mobile will make model-based image and video generation feel like an intuitive creative conversation:

- Choose an Image or Video workspace.
- Select the model or workflow that matches the task.
- Write a prompt and receive an asynchronous generation result.
- Continue refining the latest successful result with a clear active-source preview.
- Preserve the creative brief, settings, attachments, and generation history.
- Save completed media to a folder selected by the user.

The API is stateless, so the app will explicitly compose context from the conversation’s creative brief, prompt history, selected source media, and compatible model options. It will never silently assume that the server remembers previous messages.

## Planned capabilities

### Image generation

- SOUL model family
- Marketing Studio image workflows
- Qwen Image 3 generation and editing
- Reference-image and image-editing workflows where supported
- Reusable prompts, settings, and source images

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
- Model capability and cost information beside model selection
- Compatibility checks for attachments, parameters, and editing workflows

### Local media and resilience

- Save generated images and videos to a user-selected folder
- Remember folder access using Android’s Storage Access Framework
- Keep generation history and metadata locally
- Show offline state without crashing or automatically submitting drafts
- Resume polling for accepted requests after connectivity returns or the app restarts
- Explain authentication, validation, moderation, quota, upload, download, and connectivity failures in the UI

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

Use the ignored `secrets.properties` file locally and the tracked [`secrets.properties.example`](secrets.properties.example) as a template. The app is not intended to be a production-distributed client while credentials are used directly from the mobile application, because values embedded in an APK can be extracted.

## Development status

The project foundation, adaptive Compose shell, fake-backed conversation experience, model catalog, persistence layer, and documentation are being built incrementally. Real endpoint schemas remain gated until they are verified against the current Higgsfield documentation.

Current delivery progress is tracked in [`docs/progress.md`](docs/progress.md).
