# RAG Runtime Notes

This note describes how the MindEase backend wires the LLM, Redis memory, and
knowledge retrieval path.

## Runtime Components

| Component | Code Location | Purpose |
| --- | --- | --- |
| `ConsultantService` | `Backend/src/main/java/com/mindease/aiservice/ConsultantService.java` | LangChain4j AI service facade for chat and mood analysis |
| `AiConfiguration` | `Backend/src/main/java/com/mindease/config/AiConfiguration.java` | Creates chat model, streaming model, memory provider, and content retriever |
| `RedisChatMemoryStore` | `Backend/src/main/java/com/mindease/repository/RedisChatMemoryStore.java` | Persists recent conversation context in Redis |
| `content` resources | `Backend/src/main/resources/content` | Knowledge base documents loaded at application startup |

## Startup Flow

1. Spring loads `AiConfiguration` when `mindease.ai.enabled=true`.
2. Knowledge documents are loaded from the classpath `content` directory.
3. Documents are split into chunks with overlap.
4. Embeddings are generated with the configured DashScope embedding model.
5. Chunks are stored in Redis-backed embedding storage.
6. Chat requests use a content retriever with `maxResults=3` and `minScore=0.5`.

## Chat Flow

1. The frontend creates or selects a chat session.
2. The user sends a message through `/chat/message`.
3. `ChatServiceImpl` persists the user message.
4. The AI prompt requests a first-line Live2D motion directive.
5. `ConsultantService.chat` streams tokens back to the frontend.
6. After streaming completes, the cleaned AI message is saved to MySQL.

## Operational Notes

- Redis must be running for chat memory and vector retrieval.
- DashScope API key is read from `DASHSCOPE_API_KEY`.
- If AI is disabled, `AiFallbackConfiguration` provides a safe fallback response.
- The frontend handles motion directives during streaming, while backend storage
  strips directives from saved message text.

