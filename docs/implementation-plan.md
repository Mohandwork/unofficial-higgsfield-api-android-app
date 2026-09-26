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

## Experience

Home lets the user enter an Image or Video workspace. Each workspace contains multiple persistent chats rather than one permanent conversation.

The active workspace shows a chat-history rail on expanded screens and a history drawer or sheet on compact screens. Users can create a new chat, reopen an older chat, rename chats, and switch between chats without changing the saved history of other chats.

Each active chat contains its own model selection, Prompt Settings including the Creative Brief, references, options, generation timeline, results, and composer. The message timeline is distinct from the chat-history list: the timeline shows generations inside the selected chat, while the history list switches between chats.

The latest successful compatible output can become the active source for the next request. A scoped follow-up such as “Change only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing” sends that source together with the current prompt and settings to a compatible workflow. Failed, canceled, or moderated attempts never replace it. Selecting an older result creates a branch; detaching starts fresh. Multiple-image responses require the user to choose the next active source.

Requests are stateless. The app sends a deterministic composition of the current prompt, Prompt Settings, Creative Brief, references, and options; it does not silently send hidden chat history. Reuse actions explicitly carry the exact prompt, options, seeds, attachments, and source lineage forward.

Light, dark, and system themes use warm neutrals with blue and teal accents. The workspace remains adaptive across compact and expanded widths, with the chat-history rail/drawer, generation timeline, and composer receiving deliberate responsive layouts.

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
- Interaction continuity is mandatory. UI state changes must transition smoothly into their new position or visibility state; elements must not abruptly vanish, resize, or reappear without a deliberate transition. Navigation, model changes, source attachment/detachment, composer actions, sheets, and result updates must preserve visual continuity and clearly communicate what changed.
- The redesigned workspace must remain usable at compact and expanded widths, with large fonts, dark theme, IME visibility, and system gesture/navigation insets.

### Concept gaps recorded from the reference review

The current implementation has the data and API foundation, but the reference concept requires a dedicated product-UI pass. These requirements are now explicitly recorded as the next implementation order:

1. **Fix active-source dismissal and navigation state.** The `Editing this image` card must disappear immediately when its X action is pressed and remain detached after Room observation. Image and Video are persistent top tabs; switching tabs changes model selection, chat state, and preset choices without destroying the broader session history. Back, model selection, sheets, and composer actions must have deliberate transitions and no stale-state restoration.
2. **Build the actual top bar.** Center Image/Video tabs, place the compact model dropdown in the top bar, and put the `!` info action beside that selector. The info action is exclusively for the model details sheet; it must not be confused with Creative Brief.
3. **Replace generic timeline cards with media-first feed cards.** Each user prompt sits above a generated media card. Completed cards need a real media surface, compact metadata footer, model name, render duration, and cost/credit information. Loading cards need a media skeleton, dynamic progress or estimated timer, and a meaningful `Generating` state.
4. **Populate documented static model information.** Add per-workflow description, strengths, supported media, duration/FPS/resolution/aspect-ratio limits, cost structure, latency guidance, source URL, and verification date. Use the official pricing page (`https://open.higgsfield.ai/pricing`) and each model’s API reference as sources. Show documented static values as `From`/`Estimated`; show actual usage deduction only when the API returns it; never replace this with a generic `Pricing unavailable` dialog when documented values exist.
5. **Rebuild the bottom input dock.** Add the selected-reference preview with thumbnail and X, an auto-expanding prompt field, a high-contrast floating send button, and a pill row containing `+ Ref`, `Presets`, and the estimate. Keep the Creative Brief as an optional prompt-settings section rather than an information icon.
6. **Implement the preset/spec sheet.** Render only options supported by the active workflow. Image settings include verified aspect ratios, resolution, seed, and documented style presets. Video settings include verified aspect ratios, duration/FPS, motion strength, and documented camera-motion presets. Do not show controls that are not present in that model’s schema.
7. **Complete result actions.** Add Share, Copy Prompt, and Reuse Parameters. Reuse must restore the exact prompt, seed, preset, options, attachments, and source lineage that produced the selected output. Actions must be context-valid and use the same media-card action bar as the reference.
8. **Add the missing interaction polish.** Implement message insertion, result-card placement, action visibility, model/estimate crossfades, reference attachment transitions, loading-to-result transitions, bottom-sheet transitions, and completion haptics. Respect reduced-motion settings while preserving state feedback.
9. **Perform visual QA against the reference.** Verify dark-first colors, cyan/violet accents, card radii, spacing, typography, contrast, touch targets, system insets, compact/expanded widths, large text, keyboard behavior, and the absence of the unused vertical band.

## Follow-up product plans

The reference review also identified two connected product gaps that require explicit tracing beyond the original slice checklist: Creative Brief discoverability and multi-conversation history. These plans are intentionally recorded before implementation so the product behavior, persistence boundaries, and acceptance criteria remain clear.

### Product plan A: explain and improve Creative Brief

#### Goal

Make Creative Brief understandable and useful to a first-time user. It is persistent creative direction for the current workspace, explicitly added to every future request; it is not hidden chat history or model memory.

#### UX requirements

- Replace the opaque info-icon entry point with a clearly labeled `Prompt settings` / `Creative brief` control near the composer.
- Show a compact state summary: `No brief yet — prompts stand alone` or `Brief active — applied to every generation`.
- Open a bottom sheet with this explanation: “Use this for creative direction that should stay consistent across requests. Your prompt describes what you want to do now.”
- Explain that the brief is saved for the workspace, is added explicitly to each request, and does not change previous generations.
- Expose all seven fields in three groups:
  - What you’re making: Subject and Output goal.
  - Look and feel: Style, Mood, and Camera direction.
  - Guardrails: Requirements and Avoid.
- Provide `Clear brief` and `Save` actions.
- Include one explanatory, non-applied example:
  - Subject: “A single matte-black travel mug”.
  - Style and mood: “Premium editorial product photography, calm and minimal”.
  - Camera: “Eye-level three-quarter view, soft daylight, shallow depth of field”.
  - Requirements: “One mug, centered, clean cream background”.
  - Avoid: “Hands, logos, extra objects, text”.
  - Output goal: “A hero image for a product page”.
  - Current prompt: “Make the mug red and add subtle condensation.”
- Explain that the brief keeps the product, look, and constraints consistent while the current prompt requests the specific change for one generation. The example must never modify the user’s saved brief.

#### Request behavior

- Persist and edit all seven `CreativeBrief` fields.
- Keep the brief separate from the one-off prompt.
- For workflows with negative-prompt support, map `Avoid` into the workflow negative-prompt field and combine it deterministically with any one-off advanced negative prompt.
- For workflows without negative-prompt support, include `Avoid: …` in the composed prompt so the field is never silently ignored.
- Persist the exact composed prompt and negative-prompt result with each generation.

#### Acceptance and tests

- Every brief field persists and restores with the workspace.
- An empty brief leaves the user prompt unchanged.
- Supported workflows receive Brief exclusions as negative prompts.
- Unsupported workflows receive the explicit prompt fallback.
- Editing or clearing the brief affects only future generations.
- A new user can understand the feature without reading project documentation.

### Product plan B: persistent chat history and multiple chats

#### Goal

Replace the static conversation rail with real chat history. Users can see old Image and Video chats, reopen them, create new chats without losing the current one, and rename chats.

#### Conversation model and navigation

- Change navigation from `ConversationKey(mediaKind)` to `ConversationKey(conversationId, mediaKind)`.
- Give every chat a stable ID and persist its media type, title, selected model, Creative Brief, active source, and generation history.
- Show one history sidebar with Image and Video sections; each chat retains its own state.
- Switching the Image/Video tab opens the most recently used chat for that type; if none exists, create a blank chat of that type.
- Keep existing default Image and Video conversations available in history.
- Initialize the conversation ViewModel by `conversationId`, not by hard-coded `image-default` or `video-default` IDs.

#### Sidebar behavior

- Add `New chat`.
- Highlight the active chat.
- Show title, media type/icon, and last-updated time.
- Open an existing chat without modifying other chats.
- Create new chats as `New image chat` or `New video chat`.
- After the first Generate action, derive a short local title from the first prompt; allow rename from the chat row/menu.
- Leave archive and delete out of this pass.
- Keep a persistent rail on expanded screens and expose the same history through a drawer or sheet on compact screens.

#### Draft-switch behavior

- Because unsent prompts and attachment drafts are not independently persisted yet, warn before switching when the current chat has unsent changes.
- `Continue editing` keeps the current chat open.
- `Switch chat` discards only the unsent prompt and draft attachments; saved generations, Creative Brief, model, options, and active source remain persisted.

#### Acceptance and tests

- Multiple Image and Video chats have independent histories.
- Reopening an older chat restores its model, brief, active source, and timeline.
- `New chat` never overwrites the current chat.
- Automatic titles, rename behavior, ordering, and active-row highlighting work correctly.
- Clean-draft switching is immediate.
- Dirty-draft switching shows the warning and respects cancel/confirm.
- Compact history drawer and expanded history rail provide equivalent behavior.

### Next phase: conversation workspace redesign

The existing workspace does not yet meet the supplied dark, media-first design concept. This redesign supersedes the earlier Creative Brief-first priority. Treat the supplied three-phone concept as the binding visual and interaction reference while retaining the safety and data-accuracy rules in this plan.

1. Rebuild the primary header: centered Image/Video tabs, a compact model selector, and an adjacent model-info action. The model-info action opens a dimming modal bottom sheet; it is not an entry point for Creative Brief.
2. Rebuild the timeline as a media-first feed. A user prompt bubble precedes each generated-media card. Completed cards show media, model, elapsed render time, API-returned cost when available, and Download, Share, Copy Prompt, and Reuse Parameters actions. Active generations render a skeleton media surface with real progress or a documented estimate, never placeholder implementation copy.
3. Rebuild the bottom dock from the reference: selected-reference thumbnail card with remove action; auto-expanding prompt field; high-contrast floating generate button; and a compact action row containing `+ Ref`, model-aware `Presets`, and one estimate indicator.
4. Rebuild model details and presets as deliberate sheets. The details sheet displays only documented, verified capability, pricing, credit, latency, and verification information. The preset sheet renders only active-workflow controls, with compact visual selectors and no unsupported defaults.
5. Apply the reference visual system: dark-first warm charcoal surfaces, cyan/violet accents, editorial media prominence, compact rounded cards, strong contrast, and careful spacing. Keep all controls responsive, accessible, inset-aware, and reduced-motion aware.
6. Add the missing interaction polish: animated card placement, attachment transition, sheet presentation, model/estimate crossfade, loading-to-result transition, and completion haptic feedback. Preserve immediate non-spatial feedback when reduced motion is enabled.
7. Perform visual QA against the supplied concept at compact and expanded widths, including dark theme, large text, IME, and system-navigation insets.

Creative Brief UX and multi-chat history remain deferred until this redesign is visually and behaviorally complete.

### Deferred backlog

- Complete the Creative Brief UX and request-composition corrections described above.
- Complete persistent multi-chat history, navigation, naming, and draft-switch behavior described above.
- Populate and periodically re-verify static pricing metadata from the official model and pricing documentation.
- Add live model estimates only after authenticated estimate bodies are verified and billable-operation approval is granted.
- Add Compose/screenshot coverage for compact, expanded, dark-theme, large-font, and reduced-motion states.
- Perform the final controlled real-device/API verification only after the product-UI pass and explicit billable-operation approval.

## Source of truth for implementation status

Completed slices, verification evidence, current checkpoint, and the remaining real-device/API step are tracked in [`docs/progress.md`](progress.md). This implementation plan intentionally does not duplicate completed slice details.

## Reference documentation

- [Higgsfield API overview](https://docs.higgsfield.ai/docs)
- [Model catalog](https://docs.higgsfield.ai/docs/models)
- [Authentication](https://docs.higgsfield.ai/docs/authentication)
- [File uploads](https://docs.higgsfield.ai/docs/concepts/file-uploads)
- [Polling](https://docs.higgsfield.ai/docs/concepts/polling)
- [Billing and retention](https://docs.higgsfield.ai/docs/concepts/billing-and-retention)
