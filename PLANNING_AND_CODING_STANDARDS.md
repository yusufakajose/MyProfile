# LinkGrove – Planning & Coding Standards

> "Build the open-source, self-hostable, analytics-driven alternative to Linktree. Ship fast, but never at the expense of quality."

---

## 1. Product Vision
LinkGrove empowers anyone to create a single, beautiful micro-landing page for all their links **with real-time analytics**—all runnable locally via Docker-Compose or self-hostable on any cloud.

Business goals:
1. Lightning-fast public pages (≤ 50 ms P95).
2. Zero click-tracking latency for end-users (redirects feel instant).
3. Plug-and-play deployment (`docker-compose up`).
4. Codebase that **demonstrates senior-level engineering practices** and is easy for contributors to understand.

---

## 2. High-Level Architecture
```
┌──────────────┐   REST / JWT   ┌───────────────┐        AMQP         ┌─────────────────┐
│   Frontend   │ ─────────────► │  Spring API    │ ───── publish ───► │  RabbitMQ Queue │
│ (React/Next) │                │  Gateway       │                   └────────┬────────┘
└─────┬────────┘                │  (Spring Boot)│                            │consume
      │                         └──────┬────────┘                            ▼
      │ Static/SSR                   (Redis)                       ┌─────────────────┐
      ▼                               ▲                            │ AnalyticsWorker │
Public Profile (Nginx)💨           cache│                            │  (Spring Boot) │
                                      │                            └──────┬────────┘
                                      │ SQL                            write
                                      ▼                                ▼
                                  ┌───────────┐                   ┌────────────┐
                                  │PostgreSQL │◄──────────────────│ TimescaleDB│
                                  └───────────┘        optional   └────────────┘
```
* Full diagram source in `/docs/architecture.drawio` (TBD).

---

## 3. Technology Choices & Rationale
| Concern              | Tech                     | Why |
|----------------------|--------------------------|-----|
| Backend Framework    | Java 21 + Spring Boot 3  | Modern LTS, mature ecosystem, strong annotation-based config |
| API Auth             | Spring Security + JWT    | Stateless sessions, easy with SPA frontend |
| Primary Store        | PostgreSQL 15            | ACID, JSONB support, free, reliable |
| Caching              | Redis 7                  | Extreme read perf, distributed cache |
| Async Processing     | RabbitMQ 3 (AMQP 0-9-1)  | Durable queues, easy local dev via container |
| Object Storage       | AWS S3 / Local MinIO     | Handles profile pics, CDN-ready |
| Frontend             | Next.js 14 (React 18)    | Hybrid SSG/SSR for fast public pages |
| Containerisation     | Docker & Docker-Compose  | Single-command bootstrap |
| Testing              | JUnit 5, Mockito, Testcontainers | CI-friendly, realistic env |

---

## 4. Development Roadmap
All tasks tracked in GitHub Projects Kanban. Each **phase produces a shippable increment**.

### Phase 0 – Foundations
- [ ] Initialise mono-repo structure (`/backend`, `/frontend`, `/infra`).
- [ ] Add global `.editorconfig`, `Checkstyle`, `Spotless`, `Prettier`.
- [ ] Configure GitHub Actions: **build → test → docker image → push**.
- [ ] Create `docker-compose.yml` with Postgres, Redis, RabbitMQ.

### Phase 1 – Auth & User Model
- [ ] Entities: `User`, `Role`.
- [ ] Endpoints: `POST /auth/register`, `POST /auth/login` (JWT).
- [ ] Protect Swagger & all dashboard routes.
- [ ] Unit tests for `AuthService`.

### Phase 2 – Profile & Link Management (Dashboard)
- [ ] Entities: `UserProfile`, `Link` (FK → profile, sortable).
- [ ] CRUD Endpoints under `/api/dashboard/**`.
- [ ] S3/MinIO integration for avatar upload.
- [ ] React dashboard UI (Create/Edit/Delete/Reorder Links).

### Phase 3 – Public Profile Page + Caching
- [ ] `GET /{username}` returns cached HTML/JSON.
- [ ] `@Cacheable` Redis with eviction on profile update.
- [ ] Nginx static hosting / Next.js SSG fallback.
- [ ] Lighthouse score ≥ 95.

### Phase 4 – Click Tracking Pipeline
- [ ] `GET /r/{linkId}` → redirects (302 Found) + publishes `LinkClickEvent`.
- [ ] `AnalyticsWorker` consumes events, aggregates counts per link & profile daily.
- [ ] `@Transactional` batch upserts.
- [ ] Integration tests with Testcontainers + RabbitMQ.

### Phase 5 – Analytics Dashboard
- [ ] Endpoints: `/api/dashboard/analytics/summary`, `/timeseries`.
- [ ] Frontend charts with `recharts`.

### Phase 6 – Observability & Hardening
- [ ] Spring Actuator, Prometheus, Grafana dashboards.
- [ ] Centralised error handling with `@ControllerAdvice`.
- [ ] Rate-limiting on public endpoints.

### Phase 7 – Deployment & Docs
- [ ] Multi-stage Dockerfiles (slim JRE).
- [ ] Terraform scripts for AWS (RDS, ElastiCache, ECR, ECS or Lightsail).
- [ ] Stunning README with badges, screenshots, and live demo link.

### Post-MVP Ideas
- Custom domains, webhooks, theming marketplace, OAuth social login, admin panel.

---

## 5. Coding Standards & Best Practices
### 5.1 Java & General Style
1. **Consistent Formatting**: `Spotless + google-java-format` enforces style on commit.
2. **Naming**:
   * Classes: `PascalCase` (`UserProfileController`)
   * Methods/vars: `camelCase` (`getUserProfile()`)
   * Constants: `SCREAMING_SNAKE_CASE`
3. **No Magic Numbers/Strings**: extract to `static final` constants or config.
4. **SRP**: One class – one responsibility. Refactor when > ~300 LOC.
5. **Null-safety**: Prefer `Optional`, annotate with `@NotNull/@Nullable` where helpful.

### 5.2 Spring Boot Structure
```
backend/src/main/java
└── com.linkgrove
    ├── LinkGroveApplication.java
    ├── config        (security, jackson, cache...)
    ├── controller    (REST endpoints)
    ├── dto           (API contracts)
    ├── service       (business logic)
    ├── model/entity  (JPA @Entity)
    ├── repository    (Spring Data JPA)
    ├── event         (AMQP payloads)
    └── exception     (custom errs)
```

Rules:
- **Controller → Service → Repository** only; no skipping layers.
- Use **constructor injection** (`@RequiredArgsConstructor`).
- **DTOs only** in controllers; never expose entities.
- Annotate service write-methods with `@Transactional`.
- `@EntityGraph`/`JOIN FETCH` to avoid N+1.

### 5.3 Exception Handling
- Global `@ControllerAdvice` converts exceptions → JSON `{code, message, details}`.
- Map validation errors (Bean Validation) to 400 with field-level messages.

### 5.4 Caching Strategy
| Layer | What is cached | TTL/Eviction |
|-------|---------------|--------------|
| Service (`UserProfileService#getPublicProfile`) | `UserProfileDTO` (JSON) | 5 min TTL, evict on update |
| Worker | Aggregated daily stats | 1 d |  
| Static page (optional SSR) | Rendered HTML | 1 min |

### 5.5 Testing Guidelines
1. **Unit**: Service logic with JUnit + Mockito.
2. **Integration**: `@SpringBootTest` with Testcontainers (Postgres, RabbitMQ).
3. **E2E**: Playwright/Cypress for dashboard & public page.
4. **Coverage Goal**: 80 % lines on backend modules.
5. Test names: `methodUnderTest_whenCondition_expectedBehavior()`.

### 5.6 Git Workflow
1. Branch naming: `feat/<scope>`, `fix/<scope>`, `chore/<scope>`.
2. PR template enforcing description, screenshots, and checklist.
3. Conventional commits (`feat:`, `fix:`, `docs:` ...).
4. Squash & merge; CI must pass before merge.

### 5.7 Security Checklist (MVP)
- BCrypt password hashing (`12` rounds).
- HTTPS enforced (behind proxy in prod).
- JWT rotation & refresh tokens.
- Input validation & length limits.
- Rate limiting on auth routes.

---

## 6. Definition of Done (DoD)
A task/PR is complete when:
1. Code compiles, lint passes, tests green.
2. Story acceptance criteria met.
3. Additional tests added when relevant.
4. Documentation updated (JavaDoc, README, or ADR).
5. Reviewed & approved by at least one maintainer.

---

## 7. Contributor On-Boarding (TL;DR)
```bash
# 1. Clone & start stack
$ git clone https://github.com/<you>/linkgrove.git
$ cd linkgrove
$ docker-compose up -d

# 2. Import backend into IntelliJ, enable Lombok plugin.

# 3. Run backend
$ ./mvnw spring-boot:run

# 4. Visit Swagger UI
http://localhost:8080/swagger-ui.html
```

---

Happy shipping! 🚀

