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
- `WorkflowCatalog` is the single source of truth for all model IDs and user-visible model names. Do not repeat model identifiers or display names as literals elsewhere in code or tests.
- Compose-visible copy belongs in Android string resources. Non-Compose user-facing state messages must use centralized reusable copy until they migrate to a resource-backed UI-text type.

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
10. Website-inspired conversation workspace redesign: model-aware settings, estimates, action/chat motion, and composer polish.

Each model is visible through the registry, but a workflow is enabled for real submission only after its current API schema, endpoint, estimate body, and fixture tests have been verified against the official workflow page. Soul ID training is excluded.

## Experience

Home presents Image and Video cards. Selecting either opens a persistent chat workspace. The workspace contains conversation history, model selection, a pinned Creative Brief, attachments, advanced options, an estimate state, generation results, and a composer.

The latest successful image is automatically displayed in a large **Editing this image** card above the composer. A scoped follow-up such as “Change only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing” sends that image to a compatible edit workflow. Failed, canceled, or moderated attempts never replace it. Selecting an older result creates a branch; detaching starts fresh. Multiple-image responses require the user to choose the next active source.

Requests are stateless. The app sends a deterministic composition of the current instruction and Creative Brief, not hidden chat history. Reuse actions explicitly carry prompts, options, seeds, attachments, or outputs forward.

Compact screens show one full-screen workspace with sheets for history and versions. Expanded screens reserve a conversation rail beside the workspace. Light, dark, and system themes use warm neutrals with blue and teal accents.

## Planned experience improvements

The conversation workspace will be redesigned with a compact top bar that contains only navigation, workspace identity, and connection state. Model selection and its estimate card will sit directly below it. The composer will own secondary actions and expose a single primary generation action.

- Replace the current information dialog with a model estimate/details sheet. It will show a documentation-backed, static indicative price/range and the factors that affect cost until live estimates are authorized and implemented; never invent prices or make an estimate request solely to populate the UI.
- Keep Creative Brief persisted but move it into a collapsed, optional prompt-settings section instead of the top bar.
- Add explicit per-workflow option metadata for aspect ratio, resolution, duration, seed, negative prompt, audio, and reference media. Render only supported controls and clear only values invalidated by a model switch.
- Replace the generic options dialog with a model-aware settings sheet using compact chips or segmented controls.
- Add subtle functional motion for chat insertion/reordering, action visibility, attachment cards, model changes, sheets, and estimate cards. Respect the Android reduced-motion preference.
- Improve empty states, composer spacing, visual hierarchy, cards, contrast, and touch targets in a Higgsfield website-inspired editorial direction.

### Locked design specification

- Treat the supplied three-screen dark mockup as the visual reference, not a source of model names, prices, timing, credits, or other factual data.
- Use one consistent user-message color unless color communicates a documented state. Remove the unused vertical band beneath the navigation area and prioritize conversation space.
- Keep the top bar compact. Use a compact model chip rather than a dominant dropdown, then place one model/estimate strip directly beneath the bar. Do not repeat the same estimate across the toolbar, composer, and output card.
- The estimate strip opens a modal model-details sheet that dims the workspace and covers or disables the composer. It shows verified starting price/range, credit guidance, expected latency when available, supported settings, and the verification date.
- Pricing is maintained as static per-workflow metadata because no supported live pricing endpoint has been verified. Keep values in the model/data registry rather than UI code, record the source URL and `verifiedOn` date, and update them manually when Higgsfield pricing changes.
- Display static values as `Estimated` or `From` rather than `Live`. Resolve documented option-dependent prices when an exact combination is known; otherwise show `Pricing unavailable` instead of calculating or inventing a value.
- The composer contains the prompt, attachment action, capability-driven preset/settings actions, and one prominent send/generate button. Avoid presenting every possible action simultaneously.
- Hide `Reference`, `Presets`, aspect ratio, resolution, duration, seed, negative prompt, audio, and other controls whenever the selected workflow does not support them. `Reuse parameters` appears only on completed outputs whose settings can be reused.
- Label attachments by their assigned role, such as `Source image`, `Reference image`, or `Motion-reference video`; do not use generic preview headings.
- Render completed generations as media cards with compact verified metadata and context-valid actions such as Download, Share, and Reuse parameters. Metadata must remain legible and meet contrast requirements.
- Loading uses skeleton media, determinate progress when the API supplies it, and a meaningful status such as `Generating`; never expose implementation labels such as `Progressive Loading State`.
- Motion is short and functional: message insertion, output-card placement, attachment and action visibility, model/estimate crossfades, and the loading-to-result transition. Reduced-motion mode removes spatial movement while preserving immediate state feedback.
- The redesigned workspace must remain usable at compact and expanded widths, with large fonts, dark theme, IME visibility, and system gesture/navigation insets.

### Improvements backlog

- Add repository-level persistence/network integration coverage with in-memory Room and MockWebServer before the first credentialed manual call.
- Add live model estimates after authenticated estimate bodies are verified and billable operation approval is granted.
- Add Compose screenshot coverage for compact, expanded, dark-theme, large-font, and reduced-motion states.
- Complete downloads, accessibility audit, and performance profiling.
- Re-verify and manually update static pricing metadata whenever Higgsfield changes its model catalog or pricing. Replace this policy only if a supported pricing API is documented and adopted deliberately.

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

- “Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text” followed by “Change only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing” uses the latest successful apple as the editing source.
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
