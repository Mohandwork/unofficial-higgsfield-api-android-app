# Next enhancements — two stages

Status: **Stage 2 refactor complete; follow-up items 2–4 implemented in the working tree.** The owner reports Stage 1 (follow-up item 5) complete in separate work; this change does not review that implementation. Follow-up item 1 remains planned. Manual UI verification remains the chosen validation method.

## Decisions from the discussion

- **User:** Of the production concerns discussed, concurrent request polling/status writes (item 1) might be the only actual risk to prioritize. Embedding credentials in this private build (item 2) and verbose debug logging (item 3) are intentional choices for now.
- **Assistant:** Preserve those scope decisions. Do not bundle credential redesign or logging changes into the two stages below. Revisit them if the app will be distributed beyond the owner's private environment or the privacy requirements change.
- **User:** The conversation ViewModel carries too much business logic; move meaningful operations into a use case or a small set of use cases. The UI is also too hard to follow; define separate `UiState`, `UiEvent`, and `UiEffect` contracts to handle operations clearly.
- **Assistant:** Keep the existing single Gradle `:app` module unless team ownership or build performance justifies a split. Extract along behavioral boundaries, not one class per button. Preserve the current product behavior while refactoring.
- Other review findings are **not decided here**. Silence on them does not mean accepted, rejected, or completed.

## Stage 1 — make generation reconciliation safe

Goal: one accepted request must progress consistently to one terminal local result, even when the app-wide poller and a visible conversation are active at the same time.

1. Identify one owner for each request's foreground polling, or add a single-flight gate keyed by generation/request ID. Keep the app-wide recovery behavior, including requests in other chats.
2. Make Room status transitions conditional and monotonic: a stale `queued` or `in_progress` response must not overwrite `completed`, `failed`, `canceled`, or another terminal state. Perform status and output changes atomically.
3. Make repeated `completed` responses idempotent. Merge output records without replacing an already saved `localUri` or the selected active source with older data.
4. Add deterministic tests for simultaneous pollers, out-of-order responses, duplicate completion, cancellation-versus-completion, and a downloaded output surviving another completion response.

Done when: those tests pass; a completion cannot regress to a pending card; duplicate polling cannot erase the saved local media pointer; normal reopen/recovery behavior remains intact. Avoid changing submission semantics or automatically repeating a billable POST.

## Stage 2 — separate business operations and explicit UI contracts

Implementation checkpoint: `ConversationLifecycleUseCase`, `SubmitGenerationUseCase`, `ConversationComposerUseCase`, and `SourceSelectionUseCase` own the corresponding operations/rules. `ConversationUiState`, `ConversationUiEvent`, and `ConversationUiEffect` are separate feature contracts; the route consumes platform effects, while the header, history, composer, and timeline use typed event dispatch. Chat, composer, and generation state are grouped separately; transient sheet/menu visibility is view-local. Draft persistence is per-conversation, debounced, and explicitly flushed for chat switches and durable submission/reuse transitions. JVM tests pass and Android test sources compile; manual UI verification is intentionally preferred over adding further Compose coverage.

Goal: a new contributor can find a conversation operation, understand its inputs and outcomes, and change it without tracing one oversized ViewModel and a chain of positional callbacks.

1. Move cohesive business operations out of `ConversationViewModel` into focused use cases/coordinators. Candidate boundaries are conversation create/switch/remove, draft/source selection, and generation submit/retry/cancel. Keep repository and persistence contracts underneath; do not create wrappers that merely forward one method call.
2. Create feature-owned files such as `ConversationUiState.kt`, `ConversationUiEvent.kt`, and `ConversationUiEffect.kt` (or equivalent names matching the package). `UiState` holds durable renderable state, including loading/error/empty/success; `UiEvent` represents user intent; `UiEffect` represents one-time actions such as navigation, opening a picker, or showing a transient message. Do not put persistent loading state into one-time effects.
3. Let the route handle Android-specific effects and send typed events to the ViewModel. Replace long positional callback chains through `ConversationScreen`, `Workspace`, and timeline cards with smaller component contracts or a typed event dispatcher. Keep composables stateless where practical.
4. Make create/switch/remove transitions explicitly recover from persistence failure, so loading cannot remain stuck. Maintain the existing source/draft protection against stale Room emissions.
5. Refactor incrementally with characterization tests first. For each extracted operation, test event → state/effect, success and failure, stale emissions, process restoration, and the distinction between **Reuse parameters** (text/settings only) and **Edit image** (selected generated image as source). Add a few Compose interaction tests for the important user journeys.

Done when: the ViewModel primarily coordinates state and use cases; UI contracts live in their own files; no long callback list is passed across the main timeline layers; chat/generation behavior is unchanged; unit and UI tests cover the critical transitions. Do not make a new Gradle module a prerequisite for this stage.

## Follow-up architecture findings — current status

The next review found these state-consistency risks. They are recorded for later selection, not added to the completed Stage 2 scope. Address them in the order below, with deterministic JVM tests for the affected transitions; additional Compose tests are not required by the current manual-UI-validation preference.

1. **Planned — serialize draft persistence.** `ComposerDraftWriter`'s debounced loop and `flush()` can persist concurrently. An older draft may finish after a newer flush and overwrite it; a chat switch also launches the previous chat's flush without awaiting it. Serialize writes per conversation, make a switch wait for its relevant flush, and test overlapping debounce/flush and rapid chat switching. Relevant code: `ConversationWorkspaceCoordinator.kt` (`ComposerDraftWriter`) and `ConversationViewModel.kt` (`initialize`, `persistDraft`).
2. **Implemented in this working tree — make source/parameter transitions atomic and recoverable.** Parameter reuse now writes workflow, brief, draft, and cleared active source in one Room transaction. Workflow and output selection update UI only after persistence succeeds; late source/model snapshots do not replace a newly saved selection. Failure and rollback paths have JVM tests. This does not serialize unrelated debounced draft writes from item 1.
3. **Implemented in this working tree — keep submission completion scoped to its chat.** A result for a request started in another conversation still persists and polls, but cannot change the current chat's prompt, submitting flag, or message. A chat-switch test covers this transition.
4. **Implemented in this working tree — protect locally edited briefs from late snapshots.** Brief edits are tracked separately from prompt edits so a late Room emission cannot replace local brief text; JVM tests cover a late snapshot and a failed brief write.
5. **Owner reports complete — Stage 1 generation reconciliation.** Implemented separately by the owner and not reviewed or retested as part of this change. Stage 1 above remains the acceptance criteria for that work.

After the correctness work, optional readability cleanup: move the ViewModel's snapshot/record projection into a pure mapper, and have generation action events carry stable IDs instead of whole `TimelineItem` snapshots, resolving current data when handling an action. Keep the existing single `:app` module and grouped event types; do not create one file per event or one-line forwarding use cases solely for pattern conformity.

## Future model picker enhancement

Replace the long list of separate route entries with a model-first picker. Show each main model or version once (for example, Wan 3.0 Prime), then offer a clearly labeled action control containing only that model's supported operations, such as Text to Image, Image to Video, Reference to Video, Edit, or Extend. Selecting an action resolves to its existing exact API route and updates the composer slots, settings, and limits from that route's schema. Preserve stable route IDs in saved drafts and generation history, and restore the selected model and action when reopening a conversation. Guided marketing workflows remain a separate future UI flow rather than an action that silently changes the direct route.

## Scope boundary

These stages deliberately do **not** include public-release authentication, replacing intentional debug diagnostics, adding background polling while the app is closed, or implementing every other review recommendation. Those require a separate product decision. Item 1 is deferred; items 2–4 are implemented here; the owner reports item 5 complete separately.
