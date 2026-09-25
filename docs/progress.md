# Implementation Progress

This file is the running handoff record for Higgsfield Mobile. Update it at the end of every implementation slice, review cycle, or explicit stop.

## Current checkpoint

- Status: Slice 10.4 model-aware controls is complete. Verified workflow metadata controls settings visibility and draft options; static estimates remain explicitly unavailable until manually documented.
- Git: changes are intentionally uncommitted. Do not commit or push without current-conversation permission.
- API safety: no Higgsfield request, upload, estimate, or billable generation has been made.
- Toolchain: AGP 9.4.0, Gradle 9.6.0, AGP built-in Kotlin, KSP, and Android Studio JBR 25 verified locally.
- Build conventions: `gradle/libs.versions.toml` is the source of truth for project plugin and dependency versions.
- Code conventions: production domain/data literals are locally scoped constants; UI state carries resource-backed text; `core/error` owns persisted safe error mapping.
- 16 KB compatibility: `androidx.graphics:graphics-path` is explicitly resolved to `1.1.0` because Compose's transitive `1.0.1` was the native library reported by Android compatibility tooling.

## Delivery slices

| Slice | State | Evidence / next work |
|---|---|---|
| 1. Documentation and foundation | Complete | Plan and architecture documents saved; app shell, Hilt, Navigation 3, edge-to-edge, and theme added. Independently reviewed. |
| 2. Deterministic fake UI | Implemented | Home, Image/Video workspace, active editing source, local demo lineage, model picker, brief, and visible offline/cost states exist. Compose/screenshot accessibility coverage remains. |
| 3. Persistence and restoration | Complete | Room schema, Room/DataStore, persisted conversation/brief/workflow/timeline, active-source policy, and in-memory SQLite tests added. Independent review completed after the latest corrections; no blocking findings. |
| 4. Secure API lifecycle | Complete | Validated connectivity, credential-safe Retrofit service, authenticated upload-URL requests, explicitly unauthenticated presigned uploads, validated status/cancel URLs, status synchronization, backoff polling, and network-constrained WorkManager recovery added. Model-specific estimate and submission bodies remain intentionally deferred to the first verified adapter. No real request without explicit billable approval. |
| 5. SOUL vertical slice | Complete | A generic schema-driven workflow adapter and dynamic submission boundary are verified with the SOUL V2 Standard prompt schema, fixture coverage, and MockWebServer authorization/body/route coverage. SOUL Standard is registered with the same prompt schema and its own verified route. |
| 6. Verified family schemas | Complete | The single shared adapter now has real routes and request field configurations for every catalog entry: SOUL Cinema; Marketing Studio Alpha, Flare, and Sunburst; Qwen Image 3 and Edit; Seedance 2/2.5; all listed Kling workflows; Cinema Studio 4; and Wan 2.6, 2.7, 3, and 3 Prime. Text-to-video routes use their minimal documented prompt body; shared image/video URL values support Kling Motion; O3 and Omni use their documented image-reference routes. |
| 7. Local media-role assignment | Complete | The conversation UI derives attachment slots from the selected workflow, lets users assign picked images/videos to source, motion-reference, or image-reference roles, and removes slots that a newly selected model does not support. Picked device URIs remain local until the secure upload lifecycle supplies public URLs. |
| 8. Secure attachment upload bridge | Complete | Picked `content://` URIs and media roles flow into `GenerationDraft`. The upload coordinator validates MIME/kind compatibility, obtains a documented upload ticket, enforces HTTPS ticket/public URLs, streams through the unauthenticated presigned client, reuses already uploaded media, and returns centralized failures. No real network request was made during verification. |
| 9. Repository submission | Complete | `RoomGenerationRepository` persists a draft before network work, uploads local attachments, maps a verified schema only after public URLs exist, submits once, validates and persists accepted request metadata, then performs status-only reconciliation. The composer now invokes this path rather than the local demo. |
| 10. Slice 10 umbrella | Planned | Split into seven reviewable slices: repository integration hardening; real generation lifecycle UI; output handling; model-aware controls and static estimates; locked workspace redesign; accessibility/performance; and final real-device/API verification. |
| 10.1 Repository integration hardening | Complete | In-memory Room + MockWebServer coverage verifies one-shot accepted submission persistence, full draft/media reconstruction, and ambiguous disconnect persistence as `UNKNOWN_SUBMISSION_OUTCOME` without a generation retry. |
| 10.2 Generation lifecycle UI | Complete | Timeline cards project persisted records as queued, generating, completed, failed, moderated, canceled, and unknown-submission states. Only retryable failures offer retry; only queued requests offer cancel; accepted/restored active work receives status-only foreground polling. |
| 10.3 Output handling | Complete | Coil renders images; Media3 renders video/audio. System create-document downloads stream validated HTTPS output and persist the local URI only after success; remote-only media is explicitly temporary and failures preserve it. |
| 10.4 Model-aware controls | Complete | Workflow metadata declares verified adjustable options and static-estimate provenance. Only supported controls render, invalid options clear on model changes, and pricing/credits/latency show unavailable until manually documented. |
| 10.5 Locked workspace redesign | Not started | Implement the compact top bar, model/estimate strip, details sheet, composer hierarchy, output cards, action visibility, and functional motion. |
| 10.6 Accessibility and performance | Not started | Cover large text, reduced motion, contrast, semantics, adaptive widths, IME behavior, recomposition profiling, and device-focused verification. |
| 10.7 Real-device/API verification | Not started | Add secrets last, submit one controlled request, verify status lifecycle, media permissions, process death, and 16 KB compatibility. |

Slice 10 temporary test rule: skip new UI/Compose tests for this phase; fix only edge-case tests affected by each change and add a new non-UI test only when the changed behavior genuinely requires coverage. This scope does not alter the repository-wide testing rules outside Slice 10.

## Latest verification

- `testDebugUnitTest`: 38 tests passed after adding Slice 10.1 in-memory Room + MockWebServer repository integration coverage. Kotlin incremental compilation was disabled because the local Kotlin cache was locked.
- Tests cover streaming upload preparation, MIME/kind rejection, existing remote-media reuse, role-to-draft mapping, generic schemas, registry coverage, central errors, persistence, and conversation state.
- `lintDebug assembleDebug`: passed; lint has zero errors and 23 non-blocking version/plural/resource suggestions, and the debug APK assembled successfully.
- `zipalign -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk`: passed for all packaged native libraries, including `libandroidx.graphics.path.so`.
- Lint: zero errors; 23 dependency-version, plural, and resource suggestions remain.
- APK: debug assembly succeeded.
- Debug API networking now logs sanitized OkHttp method/host-path/query/ordinary headers/status/timing and JSON request/response bodies; authorization/secret headers, signed query values, private media URL fields, and presigned binary payloads remain excluded.

## Known follow-up

- Convert the fake workspace coverage into Compose/screenshot tests at compact, medium, and expanded widths.
- Start Slice 10.5: implement the locked workspace redesign, action hierarchy, and functional motion.
