# Deployment Checklist

This checklist supports the Phase 5 deployment and closing stage.

## Backend

- Java 21 installed.
- Maven 3.9+ available.
- MySQL database created.
- Redis service reachable.
- `DASHSCOPE_API_KEY` configured for AI features.
- `MINDEASE_JWT_SECRET_KEY` configured with a sufficiently strong key.
- `MINDEASE_SECURITY_AES_KEY` configured as a 32-byte AES key.
- Backend port aligned with frontend environment: `8081`.

## Frontend

- Node.js 20+ installed.
- Dependencies installed with `npm install`.
- `VITE_API_BASE_URL` points to the backend host and port.
- `VITE_USE_MOCK=false` for integrated demo.
- Production build hides Live2D motion debug controls.

## Smoke Checks

| Check | Expected Result |
| --- | --- |
| Login | User receives token and profile loads |
| Mood diary | Create and list mood logs |
| Assessment | Load scale, submit answers, view result |
| Chat | Create session and stream AI response |
| ASR | Upload audio and receive transcription |
| TTS | Receive `audio/mpeg` response |
| Appointment | List slots and create appointment |
| Safety | Crisis keyword dialog appears before AI send |

## Archive

- Keep canonical frontend source under `Frontend/mindease-frontend/src`.
- Keep canonical backend source under `Backend/src/main/java`.
- Avoid committing generated build outputs.
- Keep final screenshots and project documents separate from source commits
  unless they are explicitly part of the final deliverable archive.

