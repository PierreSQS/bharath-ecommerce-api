# ecommerce-api

## Stack
- Java 25
- Spring Boot 4.1.0
- Maven
- MySQL

## Official Spring Documentation
- For every implementation or code change, consult and follow the official Spring documentation, especially its recommended practices, APIs, configuration, and testing guidance.
- Use the [Spring Boot Documentation](https://docs.spring.io/spring-boot/index.html) as the primary reference for Spring Boot development.
- Use the [Spring Framework Testing Documentation](https://docs.spring.io/spring-framework/reference/testing.html) as the primary reference for testing.

## Package Structure
Base package: `com.bharath.ecommerce.api`

Sub-packages:
- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `exception`
- `config`

## Architecture Rules
- Always use DTOs in controllers — never expose entities directly.
- All business logic lives in the service layer — keep controllers thin.
- Centralised exception handling via `GlobalExceptionHandler`.
- All endpoints prefixed with `/api/v1/`.

## Lombok
- `@RequiredArgsConstructor` for injection everywhere (with `final` fields). No `@Autowired`.
- Entities: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
- DTOs: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.

## Exceptions
All handled in `GlobalExceptionHandler`, returning `ErrorResponse` JSON:

| Exception                    | HTTP Status |
|------------------------------|-------------|
| `ResourceNotFoundException`  | 404         |
| `BusinessRuleException`      | 422         |
| `DuplicateResourceException` | 409         |
| `AccessDeniedException`      | 403         |

## Database
- MySQL, database `ecommerce_db`.
- `ddl-auto=validate` — tables come from `schema.sql`, not Hibernate.

## Commands
- Run: `./mvnw spring-boot:run`
- Test: `./mvnw test`
- Compile check: `./mvnw clean compile`
