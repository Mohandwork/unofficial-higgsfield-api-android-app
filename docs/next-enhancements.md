# Next enhancements — two stages

Status: **Stage 1 planned; Stage 2 implemented in the working tree, pending device UI verification.** This note captures the follow-up discussion to the [codebase review](codebase-review-2026-09-26.md).

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

Implementation checkpoint: `ConversationLifecycleUseCase`, `SubmitGenerationUseCase`, and `ConversationComposerUseCase` now own the corresponding operations/rules. `ConversationUiState`, `ConversationUiEvent`, and `ConversationUiEffect` are separate feature contracts; the route consumes platform effects, while the header, history, composer, and timeline have smaller UI boundaries. Chat mutation failure paths clear the transition overlay. JVM tests pass and Compose instrumentation tests compile, but the latter have **not been run on a device**. This stage should not be considered device-QA complete until those interactions are exercised.

Goal: a new contributor can find a conversation operation, understand its inputs and outcomes, and change it without tracing one oversized ViewModel and a chain of positional callbacks.

1. Move cohesive business operations out of `ConversationViewModel` into focused use cases/coordinators. Candidate boundaries are conversation create/switch/remove, draft/source selection, and generation submit/retry/cancel. Keep repository and persistence contracts underneath; do not create wrappers that merely forward one method call.
2. Create feature-owned files such as `ConversationUiState.kt`, `ConversationUiEvent.kt`, and `ConversationUiEffect.kt` (or equivalent names matching the package). `UiState` holds durable renderable state, including loading/error/empty/success; `UiEvent` represents user intent; `UiEffect` represents one-time actions such as navigation, opening a picker, or showing a transient message. Do not put persistent loading state into one-time effects.
3. Let the route handle Android-specific effects and send typed events to the ViewModel. Replace long positional callback chains through `ConversationScreen`, `Workspace`, and timeline cards with smaller component contracts or a typed event dispatcher. Keep composables stateless where practical.
4. Make create/switch/remove transitions explicitly recover from persistence failure, so loading cannot remain stuck. Maintain the existing source/draft protection against stale Room emissions.
5. Refactor incrementally with characterization tests first. For each extracted operation, test event → state/effect, success and failure, stale emissions, process restoration, and the distinction between **Reuse parameters** (text/settings only) and **Edit image** (selected generated image as source). Add a few Compose interaction tests for the important user journeys.

Done when: the ViewModel primarily coordinates state and use cases; UI contracts live in their own files; no long callback list is passed across the main timeline layers; chat/generation behavior is unchanged; unit and UI tests cover the critical transitions. Do not make a new Gradle module a prerequisite for this stage.

## Scope boundary

These stages deliberately do **not** include public-release authentication, replacing intentional debug diagnostics, adding background polling while the app is closed, or implementing every other review recommendation. Those require a separate product decision. The user chose to start Stage 2 before Stage 1; the polling/data-integrity risk remains open and should be addressed separately.
