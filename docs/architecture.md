# Architecture

## Shape

The first release is a single `app` module with feature-oriented packages. Module boundaries can be introduced when build time or ownership justifies them; package contracts are kept extractable from the start.

```text
com.higgsfield.mobile
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

The Android build also verifies native dependency compatibility. Compose's transitive `androidx.graphics:graphics-path` is pinned through the version catalog to the 16 KB-compatible `1.1.0` artifact; packaged native libraries are checked with 16 KB ZIP alignment during release verification.

Repositories persist the stable code and safe diagnostic message for generation history, then return the mapped `AppError` to callers. UI renders only the resource-backed message and allowed action; it never displays raw exceptions, HTTP bodies, credentials, signed URLs, or unfiltered server diagnostics. This keeps errors consistent across foreground work, polling, and WorkManager recovery while retaining useful, non-secret history for support and retry decisions.

## Model submission schemas

`core/network/SchemaWorkflowAdapter` is the only model-adapter implementation. Each verified catalog model contributes a `WorkflowRequestSchema` to `WorkflowRequestSchemas`, which maps a `GenerationDraft` to a JSON request body for the generic Retrofit submission method. Do not add a model-specific adapter class or placeholder schema. Add a route and schema only after the model-specific Higgsfield documentation confirms both. Extend shared request values when a documented route needs a new input type, rather than creating a per-model mapper.

## Core contracts

- `WorkflowId` is the stable local catalog identity. `WorkflowDescriptor.endpointPath` remains null until the exact API endpoint is verified; adapters cannot submit without it.
- `WorkflowDescriptor` describes family, media kind, tier, capabilities, required media, parameters, documentation, implementation state, and schema verification date.
- `GenerationDraft` contains the current instruction, pinned Creative Brief, workflow, attachments, and options.
- `WorkflowAdapter<Request>` validates a draft and converts it to a verified request shape. The shared schema adapter emits a JSON object for the generic submission boundary.
- `GenerationRepository` owns estimates, uploads, submission, cancellation, observation, persistence, and error mapping.
- `GenerationStatus` is a closed representation of queued, in-progress, completed, failed, NSFW, and canceled.

Schemas are grouped in one registry rather than adapter classes. Common lifecycle envelopes and the generic JSON boundary are shared; each enabled model still owns its verified route and field configuration.

## Request composition

Higgsfield is stateless. `PromptComposer` deterministically joins non-empty Creative Brief fields and the current instruction. When supported, exclusions are mapped to the DTO's `negative_prompt`; they are not duplicated into hidden history. The exact composed draft and option snapshot are persisted with every generation.

## Iteration and lineage

Every generation has an optional parent generation and branch root. A conversation has at most one active source attachment. Completion rules are explicit:

- One successful image automatically becomes the active source.
- Multiple successful images wait for explicit selection.
- Failed, NSFW, and canceled results leave the source unchanged.
- Selecting an older output sets it as source and creates the next request on a new branch.
- Detaching clears the source and creates a fresh generation.
- An incompatible workflow blocks submission and returns compatible edit/reference workflows.

The linear timeline remains the primary UI. A Versions/Compare sheet visualizes parent-child relationships without turning the composer into a graph editor.

## State and recovery

Room is the durable source of truth for conversations and accepted requests. A submit transaction writes the local generation first; once the server accepts, IDs and URLs are persisted immediately. Foreground observation polls responsively. WorkManager only resumes already-accepted requests under a network constraint; it never submits a draft.

Polling starts at two seconds and grows toward ten seconds with jitter. Terminal statuses stop polling. Status GET requests can retry safely. Submission POST requests are single-attempt because the API has no idempotency key; an ambiguous timeout becomes an actionable “unknown submission outcome” state.

## Security boundary

`secrets.properties` is ignored and loaded into local `BuildConfig` values. Missing values are valid build configuration and disable API actions. The authorization interceptor combines key ID and secret only in memory. HTTP logging is limited to safe metadata and redacts authorization; request/response bodies, signed URLs, and credentials are never logged.

All traffic is HTTPS. Presigned upload requests use a separate unauthenticated client so Higgsfield credentials cannot reach the storage host. Android backups are disabled. Only the launcher Activity is exported. This design is explicitly for a private personal build: secrets embedded in an APK remain extractable.

## Offline behavior

Validated connectivity is observable UI state. Drafts, briefs, navigation, and local history work offline. The Generate action explains that a connection is required. Connectivity restoration resumes polling accepted remote jobs through WorkManager, but never auto-submits a draft or retries an uncertain POST.

## Downloads

The system Storage Access Framework selects a folder and grants persistable URI access. DataStore remembers the URI. Downloads stream into a user-created document and Room records the resulting content URI. No broad storage permission is requested.

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
