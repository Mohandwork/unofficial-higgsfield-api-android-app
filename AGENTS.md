# Higgsfield engineering instructions

## Git safety — read before doing any task

- Do not create commits, amend commits, force-push, or push to any remote without the user's explicit permission in the current conversation.
- Editing files and preparing a diff is allowed; committing or pushing is a separate action that always requires confirmation.
- Do not infer permission from a request to "finish", "ship", "publish", or "make it ready". Ask before committing or pushing.

These instructions govern work in this repository. They are intentionally opinionated about clarity and verification, while leaving room to choose the smallest design that fits the feature.

## Questions versus actions

- When the user presents a question, answer the question directly before doing anything else.
- Do not run commands, edit files, launch apps, browse external services, or otherwise take action merely because an action could help answer a question.
- Take action only when the user explicitly requests that action or clearly approves a proposed action. Read-only inspection that is necessary to answer a specifically requested review or status question is allowed.

## Product and technical baseline

- Build a focused, reliable Android experience before adding breadth.
- Use Kotlin, Jetpack Compose, Material 3, and AndroidX.
- Keep the existing package `com.higgsfield.mobile`, minimum SDK 24, and compile/target SDK 35 unless a deliberate migration is documented.
- Prefer platform and AndroidX capabilities over adding dependencies. Every new dependency needs a concrete reason and a maintenance/security check.

## Default architecture

- Organize code by user-facing feature, not by a large global collection of `utils`, `managers`, or `helpers`.
- Use a unidirectional flow: UI emits an intent/event, a state holder processes it, and the UI renders immutable state.
- Put screen state and event handling in a ViewModel when the screen has meaningful state, business behavior, or lifecycle-sensitive work. Keep composables stateless where practical.
- Treat repositories as boundaries around data sources. The UI must not know whether data comes from memory, disk, or a network.
- Add a domain/use-case layer only when logic is shared, non-trivial, or worth isolating; do not create one-file wrappers just to satisfy a pattern.
- Prefer constructor injection and explicit dependencies. Introduce a DI framework only when manual wiring becomes a real source of friction.
- Model loading, success, empty, and failure states explicitly. Do not hide important state in nullable fields or global mutable objects.

## Implementation habits

- Start with the smallest complete vertical slice: user action, state transition, persistence/network boundary if needed, and visible feedback.
- Before coding, write down the screen states, user events, data ownership, and failure behavior for non-trivial work.
- Make one coherent change at a time. Keep diffs easy to review and avoid speculative abstractions.
- Prefer readable names and straightforward control flow over clever compression.
- Preserve existing user work. Do not reset, overwrite, or broadly reformat unrelated files.
- Record meaningful architectural decisions in `docs/architecture.md` or a short ADR when the choice will affect future features.

## Quality bar

- Every feature should have appropriate unit tests for state transitions and business rules.
- Add Compose/UI tests for important user journeys and accessibility semantics.
- Handle configuration changes and process recreation intentionally; do not rely on accidental in-memory persistence.
- Expose useful loading and error feedback, including retry behavior where retry makes sense.
- Use stable keys for lists, avoid unnecessary recomposition, and keep work off the main thread.
- Provide content descriptions or meaningful semantics for interactive and non-text visuals. Check dark theme, large text, and touch target behavior for user-facing UI.
- Never commit secrets, tokens, private URLs, or generated local configuration.

## Definition of done

Before calling work complete:

1. The smallest relevant Gradle build and tests have been run, or the limitation is stated clearly.
2. The changed behavior has been manually or automatically verified.
3. Empty, loading, error, retry, and success paths are considered where applicable.
4. The diff contains no unrelated churn, temporary logging, or placeholders that could mislead users.
5. Documentation is updated when setup, behavior, or architecture changed.
6. Git status is reviewed and the commit is focused.

When requirements conflict, favor user-visible correctness, data safety, and clear failure behavior; explain the tradeoff in the implementation notes.
