# Refined implementation plan

This document describes the finished product contract and its acceptance checks. It replaces the earlier slice-based backlog; the core product work is implemented.

## 1. Product contract

The app has two independent workspaces: Image and Video. Each workspace owns its own conversations, selected workflow, Creative Brief, prompt draft, references, and generation timeline. A user can start a new chat, switch chats, rename a chat, or remove a chat without leaking state between sections.

The default path is:

1. Choose a workspace and model.
2. Optionally edit the Creative Brief and attach a reference.
3. Enter a prompt and submit once.
4. Observe queued/running progress, then a result or the provider's actual error detail.
5. Retry a failed item deliberately or continue the conversation.

## 2. Persistence and navigation

- Room is the source of truth for conversations, messages, generation records, selected workflow, brief, and active source output.
- The history rail is used on wider layouts; the modal drawer is used on compact layouts.
- History is grouped by Image and Video and shows only populated sections.
- New chat creates a persisted placeholder conversation. The first prompt can derive a useful title; rename remains available.
- Chat removal requires confirmation, deletes the local conversation and its related records, then opens the most recent remaining chat or creates a fresh one.
- Navigation and destructive updates expose a short loading overlay so the UI never appears to ignore an action.

## 3. Prompt and reference behavior

- Creative Brief fields are reusable per conversation: intent, subject, style, composition, lighting, palette, exclusions, and output guidance.
- The request sent to the provider is the exact composed prompt persisted locally.
- Workflow capabilities decide whether negative prompts are sent as a dedicated field or folded into the composed prompt.
- The reference CTA performs the action directly when there is one available slot; a menu is shown only when multiple attachment choices are valid.
- References remain visible in the timeline independently of whether a generation succeeds.

## 4. API, errors, and lifecycle

- Only an explicit submit crosses the network boundary.
- Request/response mapping is schema-aware and preserves provider detail.
- Error rendering prefers the server's `detail`/message text for any status, including 401/403; it does not infer a generic message from the status code when a useful server message exists.
- Generation records distinguish queued, in-progress, succeeded, failed, and cancelled states. Failed records remain retryable and do not become active output.
- Ambiguous POST outcomes are not automatically retried.

## 5. Catalog and visual system

- Model names, capabilities, prices, durations, resolutions, and discounts use the supplied catalog screenshots and official documentation references where available.
- Catalog values are static display metadata. No live estimate is shown until an authenticated estimate schema and an approved billable integration exist.
- Light and dark palettes follow the refined neon-lime/cyan system. Splash background and brand artwork switch with device theme.
- The bottom composer has no persistent price chip.

## 6. Acceptance checklist

- [x] Image/video workspace separation and workflow selection.
- [x] Persistent per-section conversations with rail/drawer history.
- [x] New chat, rename, removal confirmation, and post-removal recovery.
- [x] Creative Brief persistence and capability-aware prompt composition.
- [x] Direct reference CTA and reference visibility across failure states.
- [x] Provider error detail, including credit/authentication messages.
- [x] Generation progress, loading overlays, retry, and stale-response protection.
- [x] Static catalog metadata and model details dialog.
- [x] Refined light/dark theme and mode-aware splash assets.
- [x] Offline-safe local persistence and credential hygiene.

## 7. Deliberately optional verification work

These items do not block the finished product contract:

- Compose screenshot tests for every compact and expanded breakpoint.
- A final manual visual pass on representative physical devices.
- Live pricing/credit estimates after the provider publishes a stable authenticated schema and billable use is explicitly approved.

## 8. Future enhancement

Image editing still needs a dedicated implementation pass. The selected generated output must be uploaded or mapped into the workflow's image/reference field on the next submission; prompt text alone is not sufficient for an edit request.
