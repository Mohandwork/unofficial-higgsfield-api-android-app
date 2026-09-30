# Next enhancements — two stages

Status: **Stage 1 planned; Stage 2 refactor complete in the working tree; follow-up architecture findings recorded below.** Manual UI verification remains the chosen validation method. This note captures the follow-up discussion from the codebase review.

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

## Follow-up architecture findings — not yet implemented

The next review found these state-consistency risks. They are recorded for later selection, not added to the completed Stage 2 scope. Address them in the order below, with deterministic JVM tests for the affected transitions; additional Compose tests are not required by the current manual-UI-validation preference.

1. **Serialize draft persistence.** `ComposerDraftWriter`'s debounced loop and `flush()` can persist concurrently. An older draft may finish after a newer flush and overwrite it; a chat switch also launches the previous chat's flush without awaiting it. Serialize writes per conversation, make a switch wait for its relevant flush, and test overlapping debounce/flush and rapid chat switching. Relevant code: `ConversationWorkspaceCoordinator.kt` (`ComposerDraftWriter`) and `ConversationViewModel.kt` (`initialize`, `persistDraft`).
2. **Make source/parameter transitions atomic and recoverable.** `SourceSelectionUseCase.reuseParameters()` saves workflow, brief, and active source separately, so a failure can leave a partial database transition after the UI has shown success. Expose one transactional persistence operation for the combined change, and commit or restore optimistic UI state according to its result. Apply explicit failure handling to workflow and output selection as well. Relevant code: `SourceSelectionUseCase.kt`, `ConversationPersistence.kt`, and `ConversationViewModel.kt`.
3. **Keep submission completion scoped to its chat.** A request submitted in one conversation can finish after navigation; `submitDraft()` currently updates the then-current screen's submitting state and message. Guard completion-side UI updates by the captured conversation ID while still persisting and polling the accepted request. Test switching chats while submission is in flight. Relevant code: `ConversationViewModel.kt` (`submitDraft`).
4. **Protect locally edited briefs from late snapshots.** `restoreSnapshot()` protects the prompt draft with `draftTouched` but always assigns `snapshot.brief`; `updateBrief()` does not mark the brief as locally changed. Track brief edits separately or use a snapshot revision so a delayed Room emission cannot replace new text. Test editing the brief before the initial snapshot arrives. Relevant code: `ConversationViewModel.kt` (`updateBrief`, `restoreSnapshot`).
5. **Complete Stage 1 generation reconciliation.** Visible-chat polling and app-wide recovery can act on the same request; status writes must be monotonic and repeated completion must retain local media pointers. The concrete work and completion criteria remain in Stage 1 above. Relevant code: `ConversationWorkspaceCoordinator.kt`, `LocalGenerationStore.kt`, and the app-wide recovery worker.

After the correctness work, optional readability cleanup: move the ViewModel's snapshot/record projection into a pure mapper, and have generation action events carry stable IDs instead of whole `TimelineItem` snapshots, resolving current data when handling an action. Keep the existing single `:app` module and grouped event types; do not create one file per event or one-line forwarding use cases solely for pattern conformity.

## Future model picker enhancement

Replace the long list of separate route entries with a model-first picker. Show each main model or version once (for example, Wan 3.0 Prime), then offer a clearly labeled action control containing only that model's supported operations, such as Text to Image, Image to Video, Reference to Video, Edit, or Extend. Selecting an action resolves to its existing exact API route and updates the composer slots, settings, and limits from that route's schema. Preserve stable route IDs in saved drafts and generation history, and restore the selected model and action when reopening a conversation. Guided marketing workflows remain a separate future UI flow rather than an action that silently changes the direct route.

## Scope boundary

These stages deliberately do **not** include public-release authentication, replacing intentional debug diagnostics, adding background polling while the app is closed, or implementing every other review recommendation. Those require a separate product decision. The user chose to start Stage 2 before Stage 1; the polling/data-integrity risk remains open and should be addressed separately. The follow-up findings are a backlog, not a claim that those fixes were implemented.
