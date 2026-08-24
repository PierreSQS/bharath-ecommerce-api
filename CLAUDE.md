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
- Compose expects `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD`; local values are supplied through the gitignored `.env` file.
- Spring Boot Docker Compose integration is enabled during tests (`spring.docker.compose.skip.in-tests=false`).

## Commands
- Run: `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
- Test: `./mvnw test` (Windows: `.\mvnw.cmd test`)
- Compile check: `./mvnw clean compile` (Windows: `.\mvnw.cmd clean compile`)
- Start/stop MySQL directly: `docker compose up -d mysql` / `docker compose down`
- Build the image: `docker build -t ecommerce-api:local .`
- Run the full stack (MySQL + app): `docker compose up --build -d` / `docker compose down`
- The current test suite contains one `@SpringBootTest` application-context smoke test.

## Container Image
- `Dockerfile` is a multi-stage build: `maven:3.9.16-eclipse-temurin-25-alpine` builds the jar, `eclipse-temurin:25-jre-alpine` runs it.
- The fat jar is split into layers (`-Djarmode=tools ... extract --layers`) and started with `org.springframework.boot.loader.launch.JarLauncher`.
- Runs as the non-root `spring` user; `HEALTHCHECK` polls `/actuator/health` with BusyBox `wget`.
- The build stage skips tests — tests start Docker Compose (`spring.docker.compose.skip.in-tests=false`), which cannot work inside a build stage.
- The app has no `spring.datasource.*` config; outside the compose integration the datasource **must** be injected: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, plus `SPRING_DOCKER_COMPOSE_ENABLED=false`.
- The `app` service in `compose.yaml` supplies those and waits on the `mysql` healthcheck; it publishes `${APP_PORT:-8080}`.
- Flyway still migrates at application startup inside the container.
