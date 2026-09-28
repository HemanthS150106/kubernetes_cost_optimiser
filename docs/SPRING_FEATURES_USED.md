# Spring Features Utilized - Kubernetes Cost Optimizer

This document provides a technical drilldown into the Spring Boot and Spring Framework features implemented in this repository.

---

## 1. Dependency Injection via Constructor Injection

### Purpose
IoC (Inversion of Control) container feature to pass dependencies into dependent classes, decoupled from class instantiation.

### Why Chosen & Alternatives
Constructor injection is chosen over `@Autowired` field injection because:
1. It guarantees immutability of fields (`final`).
2. It prevents circular dependencies at compile-time.
3. Decouples class from Spring context, allowing easy unit testing with Mockito.
- *Alternative*: Field injection (simple to write but hard to test and allows null states) or Setter injection.

### Where Used
Used in all application services and REST controllers (e.g. `ClusterController.java`, `AuthUseCase.java`).

### Example Snippet
```java
@Service
@RequiredArgsConstructor // Lombok generates the constructor containing final fields
public class AuthUseCase {
    private final SpringDataUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // Injectable dependencies
}
```

### Lifecycle & Mechanics
During application startup, Spring scans beans, creates dependencies first, passes them to the constructors, and registers the instantiated bean into the ApplicationContext.

### Common Interview Questions
- *Why is constructor injection preferred over field injection?* Field injection makes class tightly coupled with Spring, fields cannot be final, and can hide code smells like violating Single Responsibility Principle.

---

## 2. Spring Stereotype Annotations (`@Service`, `@Repository`, `@Component`)

### Purpose
Classes annotated with stereotypes are automatically scanned and registered as Spring-managed beans inside the ApplicationContext.

### Alternatives
Explicit `@Bean` definitions inside a `@Configuration` class. Stereotypes are chosen because they add semantic clarity (JPA vs Business logic vs Utility helper).

### Example Snippet
```java
@Component
@RequiredArgsConstructor
public class RecommendationRepositoryAdapter implements RecommendationRepository { ... }
```

### Common Mistakes
- Annotating interfaces rather than concrete implementations.
- Missing `@ComponentScan` scope matching the annotated package.

---

## 3. Spring Scheduling (`@Scheduled`)

### Purpose
Runs task execution in a background thread on a recurring schedule.

### Why Chosen
Enables automatic periodic scans of Kubernetes cluster utilization metrics without blocking user threads.
- *Alternative*: Enterprise schedulers like Quartz or Kubernetes CronJobs. `@Scheduled` was chosen because it is simple to implement and doesn't require extra operational setups.

### Where Used
In `ClusterAnalysisUseCase.java` for scanning the cluster status.

### Example Snippet
```java
@Scheduled(fixedDelay = 300000) // Executes every 5 minutes
@Transactional
public void runScheduledAnalysis() { ... }
```

---

## 4. `@ControllerAdvice` and `@ExceptionHandler`

### Purpose
Centralizes error handling logic across all REST API controllers, returning standard JSON payloads on failures.

### Why Chosen
Decouples controllers from try-catch blocks and guarantees that exceptions are always mapped to proper HTTP status codes.
- *Alternative*: Try-catch blocks inside each REST method.

### Where Used
In `GlobalExceptionHandler.java`.

### Example Snippet
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        // Build JSON body with status code 400
    }
}
```

---

## 5. Spring Data JPA Repositories

### Purpose
Provides standard CRUD methods out-of-the-box and generates database queries automatically from method naming conventions.

### Where Used
In `SpringDataRecommendationRepository.java`.

### Example Snippet
```java
public interface SpringDataRecommendationRepository extends JpaRepository<RecommendationEntity, Long> {
    List<RecommendationEntity> findByStatus(RecommendationStatus status);
}
```

---

## 6. Spring Security with JWT Filter

### Purpose
Secures REST resources statelessly by checking JWT tokens on incoming requests.

### Where Used
In `SecurityConfig.java` and `JwtAuthenticationFilter.java`.

### Example Snippet
```java
http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
```

---

## 7. Method Parameter Validation (`@Valid`)

### Purpose
Performs automatic JSR-380 validation checks on API request body schemas.

### Example Snippet
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) { ... }
```
