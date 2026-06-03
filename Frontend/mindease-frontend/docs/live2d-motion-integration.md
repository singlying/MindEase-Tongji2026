# Live2D Motion Integration

The user chat page connects AI response semantics with the Live2D companion.

## Files

| File | Role |
| --- | --- |
| `src/views/user/chat/AIChatView.vue` | Parses streaming motion directives and updates UI state |
| `src/components/chat/Live2DCompanion.vue` | Loads the Live2D model and plays motion cues |
| `public/live2d/Hiyori` | Cubism model assets and motion files |

## Motion Directives

The backend asks the AI to prepend one directive:

```text
[[MOTION:neutral]]
[[MOTION:concern]]
[[MOTION:encouragement]]
[[MOTION:surprise]]
[[MOTION:shy]]
```

The frontend strips the directive from visible chat content and uses it to drive
the companion state.

## State Mapping

| Directive | UI Emotion |
| --- | --- |
| `neutral` | steady |
| `concern` | soothing |
| `encouragement` | encouraging |
| `surprise` | celebrating |
| `shy` | warm |

## Debug Controls

The motion debug panel is now gated behind `import.meta.env.DEV`. It remains
available during local development but is hidden in production builds and final
demo views.

## Failure Handling

- If Cubism runtime fails to load from the primary source, a CDN fallback is
  attempted.
- If a motion cannot play, the component stops the motion loop and keeps the
  model in an expression-driven idle state.
- Resize observer keeps the canvas layout aligned with the companion rail.

