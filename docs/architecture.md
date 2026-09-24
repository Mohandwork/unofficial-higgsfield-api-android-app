# Higgsfield architecture

This document captures the starting architectural direction for the app. It should evolve with the product rather than become ceremony.

## Current baseline

- Kotlin and Jetpack Compose for UI.
- Material 3 for the visual system.
- Feature-oriented packages with unidirectional state flow.
- ViewModels for screen-level state and lifecycle-aware work.
- Repositories as boundaries around persistence and network sources.
- Explicit constructor wiring until dependency complexity justifies a DI framework.

## Feature template

```text
app/src/main/java/com/higgsfield/mobile/
  core/                    # only genuinely shared primitives
  feature/<name>/          # screen, state, events, ViewModel, route
  MainActivity.kt          # app entry point and top-level setup
```

Keep the first implementation small. Split into `data`, `domain`, and `presentation` only when ownership, reuse, or testing benefits are concrete.

## Decision record format

For a decision with lasting impact, add a dated section containing context and constraints, the decision, alternatives considered, and consequences/follow-up work.

## Non-negotiables

User-visible state must be predictable, failures understandable, sensitive data must stay out of source control, and important behavior must be covered by tests. See the root `AGENTS.md` for the full working agreement.
