# Safety Validation Notes

MindEase includes a lightweight crisis-keyword guard before sending AI chat
messages.

## Scope

The current guard is not a clinical diagnostic model. It is a deterministic
pre-check for high-risk phrases that should trigger a crisis-support prompt
before the AI response is requested.

## Backend Path

| Step | Component |
| --- | --- |
| Keyword matching | `SensitiveWordFilter` |
| API entry point | `POST /chat/check-sensitive-words` |
| Response VO | `SensitiveWordCheckVO` |
| Frontend handling | `AIChatView.vue` safety dialog |

## Expected Behavior

| Input Type | Expected Result |
| --- | --- |
| Empty or whitespace input | `containsSensitiveWord=false` |
| Normal stress or anxiety expression | `containsSensitiveWord=false` |
| Direct self-harm phrase | `containsSensitiveWord=true` |
| Multiple crisis phrases | Returns all matched dictionary terms |
| Repeated same phrase | Returns the term once because the dictionary is unique |

## Safety UX

When a crisis expression is detected, the frontend:

1. Stops the chat send flow before calling the LLM.
2. Shows a warning dialog with hotline and emergency guidance.
3. Encourages contacting trusted people or professional support.
4. Keeps longer-term MindEase features available after the safety prompt.

## Known Limitations

- Keyword matching can miss indirect intent.
- Keyword matching can produce false positives in educational or quoted text.
- A production-grade version should combine keyword rules, model-assisted risk
  classification, human escalation rules, and audit logging.

