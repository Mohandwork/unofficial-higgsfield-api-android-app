---
name: karpathy-inspired-engineering
description: Apply a simple, explicit, iterative engineering style inspired by Karpathy's public coding habits: understand the state, build the smallest useful slice, keep feedback loops short, and verify behavior continuously. Use when planning, implementing, or reviewing Higgsfield features.
---

# Karpathy-inspired engineering

This is a local working style, not an official Karpathy specification. Use it to improve decisions, not to force a particular Android architecture.

## Operating principles

- Understand the current code and desired user behavior before editing.
- Keep the source of truth obvious; do not duplicate state without a reason.
- Prefer small, explicit functions and data structures over layers of indirection.
- Build the smallest complete behavior that a user can exercise.
- Keep the feedback loop short: compile, run focused tests, inspect the diff, then continue.
- Treat logs, failures, and test output as evidence. Do not claim success without checking.
- Make changes local and reversible. Avoid broad refactors while implementing a feature.
- Remove dead code, temporary diagnostics, and speculative scaffolding before finishing.

## Practical loop

1. State the user-visible outcome in one sentence.
2. Inspect the relevant files and trace the current data flow.
3. Write the smallest state model that covers the behavior.
4. Implement one vertical slice.
5. Run the narrowest useful build/test and fix the first real failure.
6. Exercise the happy path and the most likely failure path.
7. Review the diff for unnecessary complexity and update documentation if the design changed.

## Guardrails

- Mark invented backend contracts, product requirements, and error semantics as assumptions.
- Do not hide unfinished work behind fake success states or misleading placeholders.
- Do not add abstractions merely because a popular architecture diagram contains them.
- Do not trade away accessibility, data safety, or clear errors for implementation speed.
