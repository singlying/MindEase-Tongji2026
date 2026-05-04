### Assignment B Report Of Java Enterprise Application Development

# MindEase - Intelligent Psychological Support and Counseling System

---

- Project: MindEase - Intelligent Psychological Support and Counseling System
- Team 01
  2252584 欧宇轩 email：2252584@tongji.edu.cn
  2252752 刘继业 email：2252752@tongji.edu.cn
  2253377 李 航 email：2253377@tongji.edu.cn
- Timeline: Sep-Dec 2024 (3 months)
- Repository & demo links:
  Frontend: https://github.com/Effulgence12/MindEase_2025_TongjiUniversity_software_frontend.git
  Backend: https://github.com/Ericufo/Mindease.git

# 1. Project Overview

## 1.1 Background

With the continuous acceleration of the pace of modern society, college students and urban youth are facing increasing pressure on study, work and life, and mental health has become a common and urgent social issue to be solved.

Growing mental health challenges in modern society
Barriers to traditional counseling:

- High cost and time constraints
- Privacy concerns
- Complex appointment procedures

Gap: Need for accessible, immediate, and data-driven support

## 1.2 Solution

As a data-driven psychological support and counseling system, MindEase aims to solve the pain points of traditional psychological counseling services (difficult appointments, high prices, long time-consuming, chaotic matching and other outstanding problems).

For mental health practitioners, MindEase can help them build efficient digital workspaces.

Comprehensive platform integrating:

- Daily mood tracking
- AI-powered instant consultation
- Deep counselor matching
- Psychological assessments
- 24/7 accessibility with privacy protection

## 1.3 Innovation & Creativity

Instant Feedback Loop: AI fills the gap between appointments
Unified Experience: Breaking silos between isolated features
Hybrid Service Model: AI screening + professional counseling
Proactive Intervention: System detects patterns and suggests actions
Data-Driven Personalization: LLM-powered semantic matching

# 2. System Functionalities

## 2.1 Architecture Overview

- Three portals: User (9 modules), Counselor (4 modules), Admin (2 modules)
- 40+ RESTful APIs, 12 database entities

## 2.2 User Portal Features

## 2.3 Counselor Portal

## 2.4 Admin Portal

# 3. User Manual

## 3.1 Scenario Walkthroughs (with screenshots)

**Scenario 1: New User Registration & First Mood Entry** (8 steps)

- Register → Dashboard → Navigate mood diary → Create entry → AI analysis → View list

**Scenario 2: Taking GAD-7 Assessment** (9 steps)

- Browse scales → Select GAD-7 → Answer 7 questions → View results (score 10/21 = Moderate) → Recommendations

**Scenario 3: Finding & Booking Counselor** (10 steps)

- Smart recommendations → View profile → Check reviews → Select time slot → Confirm booking → Receive notification

**Scenario 4: Counselor Workflow** (5 steps)

- Login → View pending appointments → Confirm booking → Configure schedule

**Scenario 5: Admin Audit** (5 steps)

- View pending list → Review credentials → Approve/reject with remarks

## 3.2 Deployment And Configurations Guide

**Backend**: Java 17, MySQL 8.0, Maven build  
**Frontend**: Node 16+, npm install, vite dev/build  
**Docker**: Compose file with mysql + backend + frontend services

# 4. System Architecture & Design

## 4.1 System Overview

Client: Vue.js responsive web application
Server: Spring Boot RESTful API
Database: MySQL
AI Integration: LLM APIs for NLP tasks

## 4.2 Technology Stack

- Backend: Spring Boot 3.x (Jakarta EE), MySQL 8.0
- Frontend: Vue.js 3, Element Plus
- Security: JWT + BCrypt
- AI: OpenAI/ChatGLM APIs
- Build: Maven + Vite

## 4.3 Subsystems Design

# 5. Data Storage

## 5.1 Database Choice: MySQL 8.0

- ACID compliance for data integrity
- Excellent Spring integration
- Team familiarity

## 5.2 ER Diagram

## 5.3 Key Table Schemas

### 5.4 Normalization & Indexes

- 3NF normalization (eliminate redundancy)
- Indexes on foreign keys, username, date columns
- Composite indexes for multi-column queries

## 6. Engineering Highlights Of Java

## 6.1 Highlight 1: Springdoc OpenAPI Documentation

**Implementation**:

**Features**:

**Benefits**:

## 6.2 Highlight 2:

# 7. AI Tools Usage (4 pages)

## 7.1 Tools & Usage

- Design Phase: we used ChatGPT to draft system roles, REST API contracts, and data models; Gemini3 helped us sketch the first frontend layouts and flows.
- Implementation Phase: we coded in Cursor/Trae with inline completions and quick refactors; AI code review hints were used to spot unsafe null checks and unused code; we asked LLMs to produce small utility snippets (e.g., date formatting) that we then reviewed.
- Testing Phase: we asked AI to propose edge cases for booking and payment flows, and to draft Jest/Vitest-style cases; we kept only the cases that matched our actual API.
- Documentation: Apifox generated OpenAPI/endpoint docs from our controllers; Deepseek/Doubao helped polish README text and release notes with simple wording.
- Prompt discipline: we kept prompts short, always added current file context, and avoided sending secrets or tokens.

## 7.2 Benefits

- Faster delivery: we estimate ~25–30% time saved on boilerplate, docs, and basic refactors.
- Better consistency: suggested code patterns reduced style drift across Vue components and Java services.
- Quicker prototyping: we could try multiple UI/UX ideas in minutes before committing to one.
- Broader test ideas: AI proposed edge cases (expired slots, role mismatch) we might miss under time pressure.

## 7.3 Limitations & Countermeasures

- Limitations: AI can hallucinate APIs, miss project-specific rules, and propose insecure defaults; context windows are limited; license/provenance is unclear.
- Countermeasures: we keep human code review mandatory; we run linters/tests before merge; we do not paste secrets or production data; we restate business rules in prompts; we treat AI output as drafts and rewrite when logic is unclear.

# 8. Additional Details

## 8.1 Team Collaboration

Version control: we use GitHub with feature branches, squash merges, and protected main; every merge needs CI green and one reviewer.  
Project management: we plan 2-week sprints in Feishu, track tasks and bugs in a kanban board, and keep a release checklist for each demo.  
Communication: weekly standups plus WeChat for daily sync; shared meeting notes record decisions on API changes and UI breaks.  
Division of labor: two backend engineers on Spring Boot + MySQL, one frontend engineer on Vue3/TS + Element Plus; we cross-review to keep API/DTO aligned.  
Environments: dev and test environments are separated; we tag releases and maintain a changelog for rollback.

## 8.2 Technical Challenges & Solutions

Challenge 1: LLM integration — backend streams replies via `ConsultantService.chat(...)` with Reactor `Flux`, buffers full AI output to persist after streaming, and keeps per-session context in Redis; frontend renders streaming tokens.  
Challenge 2: Safety guard in AI chat — added a sensitive-word precheck API plus a blocking alert on the frontend; if high-risk terms are detected, the conversation stops and crisis info is shown.  
Challenge 3: Multi-role auth — JWT role claims with route guards on the frontend and controller/service checks on the backend to separate user, counselor, and admin access.  
Challenge 4: Appointment detail consistency — counselor “view detail” now merges list + detail API data to show user notes and export recent emotion report (export still awaits backend to return `userId`).  
Challenge 5: File upload and audit — avatar/certificate/scale cover share the same upload endpoint; the frontend enforces MIME/size limits and uses returned URLs for audit and display.

## 8.3 Future Improvements

- Gradual microservice split (chat, recommendation, booking) behind an API gateway.

- Mobile app (Flutter/React Native) to reuse API and push notifications.
- Analytics dashboard: cohort retention, booking funnels, counselor performance; add alerts and tracing for LLM latency.
