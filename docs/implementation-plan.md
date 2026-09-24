# Higgsfield Mobile Implementation Plan

## Product goal

Higgsfield Mobile is a personal-use Android client for creating images and videos through the Higgsfield API. It uses Kotlin, Jetpack Compose, Material 3, Navigation 3, and native REST calls. The app must remain useful without connectivity, recover accepted generations after process death, explain errors instead of crashing, and make iterative image editing visually obvious.

## Non-negotiable guardrails

- Do not commit or push without the user's explicit permission.
- Do not make a billable API request without separate explicit permission.
- Never place credentials in tracked files, logs, request diagnostics, tests, screenshots, or chat.
- Direct mobile authentication is a personal-build exception. Credentials are extractable from an APK, so this build must not be distributed.
- Generation POST requests are not retried after ambiguous timeouts. Status GET requests may retry with backoff.
- Offline drafts are editable but are never auto-submitted when connectivity returns.

## Delivery slices

1. Documentation, dependency management, edge-to-edge shell, theme, and Navigation 3.
2. Deterministic fake-backed Image and Video workspaces, including model choice, active source, branches, costs, errors, and offline states.
3. Room entities/DAOs, DataStore preferences, state restoration, and generation lineage.
4. Credential handling, HTTPS client, uploads, estimates, shared request lifecycle, connectivity, and WorkManager recovery.
5. SOUL as the first fully verified adapter and vertical slice.
6. Marketing Studio and Qwen Image adapters.
7. Seedance, Cinema Studio, and Wan adapters in family-sized patches.
8. Kling 2.x, Kling 3.x, O3, and Omni in smaller workflow patches.
9. Storage Access Framework downloads, Media3 playback, profiling, accessibility, screenshots, and hardening.

Each model is visible through the registry, but a workflow is enabled for real submission only after its current API schema, endpoint, estimate body, and fixture tests have been verified against the official workflow page. Soul ID training is excluded.

## Experience

Home presents Image and Video cards. Selecting either opens a persistent chat workspace. The workspace contains conversation history, model selection, a pinned Creative Brief, attachments, advanced options, an estimate state, generation results, and a composer.

The latest successful image is automatically displayed in a large **Editing this image** card above the composer. A follow-up such as “Make it red” sends that image to a compatible edit workflow. Failed, canceled, or moderated attempts never replace it. Selecting an older result creates a branch; detaching starts fresh. Multiple-image responses require the user to choose the next active source.

Requests are stateless. The app sends a deterministic composition of the current instruction and Creative Brief, not hidden chat history. Reuse actions explicitly carry prompts, options, seeds, attachments, or outputs forward.

Compact screens show one full-screen workspace with sheets for history and versions. Expanded screens reserve a conversation rail beside the workspace. Light, dark, and system themes use warm neutrals with blue and teal accents.

## API lifecycle

1. Validate the draft and model compatibility locally.
2. Build the exact endpoint-specific typed request DTO.
3. Request an estimate with a debounced copy of that exact body. Estimate failure does not block a valid request.
4. Upload local media through the presigned upload flow without API credentials on the storage request.
5. Submit once. Persist accepted request ID, status URL, cancellation URL, correlation ID, and a settings snapshot immediately.
6. Poll after two seconds, increasing toward ten seconds with jitter, until `completed`, `failed`, `nsfw`, or `canceled`.
7. Use foreground polling while visible and network-constrained WorkManager for accepted pending work after process death or restored connectivity.
8. Offer cancellation only while queued.

## Storage

Room stores conversations, briefs, generation lineage, request lifecycle, workflow snapshots, attachments, outputs, estimates, errors, and local download URIs. DataStore stores theme choice and the persisted download-folder URI. The system folder picker grants persisted access; Settings can change it. Remote outputs are labeled temporary because API retention is limited.

## Model rollout

- Image: SOUL, SOUL V2, SOUL Cinema; Marketing Studio 2.0 Alpha, 2.5 Flare, 2.5 Sunburst; Qwen Image 3 generation and editing.
- Video: Seedance 2.0 and 2.5; Kling 2.5 Turbo, 2.6, 2.6 Motion Control, 3.0, 3.0 Motion Control, O3, and Omni; Cinema Studio 4.0; Wan 2.6, 2.7, 3.0, and 3.0 Prime.

The registry records capabilities, required media, pricing factors, implementation state, and schema verification date. Every endpoint gets a typed DTO and adapter grouped by family; authentication, uploads, polling, persistence, and error mapping stay shared.

## Acceptance and verification

- “Create an apple” followed by “Make it red” uses the latest successful apple as the editing source.
- Accepted pending requests survive restart and resume status retrieval after connectivity returns.
- Unsupported inputs and options are rejected before submission with compatible alternatives.
- Expected API, moderation, credit, storage, and connectivity failures appear as actionable states and do not crash the app.
- Generated media can be saved to a remembered user-selected folder.
- Credentials never appear in tracked files or logs.
- Unit tests cover composition, validation, routing, DTOs, error mapping, compatibility, lineage, and polling.
- MockWebServer checks paths, headers, payloads, retry rules, and redaction without spending credits.
- Room, restoration, WorkManager, persisted-folder, Compose navigation, themes, accessibility, and adaptive layouts receive targeted tests.
- No Git commit or push occurs without explicit permission.

## Reference documentation

- [Higgsfield API overview](https://docs.higgsfield.ai/docs)
- [Model catalog](https://docs.higgsfield.ai/docs/models)
- [Authentication](https://docs.higgsfield.ai/docs/authentication)
- [File uploads](https://docs.higgsfield.ai/docs/concepts/file-uploads)
- [Polling](https://docs.higgsfield.ai/docs/concepts/polling)
- [Billing and retention](https://docs.higgsfield.ai/docs/concepts/billing-and-retention)
