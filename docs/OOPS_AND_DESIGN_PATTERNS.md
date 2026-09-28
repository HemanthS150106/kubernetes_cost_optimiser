# OOP Concepts and Design Patterns - Kubernetes Cost Optimizer

This document explains the OOP principles and design patterns applied in this codebase, explaining their trade-offs, structures, and benefits.

---

## Part 1: OOP Principles Applied

### 1. Abstraction
- **What is it**: Hiding implementation details and showing only the essential structure/ports.
- **Where used**: `ClusterRepository` and `RecommendationRepository` interfaces.
- **Why used**: The core domain needs to know how to save recommendations and get nodes/pods, but **does not need to know** that it's talking to a PostgreSQL database via JPA, or to Kubernetes via an HTTP client.
- **Example from Project**:
```java
public interface ClusterRepository {
    List<NodeResourceInfo> getNodes();
    List<PodResourceInfo> getPods();
}
```

### 2. Polymorphism
- **What is it**: The ability of an object to take on many forms. A single interface invocation resolves to different concrete behaviors.
- **Where used**: Invoking `evaluate(...)` on different strategy classes implementing `RecommendationRule`.
- **Example from Project**:
```java
for (RecommendationRule rule : rules) {
    recommendations.addAll(rule.evaluate(pods, nodes, cpuRate, memoryRate));
}
```

### 3. Composition over Inheritance
- **What is it**: Designing classes by composing them with other objects (has-a) rather than inheriting behaviors from a parent class (is-a).
- **Where used**: `CostOptimizationEngine` uses a list of `RecommendationRule` strategy objects. We avoid creating a complex hierarchy of `AbstractRecommendationEngine` classes.
- **Benefits**: Extensibility. Adding a new recommendation rule requires writing a single class and appending it to a list, without risking modifications to a base class.

---

## Part 2: Design Patterns Implemented

### 1. Strategy Pattern

#### Problem Solved
The system evaluates multiple independent heuristics to optimize costs. Placing all rules in a single class results in a massive, unmaintainable method violating the Single Responsibility Principle.

#### Reason for Choosing
Enables adding, removing, or modifying a specific cost rule strategy at compile-time without touching the core engine runner.

#### ASCII Class Diagram
```text
  +----------------------+
  |  RecommendationRule  | <--------------- Interface
  +----------------------+
            ^
            | (implements)
   +--------+---------+--------------------+
   |                  |                    |
+--+---------------++-+---------------++---+--------------+
| UnderutilizedPod || MissingRequests || IdleNodeRule     |
+------------------++-----------------++------------------+
```

#### Real-World Analogy
A pricing calculator at a retail checkout. Depending on the coupon presented, it executes a different discount strategy (e.g., 10% off, free shipping, buy-one-get-one).

---

### 2. Adapter Pattern (Wrapper)

#### Problem Solved
The Kubernetes Java Client library has its own complex API structures (e.g. `V1PodList`, `Quantity`, `CoreV1Api`). Using them directly in the domain layer couples our core logic to a third-party library, breaking Clean Architecture.

#### Reason for Choosing
Converts the interface of the Kubernetes Java client to the interface expected by the domain layer (`ClusterRepository`).

#### ASCII Class Diagram
```text
+-------------------+
| ClusterRepository | <------------ Port Interface (Domain)
+-------------------+
          ^
          | (implements)
+---------------------------+
| KubernetesClientAdapter   | <---- Adapter Class (Infrastructure)
+---------------------------+
          | (uses)
          v
+-------------------+
|    CoreV1Api      | <------------ Third-Party Class (Kubernetes Client)
+-------------------+
```

---

### 3. Repository Pattern

#### Problem Solved
Separates the domain model logic from database access operations.

#### Reason for Choosing
Hides the PostgreSQL Hibernate/JPA plumbing behind a clean interface, allowing us to swap the database implementation or mock it in unit tests easily.

---

### 4. DTO (Data Transfer Object) Pattern

#### Problem Solved
Domain models and database entity schemas contain data fields (like passwords, metadata timestamps, internal ids) that should not be exposed directly to REST API responses.

#### Reason for Choosing
Decouples client request/response JSON mapping formats from database persistence definitions.
- *Trade-off*: Adds boilerplates like writing mapper classes (`MapStruct`), but improves security and maintenance flexibility.
