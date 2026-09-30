# Higgsfield model controls reviewed on 2026-09-30

The app's request fields follow the selected route's schema. Account access still depends on the Higgsfield key. The table below records the original 26-route baseline before the direct-variant pass; the current route list is in [model-route-inventory.md](model-route-inventory.md).

| Workflow | Documented controls | App coverage before this pass |
| --- | --- | --- |
| [Seedance 2.0 text](https://docs.higgsfield.ai/docs/models/seedance-2/text-to-video) and [reference](https://docs.higgsfield.ai/docs/models/seedance-2/reference-to-video) | Duration, resolution, aspect ratio, audio generation; reference workflow accepts up to 9 images and 3 videos | Duration, resolution, aspect ratio, image and video references |
| [Seedance 2.5 text](https://docs.higgsfield.ai/docs/models/seedance-2-5/text-to-video) and [reference](https://docs.higgsfield.ai/docs/models/seedance-2-5/reference-to-video) | Duration, resolution, aspect ratio, bitrate, audio generation; text workflow also has output format; reference workflow accepts up to 30 images and 10 videos | Duration, resolution, aspect ratio, image and video references |
| [Happy Horse 1.0 text](https://docs.higgsfield.ai/docs/models/happy-horse-1/text-to-video) | Duration, resolution, aspect ratio, seed | All four |
| [Z-Image Turbo](https://docs.higgsfield.ai/docs/models/z-image-turbo/generate) | Resolution, aspect ratio, seed, prompt extension | Resolution, aspect ratio, seed |
| [SOUL standard](https://docs.higgsfield.ai/docs/models/soul-standard/generate) and [SOUL V2](https://docs.higgsfield.ai/docs/models/soul-2/generate) | Aspect ratio, resolution, style ID, seed, batch size, prompt enhancement, character reference; standard also accepts style strength and a reference image | Basic prompt only |
| [Marketing Studio 2.0 Alpha](https://docs.higgsfield.ai/docs/models/marketing-studio-image/generate-and-edit) | Quality, preset ID, reference images, moderation, resolution, aspect ratio, prompt enhancement | Resolution, aspect ratio, reference images. Enhanced requests need a preset ID from the authenticated presets endpoint and 1–2 images. |
| [LTX-2.5 Fast](https://docs.higgsfield.ai/docs/models/ltx-2-5/text-to-video-fast) | FPS, duration, resolution, aspect ratio, audio generation, camera movement | Not yet in the app |

The current options sheet presents explicit choices where a route declares them. Existing routes without declared choices still use free text; their request adapters validate some values before posting. Starting-price labels are in `app/src/main/java/com/higgsfield/mobile/core/model/ModelStartingRates.kt` and must be reviewed manually before relying on them.

## Remaining control gaps

- Qwen Image 3 text and edit expose resolution, aspect ratio, seed, and negative prompt, but the documented `prompt_extend`, `enable_thinking`, and `prompt_extend_mode` dependency still needs a coordinated UI and request validation update. [Text API](https://open.higgsfield.ai/models/alibaba/qwen-image-3/text-to-image/api-reference), [edit API](https://open.higgsfield.ai/models/alibaba/qwen-image-3/edit/api-reference).
- SOUL standard and V2 still omit trained Soul and style controls. Trained Soul creation has its own lifecycle, so a plain text field would not be sufficient. [SOUL standard](https://open.higgsfield.ai/models/higgsfield-ai/soul/standard/api-reference), [V2](https://open.higgsfield.ai/models/higgsfield-ai/soul/v2/standard/api-reference).
- Recraft V4.1 exposes resolution, aspect ratio, and output format. The documented palette and background-color objects need a color editor and exact object serialization. [Recraft API](https://open.higgsfield.ai/models/recraft/v4.1/text-to-image/api-reference).
- Kling O3 and Omni image-reference routes still omit element definitions and other advanced controls. [O3 API](https://open.higgsfield.ai/models/kling-video/o3/image-reference/api-reference), [Omni API](https://open.higgsfield.ai/models/kling-video/omni/image-reference/api-reference).
- Public API tables omit numeric maxima for several reference arrays. Those upload actions show an unspecified maximum; no count is invented. A real account-level request check is needed to confirm any unpublished cap.

Z-Image Turbo now exposes `prompt_extend`. Seedance 2.0 and 2.5 text/reference routes now expose audio generation, and 2.5 also exposes output format. New direct variants expose route-specific frame, source-video, image/video/audio-reference, and constrained controls where documented. This audit tracks advanced gaps separately from endpoint registration.

## Reference upload limits

The composer shows each selected workflow's media type and count beside its upload action. The same `MediaRequirement` values gate additional selections. The request schema also validates finite limits before the generation POST.

| Workflow | Photos | Videos | Source |
| --- | ---: | ---: | --- |
| Qwen Image 3 Edit | 1–3 required | — | [Edit schema](https://docs.higgsfield.ai/docs/models/qwen-image-3/edit) |
| Marketing Studio Image, unenhanced | Up to 16 | — | [2.0 Alpha](https://docs.higgsfield.ai/docs/models/marketing-studio-image/generate-and-edit), [Flare](https://docs.higgsfield.ai/docs/models/marketing-studio-image/flare), [Sunburst](https://docs.higgsfield.ai/docs/models/marketing-studio-image/sunburst) |
| Seedance 2.0 reference to video | Up to 9 | Up to 3 | [Reference schema](https://docs.higgsfield.ai/docs/models/seedance-2/reference-to-video) |
| Seedance 2.5 reference to video | Up to 30 | Up to 10 | [Reference schema](https://docs.higgsfield.ai/docs/models/seedance-2-5/reference-to-video) |
| Kling O3 and Omni image-reference routes | Numeric maximum not published | — | [O3](https://docs.higgsfield.ai/docs/models/kling-o3/image-reference), [Omni](https://docs.higgsfield.ai/docs/models/kling-omni/image-reference) |

Marketing Studio's enhanced preset route has a different conditional limit of 1–2 images. The app currently sends `enhance_prompt=false`, so its active limit is 16.
