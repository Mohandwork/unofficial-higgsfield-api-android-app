# Higgsfield route inventory — 2026-09-30

Scope: every family already registered in the app, plus Genjutsu, MiniMax H3, PixVerse V6, Grok Imagine 2.0, Ideogram 4.0, Recraft V4.1, and the three requested marketing workflows. This is an endpoint inventory, not a promise that every advanced control is implemented or that account access is available. Routes are copied from the public Higgsfield catalog and linked references; undocumented media limits still need verification.

`Current` means the app has an entry for that **exact endpoint**. `Missing` means it does not. The existing entries often send only a subset of the route's options; `Current` does not imply full control coverage. An image or video reference is a separate route when the API gives it a separate model ID. A workflow page that invokes an existing endpoint is a guided experience, not an additional endpoint.

This pass records **72 endpoint candidates: 71 current and 1 missing**, plus three marketing guides that reuse an existing endpoint. The remaining route is Wan 2.7 Reference to Video: its public API table was inaccessible during implementation, so its exact request fields and media limits remain unverified. The count follows the published family listings for this scope; it is not the size of Higgsfield's entire catalog. `Current` means the route and its core request shape are registered; additional advanced fields are tracked separately in [model-controls-audit.md](model-controls-audit.md).

## Image routes

| Family | Variant and endpoint | App | Media and schema notes |
| --- | --- | --- | --- |
| SOUL | [Standard](https://open.higgsfield.ai/models/higgsfield-ai/soul/standard/api-reference) `higgsfield-ai/soul/standard` | Current | Text; style, seed, batch, resolution, aspect ratio, prompt enhancement; optional image reference. |
| SOUL | [V2 Text-to-Image](https://open.higgsfield.ai/models/higgsfield-ai/soul/v2/standard/api-reference) `higgsfield-ai/soul/v2/standard` | Current | Text; optional trained Soul ID, style, seed, batch, resolution, aspect ratio, prompt enhancement. Soul ID creation is a separate lifecycle. |
| SOUL | [V2 Image-to-Image](https://open.higgsfield.ai/models/higgsfield-ai/soul/v2/image-to-image/api-reference) `higgsfield-ai/soul/v2/image-to-image` | Current | One required scene image; optional trained Soul ID and other V2 controls. |
| SOUL | `higgsfield-ai/soul/cinema` | Current | Existing route; public API reference not found during this pass. Reverify endpoint and schema before expanding controls. |
| Marketing Studio | [2.0 Alpha](https://open.higgsfield.ai/models/marketing-studio/image/api-reference) `marketing-studio/image` | Current | Direct text generation or edit with up to 16 images; enhanced preset mode requires a product image, allows a second model image, and uses a live preset UUID. |
| Marketing Studio | [2.5 Flare](https://open.higgsfield.ai/models/marketing-studio/image/flare/api-reference) `marketing-studio/image/flare` | Current | Direct edit up to 16 images; enhanced preset mode requires one product image and allows one model image. Quality, resolution, aspect ratio, moderation, prompt enhancement. |
| Marketing Studio | [2.5 Sunburst](https://open.higgsfield.ai/models/marketing-studio/image/sunburst/api-reference) `marketing-studio/image/sunburst` | Current | Direct mode up to 16 images; enhanced mode requires a product image and allows one model image. |
| Qwen Image 3 | [Text-to-Image](https://open.higgsfield.ai/models/alibaba/qwen-image-3/text-to-image/api-reference) `alibaba/qwen-image-3/text-to-image` | Current | Text; seed, resolution, aspect ratio, prompt extension and thinking controls. |
| Qwen Image 3 | [Edit](https://open.higgsfield.ai/models/alibaba/qwen-image-3/edit/api-reference) `alibaba/qwen-image-3/edit` | Current | One to three ordered required images; same broader option family. |
| Z-Image | [Turbo](https://open.higgsfield.ai/models/z-image/turbo/api-reference) `z-image/turbo` | Current | Text; seed, 1k/2k, aspect ratio, `prompt_extend` (currently missing in UI). |
| Grok Imagine | [Image 2.0](https://open.higgsfield.ai/models/xai/grok-imagine-image-2.0/api-reference) `xai/grok-imagine-image-2.0` | Current | Text; optional `image_urls`, quality, resolution, aspect ratio. Public reference does not state a numeric image limit. |
| Ideogram | [4.0](https://open.higgsfield.ai/models/ideogram/v4.0/api-reference) `ideogram/v4.0` | Current | Text; optional single `image_url`, image weight, rendering speed, aspect ratio. |
| Recraft | [V4.1 Text-to-Image](https://open.higgsfield.ai/models/recraft/v4.1/text-to-image/api-reference) `recraft/v4.1/text-to-image` | Current | Text; palette/background colors, 1k, aspect ratio, output format. |
| Recraft | [V4.1 Pro](https://open.higgsfield.ai/models/recraft/v4.1/pro/text-to-image/api-reference) `recraft/v4.1/pro/text-to-image` | Current | Text; 2k tier and route-specific options. |
| Recraft | [V4.1 Utility](https://open.higgsfield.ai/models/recraft/v4.1/utility/text-to-image/api-reference) `recraft/v4.1/utility/text-to-image` | Current | Text; utility color controls and output format. |
| Recraft | [V4.1 Utility Pro](https://open.higgsfield.ai/models/recraft/v4.1/utility/pro/text-to-image/api-reference) `recraft/v4.1/utility/pro/text-to-image` | Current | Text; 2k utility tier. |

### Guided image workflows

| User choice | Actual submission | Requirements |
| --- | --- | --- |
| [Graphic Ads](https://open.higgsfield.ai/models/workflows/graphic-ads/api-reference) | `marketing-studio/image` | Choose a live visible Ads preset, product image required, optional second model image, `enhance_prompt=true`. |
| [Marketplace Design](https://open.higgsfield.ai/models/workflows/marketplace-design/api-reference) | `marketing-studio/image` | Same endpoint and preset flow; different guidance and preset selection. |
| [Product Shots](https://open.higgsfield.ai/models/workflows/product-shots/api-reference) | `marketing-studio/image` | Same endpoint and preset flow; different guidance and preset selection. |

Preset UUIDs must come from `GET /marketing-studio/image/presets`, including cursor pagination. The examples in the workflow pages are not stable IDs to embed in the app. Enhanced mode's 1–2 image rule differs from direct mode's up-to-16 rule. [Source](https://open.higgsfield.ai/models/workflows/graphic-ads/api-reference).

## Video routes

| Family | Variant and endpoint | App | Media and schema notes |
| --- | --- | --- | --- |
| Seedance 2.0 | [Text-to-Video](https://open.higgsfield.ai/models/bytedance/seedance-2.0/text-to-video/api-reference) `bytedance/seedance-2.0/text-to-video` | Current | Text; duration, resolution, aspect ratio, audio generation. |
| Seedance 2.0 | [Image-to-Video](https://open.higgsfield.ai/models/bytedance/seedance-2.0/image-to-video/playground) `bytedance/seedance-2.0/image-to-video` | Current | First frame required, optional end frame; route-specific options. |
| Seedance 2.0 | [Reference-to-Video](https://open.higgsfield.ai/models/bytedance/seedance-2.0/reference-to-video/api-reference) `bytedance/seedance-2.0/reference-to-video` | Current | Up to 9 images and 3 videos per playground; optional audio. Cross-media limit needs checking. |
| Seedance 2.5 | [Text-to-Video](https://open.higgsfield.ai/models/bytedance/seedance-2.5/text-to-video/api-reference) `bytedance/seedance-2.5/text-to-video` | Current | Text; duration, resolution, aspect ratio, bitrate, format, audio generation. |
| Seedance 2.5 | [Image-to-Video](https://open.higgsfield.ai/models/bytedance/seedance-2.5/image-to-video/api-reference) `bytedance/seedance-2.5/image-to-video` | Current | First frame required, optional end frame; bitrate/audio controls. |
| Seedance 2.5 | [Reference-to-Video](https://open.higgsfield.ai/models/bytedance/seedance-2.5/reference-to-video/api-reference) `bytedance/seedance-2.5/reference-to-video` | Current | Up to 30 images and 10 videos per playground; optional audio. Cross-media limit needs checking. |
| Seedance 2.5 | [Video Edit](https://open.higgsfield.ai/models/bytedance/seedance-2.5/video-edit/playground) `bytedance/seedance-2.5/video-edit` | Current | Source video required; optional image/audio/video references. |
| Seedance 2.5 | [Video Extend](https://open.higgsfield.ai/models/bytedance/seedance-2.5/video-extend/playground) `bytedance/seedance-2.5/video-extend` | Current | Source video required; optional image/audio references. |
| Genjutsu | [Motion Transfer](https://open.higgsfield.ai/models/higgsfield/genjutsu/motion-transfer/v1.0/api-reference) `higgsfield/genjutsu/motion-transfer/v1.0` | Current | One source video, 1–8 reference images, optional prompt, resolution; source video minimum 4 s, trimmed above 30 s. |
| Genjutsu | Object Swap `higgsfield/genjutsu/object-swap/v1.0` | Current | One source video and up to 8 images in playground. The user supplied the direct-model example with this ID; the [workflow example](https://open.higgsfield.ai/models/workflows/genjutsu/object-swap/api-reference) spells the provider segment `higgsfiled`. |
| MiniMax H3 | [Text-to-Video](https://open.higgsfield.ai/models/minimax/h3/text-to-video/api-reference) `minimax/h3/text-to-video` | Current | Text; 5–15 s, 2K, aspect ratio, optional AIGC watermark. |
| MiniMax H3 | [Image-to-Video](https://open.higgsfield.ai/models/minimax/h3/image-to-video/api-reference) `minimax/h3/image-to-video` | Current | Required first frame, optional last frame; same duration/resolution family. |
| MiniMax H3 | [Reference-to-Video](https://open.higgsfield.ai/models/minimax/h3/reference-to-video/api-reference) `minimax/h3/reference-to-video` | Current | Optional image, video, and audio URL arrays; numeric limits not published in API table. |
| PixVerse V6 | [Text-to-Video](https://open.higgsfield.ai/models/pixverse/v6/text-to-video/api-reference) `pixverse/v6/text-to-video` | Current | Text; 1–15 s, resolution, aspect ratio, audio, seed, negative prompt. |
| PixVerse V6 | [Image-to-Video](https://open.higgsfield.ai/models/pixverse/v6/image-to-video/api-reference) `pixverse/v6/image-to-video` | Current | Required first frame, optional last frame; audio, seed, negative prompt. |
| Wan 2.6 | [Text-to-Video](https://open.higgsfield.ai/models/wan/v2.6/text-to-video/api-reference) `wan/v2.6/text-to-video` | Current | Text; duration/resolution and other controls. |
| Wan 2.6 | [Image-to-Video](https://open.higgsfield.ai/models/wan/v2.6/image-to-video/playground) `wan/v2.6/image-to-video` | Current | First frame required; verify optional end frame and route controls. |
| Wan 2.6 | [Reference-to-Video](https://open.higgsfield.ai/models/wan/v2.6/reference-to-video/api-reference) `wan/v2.6/reference-to-video` | Current | `video_urls` required; published table does not state numeric limit. |
| Wan 2.7 | [Text-to-Video](https://open.higgsfield.ai/models/wan/v2.7/text-to-video/api-reference) `wan/v2.7/text-to-video` | Current | Text; optional audio, seed, prompt extension, negative prompt. |
| Wan 2.7 | [Image-to-Video](https://open.higgsfield.ai/models/wan/v2.7/image-to-video/api-reference) `wan/v2.7/image-to-video` | Current | Required first frame, optional end frame and audio. |
| Wan 2.7 | [Reference-to-Video](https://open.higgsfield.ai/models/wan/v2.7/reference-to-video/playground) `wan/v2.7/reference-to-video` | Missing | Reference media route; exact types/counts need API table verification. |
| Wan 3.0 | [Text-to-Video](https://open.higgsfield.ai/models/alibaba/wan-3.0/text-to-video/api-reference) `alibaba/wan-3.0/text-to-video` | Current | Text; 2–30 s, 480p/720p/1080p, aspect ratio, audio, thinking, seed. |
| Wan 3.0 | [Image-to-Video](https://open.higgsfield.ai/models/alibaba/wan-3.0/image-to-video/api-reference) `alibaba/wan-3.0/image-to-video` | Current | Required first frame, optional last frame; same settings family. |
| Wan 3.0 | [Reference-to-Video](https://open.higgsfield.ai/models/alibaba/wan-3.0/reference-to-video/api-reference) `alibaba/wan-3.0/reference-to-video` | Current | Image/video references; shared total advertised in playground; verify exact schema count. |
| Wan 3.0 Prime | [Text-to-Video](https://open.higgsfield.ai/models/alibaba/wan-3.0-prime/text-to-video/api-reference) `alibaba/wan-3.0-prime/text-to-video` | Current | Text; 2–30 s, resolution/aspect ratio, audio, thinking, seed. |
| Wan 3.0 Prime | [Image-to-Video](https://open.higgsfield.ai/models/alibaba/wan-3.0-prime/image-to-video/api-reference) `alibaba/wan-3.0-prime/image-to-video` | Current | Required first frame, optional last frame; same settings family. |
| Wan 3.0 Prime | [Reference-to-Video](https://open.higgsfield.ai/models/alibaba/wan-3.0-prime/reference-to-video/playground) `alibaba/wan-3.0-prime/reference-to-video` | Current | Playground advertises up to 10 images **or videos combined**; API table must confirm URL fields and other limits. |
| Happy Horse 1.0 | [Text-to-Video](https://open.higgsfield.ai/models/alibaba/happy-horse/text-to-video/api-reference) `alibaba/happy-horse/text-to-video` | Current | Text; duration/resolution/aspect ratio/seed. |
| Happy Horse 1.0 | [Image-to-Video](https://open.higgsfield.ai/models/alibaba/happy-horse/image-to-video/api-reference) `alibaba/happy-horse/image-to-video` | Current | One required image; duration and resolution. |
| Happy Horse 1.0 | [Reference-to-Video](https://open.higgsfield.ai/models/alibaba/happy-horse/reference-to-video/api-reference) `alibaba/happy-horse/reference-to-video` | Current | Required `image_urls`; public table does not give numeric count. |
| Cinema Studio 4.0 | [Generate](https://open.higgsfield.ai/models/higgsfield/cinema-studio/4.0/api-reference) `higgsfield/cinema-studio/4.0` | Current | One route advertises text, image, and video references; API page omits a parameter table. Verify accepted fields before enabling media slots. |

### Kling routes

The [Kling family catalog](https://open.higgsfield.ai/models/kling-video/v3.0/std/text-to-video/playground) exposes distinct tier and task endpoints. These are not all represented by the existing generic Kling 2.5/2.6/3 entries. For request implementation, inspect each linked route's table; Kling uses route-specific `image_url`, `video_url(s)`, `sound`, `cfg_scale`, `mode`, `multi_shots`, `elements`, and frame fields.

| Family | Variant and endpoint | App | Media shape |
| --- | --- | --- | --- |
| Kling 2.5 Turbo | [Pro Text](https://open.higgsfield.ai/models/kling-video/v2.5-turbo/pro/text-to-video/api-reference) `kling-video/v2.5-turbo/pro/text-to-video` | Current | Text |
| Kling 2.5 Turbo | [Pro Image](https://open.higgsfield.ai/models/kling-video/v2.5-turbo/pro/image-to-video/api-reference) `kling-video/v2.5-turbo/pro/image-to-video` | Current | Required image |
| Kling 2.5 Turbo | [Standard Image](https://open.higgsfield.ai/models/kling-video/v2.5-turbo/standard/image-to-video/playground) `kling-video/v2.5-turbo/standard/image-to-video` | Current | Required image |
| Kling 2.6 | [Pro Text](https://open.higgsfield.ai/models/kling-video/v2.6/pro/text-to-video/api-reference) `kling-video/v2.6/pro/text-to-video` | Current | Text |
| Kling 2.6 | [Pro Image](https://open.higgsfield.ai/models/kling-video/v2.6/pro/image-to-video/api-reference) `kling-video/v2.6/pro/image-to-video` | Current | Required image |
| Kling 2.6 | [Motion Standard](https://open.higgsfield.ai/models/kling-video/motion-control/std/playground) `kling-video/motion-control/std` | Current | Required character image and motion video |
| Kling 2.6 | [Motion Pro](https://open.higgsfield.ai/models/kling-video/motion-control/pro/api-reference) `kling-video/motion-control/pro` | Current | Required character image and motion video |
| Kling 3.0 | [Standard Text](https://open.higgsfield.ai/models/kling-video/v3.0/std/text-to-video/api-reference) `kling-video/v3.0/std/text-to-video` | Current | Text |
| Kling 3.0 | [Standard Image](https://open.higgsfield.ai/models/kling-video/v3.0/std/image-to-video/api-reference) `kling-video/v3.0/std/image-to-video` | Current | Required image |
| Kling 3.0 | [Pro Text](https://open.higgsfield.ai/models/kling-video/v3.0/pro/text-to-video/api-reference) `kling-video/v3.0/pro/text-to-video` | Current | Text |
| Kling 3.0 | [Pro Image](https://open.higgsfield.ai/models/kling-video/v3.0/pro/image-to-video/api-reference) `kling-video/v3.0/pro/image-to-video` | Current | Required image |
| Kling 3.0 | [4K Text](https://open.higgsfield.ai/models/kling-video/v3.0/4k/text-to-video/playground) `kling-video/v3.0/4k/text-to-video` | Current | Text |
| Kling 3.0 | [4K Image](https://open.higgsfield.ai/models/kling-video/v3.0/4k/image-to-video/api-reference) `kling-video/v3.0/4k/image-to-video` | Current | Required image |
| Kling 3.0 | [Turbo Text](https://open.higgsfield.ai/models/kling-video/v3.0-turbo/text-to-video/playground) `kling-video/v3.0-turbo/text-to-video` | Current | Text |
| Kling 3.0 | [Turbo Image](https://open.higgsfield.ai/models/kling-video/v3.0-turbo/image-to-video/api-reference) `kling-video/v3.0-turbo/image-to-video` | Current | Required image |
| Kling 3.0 | [Motion Standard](https://open.higgsfield.ai/models/kling-video/v3/motion-control/std/playground) `kling-video/v3/motion-control/std` | Current | Required character image and motion video |
| Kling 3.0 | [Motion Pro](https://open.higgsfield.ai/models/kling-video/v3/motion-control/pro/api-reference) `kling-video/v3/motion-control/pro` | Current | Required character image and motion video |
| Kling O3 | [First/Last Frame](https://open.higgsfield.ai/models/kling-video/o3/first-last-frame/api-reference) `kling-video/o3/first-last-frame` | Current | First frame; optional last frame |
| Kling O3 | [Image Reference](https://open.higgsfield.ai/models/kling-video/o3/image-reference/api-reference) `kling-video/o3/image-reference` | Current | Image array and optional frames/elements |
| Kling O3 | [Video Edit](https://open.higgsfield.ai/models/kling-video/o3/video-edit/api-reference) `kling-video/o3/video-edit` | Current | Source video; optional images/elements |
| Kling O3 | [Video Reference](https://open.higgsfield.ai/models/kling-video/o3/video-reference/api-reference) `kling-video/o3/video-reference` | Current | Required video array; optional images/elements |
| Kling Omni | [First/Last Frame](https://open.higgsfield.ai/models/kling-video/omni/first-last-frame/playground) `kling-video/omni/first-last-frame` | Current | First frame; optional last frame |
| Kling Omni | [Image Reference](https://open.higgsfield.ai/models/kling-video/omni/image-reference/api-reference) `kling-video/omni/image-reference` | Current | Image array/elements |
| Kling Omni | [Video Edit](https://open.higgsfield.ai/models/kling-video/omni/video-edit/api-reference) `kling-video/omni/video-edit` | Current | Source video; optional images/elements |
| Kling Omni | [Video Reference](https://open.higgsfield.ai/models/kling-video/omni/video-reference/api-reference) `kling-video/omni/video-reference` | Current | Required video array; optional images/elements |

### Guided video workflows

[Genjutsu Motion Transfer workflow](https://open.higgsfield.ai/models/workflows/genjutsu/api-reference) points at the same operation as the direct Motion Transfer page, but its example has a `higgsfiled` spelling. Its generic API section also describes a different client/auth shape. Do not register it as a duplicate selectable route. The user supplied the direct Object Swap model ID, `higgsfield/genjutsu/object-swap/v1.0`; the [Object Swap workflow example](https://open.higgsfield.ai/models/workflows/genjutsu/object-swap/api-reference) spells it `higgsfiled/...`. [Cinema Studio workflow](https://open.higgsfield.ai/models/workflows/cinema-studio-4-0/playground) is another guide over the existing Cinema Studio operation.

## Implementation consequences

1. The current picker groups explicit route names by family. A future model-first picker with a separate supported-action control is planned in [next-enhancements.md](next-enhancements.md); keep the stable route IDs for saved records.
2. Attachment policy must describe ordered roles, optionality, per-kind limits, and a combined reference maximum. Wan 3.0 Prime's ten mixed references cannot be modeled by two independent limits alone.
3. Route-specific choices and typed controls are implemented where recorded in descriptors; remaining advanced settings are tracked in [model-controls-audit.md](model-controls-audit.md).
4. Workflow guides need a distinct representation from endpoint routes. Marketing guides need live preset pagination and conditional 1–2 image validation.
5. Never enable submission solely because a catalog row exists. Check the endpoint's exact schema, account access, JSON mapping, upload policy, and response handling first. Keep unresolved rows visible only as unavailable information if shown at all.
6. Starting rate labels remain manually editable in `ModelStartingRates.kt`. Recheck them separately from route correctness; some catalog rates are time-limited discounts.

## Follow-up verification

- Confirm the registered, user-supplied direct Genjutsu Object Swap ID against a reachable direct API reference or a non-billable model listing when available. The workflow example contains `higgsfiled`; the registered endpoint uses `higgsfield`.
- Confirm full input tables for Cinema Studio 4.0 and the existing SOUL Cinema route. The public pages inspected here did not expose usable parameter tables.
- Where an API table says `array[string]` without a maximum, record the limit as unknown until the playground or official docs supply one. Do not invent an unlimited or fixed cap.
- Verify mixed image/video maxima, accepted file types, ordered media semantics, and duration constraints in the public playground for every reference route.
- Verify all newly exposed routes against account access when test credentials are available. Do not put credentials or real media URLs in this document.
