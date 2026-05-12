# MindEase

MindEase: An LLM-based Intelligent Mental Health System featuring Virtual Emotional Companionship and RAG-driven Psychological Support.

Developed by the team from computer science school, Tongji University, under the guidance of Professor DengHao.

This repository currently contains both the frontend and backend of the project:

- Frontend: `Vue 3 + TypeScript + Vite + Element Plus`
- Backend: `Spring Boot 3 + MyBatis + MySQL + Redis + DashScope`

The project is designed as a course/demo system that covers a basic mental health service loop, including user registration, mood tracking, psychological scales, AI chat, counselor recommendation, and appointment management.

## Repository Structure

```text
Mindease/
├─ Mindease_backend/         # Spring Boot backend
├─ MindEase_frontend/        # frontend repository
│  └─ mindease-frontend/     # actual Vue 3 application
├─ mindease-init.sql         # initialization SQL
└─ README.md                 # repository overview
```

## Main Features

- User registration and login
- Mood diary and history tracking
- Psychological scale assessment
- AI chat with basic voice capabilities
- Counselor recommendation and appointment scheduling
- Emotion reports and partial statistical views

## Tech Stack

### Frontend

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- Axios

### Backend

- Spring Boot 3.5.8
- Java 21
- MyBatis
- MySQL 8
- Redis
- LangChain4j
- DashScope

## Prerequisites

To run the project locally, you will usually need:

- Node.js 20+
- Java 21
- Maven 3.9+
- Docker Desktop

Docker is mainly used for local database and Redis containers.

## Local Development Setup

Based on the current local integration setup, these are the commonly used ports:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8081`
- MySQL (Docker): `localhost:3307`
- Redis: `localhost:6379`

If any of these ports are already occupied on your machine, update the corresponding configuration.

## Running the Frontend

Start with:

```powershell
cd Frontend\mindease-frontend
npm install
npm run dev
```

If you only want to preview the UI, you can use mock mode.  
If you want to integrate with the backend, make sure `.env.development` points to the correct backend port.

## Running the Backend

Start with:

```powershell
cd Backend
mvn spring-boot:run
```

## Database and Redis

### MySQL

The commonly used local setup is:

- Host: `localhost`
- Port: `3307`
- Database: `mindease`
- Username: `root`
- Password: `1234`

If you use DataGrip or another database client, these are the values to fill in.

### Redis

AI chat, chat memory, and the current RAG implementation depend on Redis. Default setup:

- Host: `localhost`
- Port: `6379`

If Redis is not running, AI-related features will usually fail.

## AI Notes

The AI capabilities in this project currently depend on DashScope, including:

- Text chat
- Speech-to-text ASR
- Text-to-speech TTS
- Vector retrieval and basic RAG

The backend requires a valid `DASHSCOPE_API_KEY`.  
If the API key is invalid, or Redis is unavailable, AI chat, voice features, and retrieval-based responses may fail.

## RAG Overview

The current RAG implementation is lightweight:

- Load knowledge files from the `content` directory
- Split documents during application startup
- Generate embeddings using the embedding model
- Store embeddings in Redis
- Retrieve relevant chunks during chat to support the final answer

## Additional Notes

- There are also some standalone test-program directories in this repository, but they are not part of the main application.
- If you only care about the main product, focus on `Backend` and `Frontend/mindease-frontend`.
