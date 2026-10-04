# Architecture

## Shape

The first release is a single `app` module with feature-oriented packages. Module boundaries can be introduced when build time or ownership justifies them; package contracts are kept extractable from the start.

```text
com.promptstudio.app
├── core
│   ├── model          immutable domain contracts and workflow registry
│   ├── data           repositories and fake/production implementations
│   ├── database       Room entities, DAOs, database, and mappings
│   ├── network        API DTOs, Retrofit services, auth, polling, errors
│   ├── preferences    DataStore theme and download-folder preferences
│   ├── connectivity   observable validated-network state
│   └── work           recovery of accepted pending requests
├── di                 Hilt modules
├── feature
│   ├── home
│   ├── conversation
│   └── settings
└── ui
    ├── navigation     serializable Navigation 3 keys and app shell
    └── theme
```

UI reads immutable state and emits events to ViewModels. ViewModels call repository contracts. Repositories coordinate local and remote data sources and expose domain models; Composables do not know Retrofit or Room types.

## Code and UI text conventions

- Domain and data code must not repeat meaningful string literals. Define constants for stable values such as persistence keys, API fields, status codes, MIME types, and workflow-independent messages.
- Keep constants as narrow as possible: a value used only by one file belongs in that file as a private constant; promote it to a feature or shared contract only when it has multiple consumers.
- UI code must use Android resource IDs for user-visible copy rather than hardcoded text. UI state and events should carry a `@StringRes` identifier (and formatting arguments when needed) where copy must cross a layer boundary; Compose resolves it with `stringResource`.
- Do not introduce a global constants dump. Constants are grouped with the feature or data boundary that owns their meaning.

## Error boundary

`core/error` is the single boundary for application errors. `ErrorMapper` converts network, storage, validation, and unexpected failures into a closed `AppError` contract containing a stable error code, a user-facing string resource ID, retry guidance, and an optional safe diagnostic message.

The Android build verifies native dependency compatibility. Unused native dependencies are excluded, and packaged native libraries must pass both 16 KB ZIP alignment and ELF `LOAD`/`GNU_RELRO` alignment checks during release verification.

Repositories persist the stable code and safe diagnostic message for generation history, then return the mapped `AppError` to callers. UI renders only the resource-backed message and allowed action; it never displays raw exceptions, HTTP bodies, credentials, signed URLs, or unfiltered server diagnostics. This keeps errors consistent across foreground work, polling, and WorkManager recovery while retaining useful, non-secret history for support and retry decisions.

## Model submission schemas

`core/network/SchemaWorkflowAdapter` is the only model-adapter implementation. Each verified catalog model contributes a `WorkflowRequestSchema` to `WorkflowRequestSchemas`, which maps a `GenerationDraft` to a JSON request body for the generic Retrofit submission method. Do not add a model-specific adapter class or placeholder schema. Add a route and schema only after the model-specific API documentation confirms both. Extend shared request values when a documented route needs a new input type, rather than creating a per-model mapper.

## Core contracts

- `WorkflowId` is the stable local catalog identity. `WorkflowDescriptor.endpointPath` remains null until the exact API endpoint is verified; adapters cannot submit without it.
- `WorkflowDescriptor` describes family, media kind, tier, capabilities, required media, parameters, documentation, implementation state, and schema verification date.
- `GenerationDraft` contains the current instruction, pinned Creative Brief, workflow, attachments, and options.
- `WorkflowAdapter<Request>` validates a draft and converts it to a verified request shape. The shared schema adapter emits a JSON object for the generic submission boundary.
- `GenerationRepository` owns estimates, uploads, submission, cancellation, observation, persistence, and error mapping.
- `GenerationStatus` is a closed representation of queued, in-progress, completed, failed, NSFW, and canceled.

Schemas are grouped in one registry rather than adapter classes. Common lifecycle envelopes and the generic JSON boundary are shared; each enabled model still owns its verified route and field configuration.

The conversation state derives local attachment slots from the selected workflow. A user assigns each picked item to the declared media role (for example, a source image or motion-reference video); changing models discards slots unsupported by the new workflow. These are local draft references until the existing secure upload lifecycle produces public URLs, so the UI never treats a picked device URI as remotely submittable media.

## Request composition

The prompt flow is stateless. `PromptComposer` deterministically joins non-empty Creative Brief fields and the current instruction. When supported, exclusions are mapped to the DTO's `negative_prompt`; they are not duplicated into hidden history. Each generation stores the exact composed prompt, negative prompt, options, source output ID, and attachments. Retry uses these submission-time values instead of recomposing with the conversation's current brief.

Composer drafts are stored separately per conversation in Room (`conversation_drafts`), with a version 1-to-2 migration that preserves existing chats and generations. Normal draft writes are debounced and flushed at key transitions; serialization of overlapping writes remains planned. Reusing a generation's parameters writes its selected model, brief, draft, and cleared active source in one Room transaction. Workflow and source selection update the visible composer after their persistence call succeeds, and late snapshots are held until they confirm the new selection.

App launch and New Chat open a local, unsaved workspace. Model, media, brief, and option browsing does not insert a conversation row. The first nonblank prompt creates a conversation and its initial draft in one transaction; a prompt-free media route also creates one when the user submits it. Opening a saved conversation only observes its existing row, and removing the last chat returns to the unsaved workspace.

## Attachment upload boundary

The conversation draft retains each picked `content://` URI with its explicit media role. `SecureAttachmentUploader` resolves its MIME type through `ContentResolver`, validates that it matches the declared image/video/audio kind, requests a provider upload ticket, and streams the content to the presigned URL without buffering the complete file. Both the presigned URL and returned public URL must use HTTPS, and the ticket may not change the requested MIME type.

Storage uploads pass through the dedicated unauthenticated client, which strips any authorization header. The uploader returns either attachments containing public URLs or a centralized `AppError`; it never exposes provider errors directly to Compose. Existing valid HTTPS remote attachments are reused without reading local content or uploading again.

## Iteration and lineage

Every generation has an optional parent generation and branch root. A conversation has at most one active source attachment. Completion rules are explicit:

- One successful image automatically becomes the active source.
- Multiple successful images wait for explicit selection.
- Failed, NSFW, and canceled results leave the source unchanged.
- Selecting an older output sets it as source and creates the next request on a new branch.
- Detaching clears the source and creates a fresh generation.
- An incompatible workflow blocks submission and returns compatible edit/reference workflows.

The linear timeline remains the primary UI. A Versions/Compare sheet visualizes parent-child relationships without turning the composer into a graph editor.

The compact history surface remains a bottom sheet. It dismisses before creating or switching chats, and its list scrolls only after full expansion; expanded layouts keep a scrollable rail. A workspace-loading overlay stays visible until both the conversation snapshot and generation records have been observed. Result actions distinguish selecting an image as the next edit source from restoring a generation's original prompt, model, options, brief, and references. Image/video previews expose loading and retryable failure states instead of relying on the placeholder gradient.

## State and recovery

Room is the durable source of truth for conversations and accepted requests. A submit transaction writes the local generation first; once the server accepts, IDs and URLs are persisted immediately. Opening the app polls accepted requests across conversations, and foreground observation also polls the current conversation. A recovery worker exists but is not scheduled in this release; polling while the app is closed is deferred.

Polling starts at two seconds and grows toward ten seconds with jitter. Terminal statuses stop polling. Status GET requests can retry safely. Submission POST requests are single-attempt because the API has no idempotency key; an ambiguous timeout becomes an actionable “unknown submission outcome” state.

## Security boundary

`app/secrets/secrets.properties` is ignored and loaded into local `BuildConfig` values. Missing values are valid build configuration and disable API actions. The authorization interceptor combines key ID and secret only in memory. HTTP logging is limited to safe metadata and redacts authorization; request/response bodies, signed URLs, and credentials are never logged.

All traffic is HTTPS. Presigned upload requests use a separate unauthenticated client so API credentials cannot reach the storage host. Android backups are disabled. Only the launcher Activity is exported. This design uses build-time API credentials: secrets embedded in an APK remain extractable, so distributable builds must not include a maintainer's keys.

## Offline behavior

Validated connectivity is observable UI state. Drafts, briefs, navigation, and local history work offline. The Generate action explains that a connection is required. Reopening the app resumes polling accepted remote jobs, but never auto-submits a draft or retries an uncertain POST.

## Downloads

The system Storage Access Framework selects a folder and grants persistable URI access. DataStore remembers the URI. Downloads stream into a user-created document and Room records the resulting content URI. The app takes a persistable read grant for newly created downloads; previews use a saved local URI only while that grant exists, otherwise they use the remote output URL. This also keeps older downloads with missing grants from breaking media previews after app relaunch. No broad storage permission is requested.

## Adaptive and edge-to-edge UI

`enableEdgeToEdge()` runs before `setContent`. The launcher Activity uses `adjustResize`. A top-level Scaffold owns system-bar insets; the composer consumes IME/navigation padding once. Compact screens show one workspace; expanded widths render a conversation rail and workspace side-by-side. Touch targets, semantics, contrast, font scaling, and reduced-motion behavior are acceptance concerns, not polish work.

## Test seams

- Fake repository and deterministic clock/random sources for UI and polling tests.
- Pure prompt composition, compatibility, lineage, and adapter validation.
- Serialized request tests for the shared schema adapter and each enabled workflow shape.
- MockWebServer for authentication redaction, exact paths/bodies, status retries, and no-POST-retry behavior.
- In-memory Room plus migration tests.
- Compose tests across compact/expanded widths, light/dark themes, large font, offline and terminal states.

Real API calls are never part of automated verification and require explicit billable-operation approval.
## Production generation submission boundary

`RoomGenerationRepository` is the single production path for a deliberate Generate tap. It persists a draft and local attachment metadata first, uploads only attachments that lack a verified HTTPS public URL, maps the uploaded draft with the selected verified schema, and makes exactly one generation POST. An accepted response must contain a trusted HTTPS status URL and request ID before it is marked queued; the existing status synchronizer then reconciles status without repeating the POST. Authentication is deliberately not pre-checked: a build without local credentials reaches the real API boundary on a user tap and records the returned authentication failure. No submission, upload, or estimate runs automatically.

Debug builds add a verbose, sanitized OkHttp interceptor for the authenticated API client. It logs method, host/path, query parameters, ordinary headers, status, timing, and JSON request/response bodies so manual failures can be diagnosed. Authorization/secret headers, signed query values, and private media URL fields are redacted. The separate presigned upload client retains route/status-only logging and never logs binary media or signing values. Release builds have no network logging interceptor.

## Repository restoration and accepted-request boundary

`RoomGenerationRepository` observes the persisted conversation together with generation media relations, so restored domain records retain the Creative Brief, generation options, attachments, and completed outputs rather than rebuilding a partial draft. `UNKNOWN_SUBMISSION_OUTCOME` has a distinct domain state: it records an ambiguous generation POST without retrying it. The repository depends on the narrow `GenerationRequestSynchronizer` interface; production binds it to `RequestStatusSynchronizer`, while integration tests can isolate the one-shot POST from status polling safely.

## Generation lifecycle presentation

The conversation timeline is a projection of observed `GenerationRecord`s, not a local optimistic demo. It presents queued, generating, completed, failed, moderated, canceled, and unknown-submission states from their domain status. A foreground `RequestStatusPoller` starts only for accepted or restored queued/in-progress records and stops at a terminal state; it makes status-only requests. Completed outputs can become the active editing source, retry is exposed only for retryable failures, and cancellation is exposed only while a request is queued. If the server rejects a stale queued cancellation because processing already started, the app immediately fetches the current status, never marks the request canceled, and preserves the server's rejection message.

## Output rendering and retention

Completed cards render the actual persisted `GenerationOutput`: Coil loads images and Media3 plays video or audio. Remote outputs are visibly temporary. Download uses the system create-document flow, streams a validated HTTPS output to the user-selected URI, and records that URI in Room only after the write completes. The next observed record uses the local copy; a failed download leaves the remote output intact and reports the centralized error.

## Model-aware settings and estimates

`WorkflowDescriptor` declares its verified adjustable options and static estimate metadata. The conversation stores those selected options in `GenerationDraft` and clears only options unsupported by a newly selected workflow. The settings sheet renders no unsupported controls. Prices, credits, and latency remain unavailable until manually entered with a documentation URL and verification date; the UI never treats them as live values or manufactures a cost.

Direct model variants are registered as separate descriptors in `DirectModelRoutes.kt` and serialized by `DirectModelRequestSchemas.kt`. Each descriptor declares its endpoint, named media roles, required and maximum counts, supported controls, allowed choices, and numeric bounds. The composer renders those roles and validates them before submission; the request adapter validates again after uploaded URLs are available. The settings sheet keeps route-specific values in the draft, and Room migration 2→3 stores them as JSON so a restored draft or retry uses the same controls. `ModelStartingRates.kt` owns the manually editable starting-price labels. Each route inherits its main model's rate unless a verified route override is added. Unverified rates display as unavailable. These are display estimates, not a billing calculation.

Guided workflows are a separate future feature. Their API behavior and required UI states are recorded in `docs/workflow-api-future.md`; direct model variants remain available without a preset-fetching flow. `docs/model-route-inventory.md` tracks endpoint coverage and `docs/model-controls-audit.md` tracks control gaps. Undocumented media maxima remain unspecified in the UI rather than being guessed.

## Workspace composition

The workspace uses a compact identity and connection top bar. A single strip directly below owns model selection and the static estimate entry point. Its modal details sheet blocks interaction behind it. The composer exposes media and settings only when the selected workflow supports them, keeps the Creative Brief as a secondary action, and uses one primary Generate action. Attachment visibility and composer size animate functionally; active requests use real indeterminate or determinate progress rather than demo loading copy.

## Accessibility and performance

The conversation workspace labels connection state and generation cards for accessibility services and marks workspace identity as a heading. It remains adaptive at compact and expanded widths, retains IME padding on the composer, and uses keyed/content-typed timeline items. Functional attachment/composer motion observes the system animator setting: with animations disabled, the same state updates occur immediately without spatial transitions. Media players are scoped to each output and released through Compose disposal.
