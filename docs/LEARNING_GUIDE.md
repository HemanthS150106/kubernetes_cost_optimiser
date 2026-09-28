# Step-by-Step Learning Guide: Kubernetes Cost Optimizer

This guide is designed to teach you the entire architecture, technologies, and patterns used in this project from scratch. It assumes you know only basic Java.

---

## Section 1: Spring Boot & Dependency Injection

### 1. Inversion of Control (IoC) and Beans

#### What
In traditional programming, you instantiate objects using the `new` keyword:
```java
UserService service = new UserService();
```
With **Inversion of Control**, the framework (Spring) controls the lifecycle of objects. It instantiates classes, wires them together, and manages them. These Spring-managed objects are called **Beans**.

#### Why
Decoupling. If class `A` instantiates class `B` directly, you cannot test `A` without `B`. By letting Spring manage and inject them, we can easily swap `B` with a mock.

#### How
We annotate classes with stereotypes like `@Component`, `@Service`, or `@Repository`. Spring scans these annotations at startup, creates them, and stores them in the **IoC Container** (or ApplicationContext).

---

### 2. Dependency Injection (DI)

#### What
Passing required dependencies into a class rather than letting the class create them.

#### How (Constructor Injection)
Spring looks at the constructor parameters of a bean and passes matching registered beans.
```java
@Service
public class ClusterAnalysisUseCase {
    private final ClusterRepository clusterRepository;

    // Constructor Injection
    public ClusterAnalysisUseCase(ClusterRepository clusterRepository) {
        this.clusterRepository = clusterRepository;
    }
}
```

#### Advantages
- **Testability**: You can write unit tests by passing mocks into the constructor.
- **Null Safety**: Beans cannot be initialized in a partially constructed null state.

---

## Section 2: Core Kubernetes & Client Mappings

```text
+-------------------------------------------------------+
|                 Kubernetes Cluster                    |
|                                                       |
|  +--------------------+       +--------------------+  |
|  |       Node 1       |       |       Node 2       |  |
|  |                    |       |                    |  |
|  | +----------------+ |       | +----------------+ |  |
|  | |      Pod       | |       | |      Pod       | |  |
|  | |                | |       | |                | |  |
|  | |  [Container]   | |       | |  [Container]   | |  |
|  | |  Req: 100m CPU | |       | |  Req: 250m CPU | |  |
|  | +----------------+ |       | +----------------+ |  |
|  +--------------------+       +--------------------+  |
+-------------------------------------------------------+
```

### 1. Resource Requests vs. Limits

#### What
- **Requests**: The minimum amount of CPU and Memory guaranteed to a container. The scheduler uses requests to find a suitable Node.
- **Limits**: The absolute maximum amount of CPU and Memory a container is allowed to consume.
  - If a container exceeds its Memory Limit, it is **OOM-Killed** (Out of Memory).
  - If a container exceeds its CPU Limit, it is **throttled** (slowed down).

#### Why Optimize?
Many developers allocate 1 CPU and 2GB Memory to their containers just to be safe, while actual usage might average 0.05 CPU and 100MB Memory. The cloud provider charges you based on the **Allocated Nodes** needed to cover these high requests, resulting in **wasted expenses**.

---

### 2. Metrics Server & Kube Client API

#### What
- **Metrics Server**: A cluster-wide aggregator of resource usage data (CPU/Memory).
- **Kubernetes Java Client**: An API library that communicates with the cluster's API Server over HTTP, retrieving pod lists and live usage numbers.

#### Flow of Metrics Data
1. Kubelet on each node gathers usage.
2. Metrics Server pulls usage stats from Kubelets.
3. The Cost Optimizer calls `/apis/metrics.k8s.io/v1beta1/pods` via the Java client.
4. The Cost Engine compares the Requests (static spec) vs. Usage (live telemetry).

---

## Section 3: React & Frontend Architecture

### 1. React Hooks

#### What
Functions that let you hook into React state and lifecycle features from functional components.
- `useState`: Holds variables that trigger re-rendering when updated.
- `useEffect`: Triggers side-effects (like fetching data) after a component mounts.

---

### 2. React Query (TanStack Query)

#### What & Why
A library for fetching, caching, and updating asynchronous server state in React.
- **Why**: Traditional fetching with `useEffect` + `fetch` requires writing state variables for loading, error, and caching. React Query automates this, providing loading variables (`isLoading`), auto-retry, and automatic cache invalidation.

---

## Section 4: Mini Exercises to Learn

### Exercise 1: Write a Custom Strategy
**Task**: Write a rule strategy `UnusedServiceRule` that flags Kubernetes Services that have no active pods backing them.
- *How*:
  1. Inspect the Service spec selectors.
  2. Match selectors against Pod labels.
  3. If no pods match, generate a `DELETE_UNUSED_SERVICE` recommendation with estimated savings of $5/month.

### Exercise 2: Mock a Kubernetes Node
**Task**: Write a JUnit test in `RecommendationEngineTest.java` that creates a simulated node containing 8 CPUs and 32GB RAM, runs `IdleNodeRule`, and asserts that a `DRAIN_NODE` recommendation is generated.
