# Workflow API integration (future UI work)

This file records workflow-specific API behavior separately from direct model routes. The app does not currently expose these guided workflows as picker entries.

## Marketing guides

[Graphic Ads](https://open.higgsfield.ai/models/workflows/graphic-ads/api-reference), [Marketplace Design](https://open.higgsfield.ai/models/workflows/marketplace-design/api-reference), and [Product Shots](https://open.higgsfield.ai/models/workflows/product-shots/api-reference) guide a request to the existing `marketing-studio/image` endpoint. They are not three new generation endpoints.

The guided mode uses `enhance_prompt=true` and a `preset_id` chosen from `GET https://api.higgsfield.ai/marketing-studio/image/presets?size=50`. The preset list is paginated; follow its cursor. Require one product image, allow an optional second model image, and submit the selected live preset UUID. Handle loading, no presets, failed fetch, and retry states. Preset IDs in examples are illustrative and must not be embedded in the app. The direct model mode remains a separate form with up to 16 images and `enhance_prompt=false`.

## Genjutsu guides

[Motion Transfer](https://open.higgsfield.ai/models/workflows/genjutsu/api-reference) and [Object Swap](https://open.higgsfield.ai/models/workflows/genjutsu/object-swap/api-reference) show the same input shape as their direct model operations: one video, reference images, an optional prompt, and resolution. Their workflow examples spell the provider `higgsfiled`; the [direct Motion Transfer page](https://open.higgsfield.ai/models/higgsfield/genjutsu/motion-transfer/v1.0/api-reference) spells it `higgsfield`. Treat `higgsfiled` as a documentation typo, not a second selectable route. The direct Object Swap model ID and account availability still need a live check.

## Integration boundary

Workflow guides can reuse the existing upload, submit, poll, and result persistence path. The extra work is a guided form, preset fetching when applicable, conditional validation, and storing chosen guide/preset state for faithful retry. Guided forms and preset fetching are not yet implemented.
