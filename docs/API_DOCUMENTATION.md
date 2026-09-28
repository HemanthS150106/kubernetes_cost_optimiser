# REST API Specifications - Kubernetes Cost Optimizer

This document catalogs the REST API endpoints exposed by the Kubernetes Cost Optimizer backend on port `8080`.

---

## Swagger UI Documentation
OpenAPI specifications are compiled automatically. Once the application is running, API routes can be explored interactively at:
- **Swagger URL**: `http://localhost:8080/swagger-ui.html`
- **JSON Docs**: `http://localhost:8080/api-docs`

---

## 1. Authentication Endpoints

### POST `/api/v1/auth/login`
Validates user credentials and issues a stateless JWT.

#### Request Body
- Type: `application/json`
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- **Validation**: Username and Password cannot be blank.

#### Response (`200 OK`)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcyMTY1...",
  "username": "admin",
  "email": "admin@costoptimizer.k8s",
  "role": "ROLE_ADMIN"
}
```

#### Error Response (`400 Bad Request` or `401 Unauthorized`)
- Returns `401` on invalid password or user not found.

---

### POST `/api/v1/auth/register`
Creates an admin user profile.

#### Request Body
- Type: `application/json`
```json
{
  "username": "developer",
  "password": "strongPassword123",
  "email": "dev@company.com"
}
```
- **Validation**: Username must be between 3 and 20 characters; Password must be between 6 and 40 characters; Email must be in a valid format.

#### Response (`200 OK`)
`User registered successfully`

---

## 2. Cluster Analytics Endpoints

*All endpoints in this section require an `Authorization: Bearer <token>` header.*

### GET `/api/v1/cluster/overview`
Retrieves aggregated resource capacity, usage totals, and cost optimization opportunities.

#### Response (`200 OK`)
```json
{
  "totalNodes": 1,
  "totalPods": 12,
  "totalNamespaces": 4,
  "totalDeployments": 3,
  "totalDaemonSets": 1,
  "totalStatefulSets": 0,
  "totalServices": 8,
  "totalCpuCapacity": 4.0,
  "totalCpuRequest": 1.25,
  "totalCpuLimit": 2.5,
  "totalCpuUsage": 0.15,
  "cpuUtilizationPercentage": 3.75,
  "totalMemoryCapacityGb": 16.0,
  "totalMemoryRequestGb": 4.0,
  "totalMemoryLimitGb": 8.0,
  "totalMemoryUsageGb": 0.88,
  "memoryUtilizationPercentage": 5.5,
  "currentMonthlyCost": 212.0,
  "potentialMonthlySavings": 37.5,
  "optimizedMonthlyCost": 174.5,
  "nodes": [
    {
      "name": "minikube",
      "status": "Ready",
      "role": "control-plane",
      "cpuCapacity": 4.0,
      "cpuAllocatable": 4.0,
      "cpuUsage": 0.15,
      "memoryCapacityGb": 16.0,
      "memoryAllocatableGb": 16.0,
      "memoryUsageGb": 0.88,
      "podCount": 12,
      "estimatedMonthlyCost": 212.0
    }
  ],
  "pods": []
}
```

---

### GET `/api/v1/cluster/namespaces`
Lists all namespaces within the active Kubernetes cluster.

#### Response (`200 OK`)
```json
[
  "default",
  "kube-system",
  "kube-public",
  "production"
]
```

---

## 3. Recommendation Endpoints

*All endpoints in this section require an `Authorization: Bearer <token>` header.*

### GET `/api/v1/recommendations`
Lists generated optimization recommendations, with support for namespace or status filters.

#### Query Parameters
- `status` (Optional): `ACTIVE`, `DISMISSED`, `IMPLEMENTED`
- `namespace` (Optional): Namespace string, e.g. `production`

#### Response (`200 OK`)
```json
[
  {
    "id": 1,
    "clusterId": "default-cluster",
    "namespace": "production",
    "resourceName": "web-pod-547df/nginx",
    "resourceType": "POD",
    "type": "REDUCE_CPU_REQUEST",
    "severity": "MEDIUM",
    "currentCpuRequest": 0.5,
    "recommendedCpuRequest": 0.05,
    "currentMemoryRequestGb": 1.0,
    "recommendedMemoryRequestGb": 1.0,
    "currentReplicas": null,
    "recommendedReplicas": null,
    "estimatedMonthlySavings": 14.78,
    "status": "ACTIVE",
    "details": "CPU utilization is low (8% of requested 0.5 cores). Reduce request to 0.05 cores.",
    "createdAt": "2026-07-22T11:45:00",
    "updatedAt": null
  }
]
```

---

### PUT `/api/v1/recommendations/{id}/status`
Updates the status state of a recommendation.

#### Path/Query Variables
- `id` (Path): Recommendation database primary key (Long)
- `status` (Query parameter): `ACTIVE`, `DISMISSED`, `IMPLEMENTED`

#### Response (`200 OK`)
- Returns the updated recommendation JSON block.

---

### POST `/api/v1/recommendations/scan`
Triggers an immediate live cluster scan and generates recommendations.

#### Response (`200 OK`)
- Returns the updated list of `ACTIVE` recommendations.

---

### DELETE `/api/v1/recommendations/{id}`
Deletes a recommendation record.

#### Response (`204 No Content`)
- Returns empty response.
