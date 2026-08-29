# ecommerce-api

## Stack
- Java 25
- Spring Boot 4.1.0
- Maven 3.9.16 (Maven Wrapper)
- MySQL 9.5 (Docker Compose)
- Spring MVC, Spring Data JPA, Bean Validation, Actuator, Flyway, and Lombok

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
- Flyway owns the runtime schema: `V1__init_schema.sql` creates the `categories`, `products`, `customers`, `orders`, `order_items`, and `payments` tables and their indexes; `V2__populate_db.sql` seeds categories, products, and customers.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate validates entity mappings and does not create or update tables.
- `src/main/resources/db-scripts/schema.sql` is a standalone reset/setup script; it is not in Flyway's configured `classpath:db/migration` location.
- Open EntityManager in View is disabled. Hibernate SQL formatting, display, and highlighting are enabled.
- Docker Compose runs `mysql:9.5`, publishes container port `3306` on `${MYSQL_PORT:-3308}`, and persists data in `./mysql-files`.
- Compose also builds the `api` service from the root `Dockerfile`, publishes it on `${API_PORT:-8080}`, waits for the MySQL healthcheck (`depends_on: condition: service_healthy`), and disables the Docker Compose integration inside the container (`SPRING_DOCKER_COMPOSE_ENABLED=false`).
- Compose expects `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD`, plus the optional `MYSQL_PORT`/`API_PORT`; local values are supplied through the gitignored `.env` file.
- Spring Boot Docker Compose integration is enabled during tests (`spring.docker.compose.skip.in-tests=false`).

## Commands
- Run: `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
- Test: `./mvnw test` (Windows: `.\mvnw.cmd test`)
- Compile check: `./mvnw clean compile` (Windows: `.\mvnw.cmd clean compile`)
- Start/stop MySQL directly: `docker compose up -d` / `docker compose down`
- The current test suite contains one `@SpringBootTest` application-context smoke test.

## Commit Messages

Follow Conventional Commits:

- feat: add saved jobs count to navbar
- fix: correct role guard on employer routes
- docs: update README with localStorage keys
- chore: upgrade react-router to v7.8
- refactor: extract job card into reusable component
- style: fix spacing on mobile job list
- build: update or add build scripts or dependencies
