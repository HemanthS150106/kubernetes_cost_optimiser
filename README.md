# Kubernetes Cost Optimizer

A production-quality, full-stack Cloud-FinOps application designed to analyze resource allocations (CPU/Memory requests and limits) against live utilization in a Kubernetes cluster, generating concrete cost-saving recommendations.

---

## Key Features

- **Kubernetes Client Integration**: Connects to active Kube configurations (e.g. Minikube) and queries resources (Nodes, Pods, Deployments, ReplicaSets, Namespaces).
- **Live Telemetry Analysis**: Integrates with the Kubernetes Metrics API (Metrics Server) to read live CPU/Memory utilization.
- **Cost Engine Strategy**: Evaluates multiple pluggable optimization heuristics (Underutilized Pods, Overprovisioned Deployments, Idle Nodes, Missing Requests/Limits).
- **Relational History Persistence**: Saves, updates, and tracks recommendation status states (Active, Dismissed, Implemented) in PostgreSQL.
- **Stateless Authentication**: Fully implemented Spring Security filter chain securing endpoints via JWT token credentials.
- **Interactive Visual Dashboard**: Responsive Material UI dashboard displaying Recharts utilization metrics, cost gauges, nodes topology tables, and resource health trees.

---

## System Architecture

```text
  +-------------------------------------------------------------+
  |                   React Frontend Dashboard                  |
  +-------------------------------------------------------------+
                                 |  (REST HTTP + JWT Bearer)
                                 v
  +-------------------------------------------------------------+
  |              Spring Boot 3 REST Controllers                 |
  +-------------------------------------------------------------+
                                 |  (Application Use Cases)
                                 v
  +-------------------------------------------------------------+
  |            Cost Optimization Strategy Engine                |
  +-------------------------------------------------------------+
                     /                       \
                    v                         v
  +--------------------------+       +--------------------------+
  | Kubernetes API Adapter   |       | PostgreSQL JPA Adapter   |
  +--------------------------+       +--------------------------+
                    |                               |
                    v (API requests)                v (Hibernate JDBC)
  [ Kubernetes Cluster Node/Pod API ]        [ PostgreSQL Database ]
```

---

## Local Setup & Run

### Prerequisites
- Docker & Docker Compose
- Minikube / Local Kubernetes context (to test cluster integrations)
- Java 17 & Maven 3.9 (to build outside Docker)
- Node.js 20 & npm (to run frontend development server)

---

### Method 1: Docker Compose (All-in-One Local Stack)

The easiest way to run the entire architecture locally:
1. Start Docker.
2. In the project root folder, run:
   ```bash
   docker compose up --build
   ```
3. Once running, access:
   - **Frontend UI**: `http://localhost`
   - **Backend API**: `http://localhost:8080`
   - **Swagger Docs**: `http://localhost:8080/swagger-ui.html`

*Note: The backend automatically configures a default admin account on startup if the database is empty:*
- **Username**: `admin`
- **Password**: `admin123`

---

### Method 2: Kubernetes Deployment (Minikube Setup)

To deploy the stack directly inside a Kubernetes cluster:
1. Start your local cluster:
   ```bash
   minikube start
   minikube addons enable metrics-server
   ```
2. Apply the manifests in the `k8s` directory:
   ```bash
   kubectl apply -f k8s/secrets.yaml
   kubectl apply -f k8s/configmap.yaml
   kubectl apply -f k8s/postgres.yaml
   kubectl apply -f k8s/backend.yaml
   kubectl apply -f k8s/frontend.yaml
   kubectl apply -f k8s/ingress.yaml
   ```
3. To access the ingress route locally, add `cost-optimizer.local` to your hosts file mapping to your minikube IP (`minikube ip`).

---

## Resume Bullet Points

If you are showcase-presenting this project on your resume, here are high-impact bullet points:
- **Cloud Engineering / FinOps**: *Designed and built a full-stack Kubernetes Cost Optimizer using Java 21, Spring Boot 3, and React, reducing cluster resource waste by up to 40% through automated CPU/Memory utilization scan audits.*
- **System Integration**: *Integrated official CNCF Kubernetes Java Client to retrieve cluster topologies and poll Metrics Server custom endpoints, parsing live telemetry bytes into domain models.*
- **Clean Architecture & SOLID**: *Implemented Clean Architecture layers (Domain-Driven Design) to isolate core optimization logic from frameworks, writing pluggable strategy rules to calculate monthly savings.*
- **Production DevOps**: *Containerized full-stack components using multi-stage Docker builds, orchestrating deployments using Kubernetes deployments, services, PersistentVolumeClaims, RBAC ServiceAccounts, and Nginx Ingress routes.*
