# Engineering Decisions Log - Kubernetes Cost Optimizer

This document chronicles the primary engineering decisions, technology choices, and architectural trade-offs made during the development of this project.

---

## 1. Core Technology Choices

### Backend Framework: Spring Boot 3
- **Decision**: Use Spring Boot 3.2.5 over raw Java EE or Go.
- **Rationals**:
  - The Spring ecosystem provides built-in enterprise frameworks: Spring Security (JWT), Spring Data JPA, Actuator, and Spring validation, allowing us to build a secure, validated API quickly.
  - Active scheduling features (`@Scheduled`) enable running background cluster scans out-of-the-box.
- **Alternatives**: Go (Go has a smaller memory footprint and native Kubernetes client support, but Java Spring Boot offers richer abstraction layers, JPA persistence patterns, and JWT security ecosystems standard in enterprise setups).

### Database: PostgreSQL
- **Decision**: PostgreSQL 15 over NoSQL (MongoDB) or SQLite.
- **Rationals**:
  - Recommendations, user logins, and settings are relational. Relational integrity (foreign keys, transactions) prevents corruption.
  - PostgreSQL is the industry standard for microservices due to its performance, transactional guarantees, and cloud compatibility.
- **Alternatives**: SQLite (useful for prototyping but unsuitable for production multi-container deployments where concurrent writes and persistence are required).

### Frontend bundler: React + Vite + TypeScript
- **Decision**: React with Vite template over Webpack/Next.js.
- **Rationals**:
  - Vite uses esbuild to bundle assets, providing sub-second Hot Module Replacement (HMR) and fast build times.
  - TypeScript catches type errors at compile time, preventing runtime UI crashes.
  - Recharts and Material UI provide rich visualization components suited for dashboards.

---

## 2. Integration and Architecture Decisions

### Kubernetes Integration: Official Kubernetes Java Client
- **Decision**: Use the official CNCF Java client (`io.kubernetes:client-java`) over Fabric8.
- **Rationals**:
  - The official client is maintained directly by the Kubernetes team, ensuring day-one support for new Kubernetes API additions.
  - It supports direct API clients (`CoreV1Api`, `AppsV1Api`) and generic adapters (`CustomObjectsApi`) to retrieve custom metrics from the Metrics Server.
- **Trade-offs**: The official client's builder classes have complex method signatures (like requiring 11 null parameters on listing endpoints), but it offers native support and matches production cloud installations.

### Architecture Pattern: Clean Architecture
- **Decision**: Structure the application into Domain, Application, Infrastructure, and Presentation packages.
- **Rationals**:
  - Separating the domain from dependencies ensures that changes in database engines (e.g. swapping PostgreSQL for MySQL) or updates to the Kubernetes Java client **do not touch** the core cost-recommendation business rules.
  - Testing is simplified: the domain optimization engine can be tested using mock data inputs without spinning up database containers or active Kubernetes contexts.
- **Trade-offs**: Adds minor boilerplate files (mapping domain entities to JPA entities via MapStruct), but guarantees project readability and maintainability at scale.
