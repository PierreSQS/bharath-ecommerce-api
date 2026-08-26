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
- Flyway owns the runtime schema: `V1__init_schema.sql` creates the `categories`, `products`, `customers`, `orders`, `order_items`, and `payments` tables and their indexes; `V2__populate_db.sql` seeds categories, products, and customers; `V3__add_reviews_table.sql` adds `reviews`.
- `reviews` enforces one review per customer per product (`uk_reviews_product_customer`) and a 1–5 `rating` (`ck_reviews_rating`). The unique key also indexes `product_id`, so there is no separate `idx_reviews_product`.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate validates entity mappings and does not create or update tables.
- `src/main/resources/db-scripts/schema.sql` is a standalone reset/setup script; it is not in Flyway's configured `classpath:db/migration` location.
- Open EntityManager in View is disabled. Hibernate SQL formatting, display, and highlighting are enabled.
- Docker Compose runs `mysql:9.5`, publishes container port `3306` on `${MYSQL_PORT:-3308}`, and persists data in `./mysql-files`.
- Compose expects `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD`; local values are supplied through the gitignored `.env` file.
- Spring Boot Docker Compose integration is skipped during tests by default. Tests that boot the full context must opt in per class: `@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")`. Without it the datasource cannot be resolved ("Failed to determine a suitable driver class"). `@WebMvcTest` slices do not need it.

## Commands
- Run: `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
- Test: `./mvnw test` (Windows: `.\mvnw.cmd test`)
- Compile check: `./mvnw clean compile` (Windows: `.\mvnw.cmd clean compile`)
- Start/stop MySQL directly: `docker compose up -d mysql` / `docker compose down`
- Build the image: `docker build -t ecommerce-api:local .`
- Run the full stack (MySQL + app): `docker compose up --build -d` / `docker compose down`
- The test suite contains one `@SpringBootTest` application-context smoke test (`EcommerceApiApplicationTests`), which requires Docker to be running; all other tests are `@WebMvcTest` slices or plain Mockito unit tests.

## Container Image
- `Dockerfile` is a multi-stage build: `maven:3.9.16-eclipse-temurin-25-alpine` builds the jar, `eclipse-temurin:25-jre-alpine` runs it.
- The fat jar is split into layers (`-Djarmode=tools ... extract --layers`) and started with `org.springframework.boot.loader.launch.JarLauncher`.
- Runs as the non-root `spring` user; `HEALTHCHECK` polls `/actuator/health` with BusyBox `wget`.
- The build stage skips tests — `EcommerceApiApplicationTests` starts Docker Compose, which cannot work inside a build stage.
- The app has no `spring.datasource.*` config; outside the compose integration the datasource **must** be injected: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, plus `SPRING_DOCKER_COMPOSE_ENABLED=false`.
- The `app` service in `compose.yaml` supplies those and waits on the `mysql` healthcheck; it publishes `${APP_PORT:-8080}`.
- Flyway still migrates at application startup inside the container.

## CI
- `.github/workflows/ci.yml` runs on push and pull request to `main`.
- `test` job: Temurin JDK 25 plus a `mysql:9.5` service container reachable at `127.0.0.1:3306`, database `ecommerce_db`. Flyway migrates it when the context smoke test boots.
- CI does **not** use Docker Compose. `EcommerceApiApplicationTests` pins `spring.docker.compose.skip.in-tests=false`, and inlined `@SpringBootTest(properties = ...)` cannot be overridden by an environment variable. Setting `SPRING_DOCKER_COMPOSE_ENABLED=false` disables the integration before `skip.in-tests` is consulted, so CI injects `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` instead.
- `build-and-push` job: `needs: test`, and only on pushes to `main`. Builds the `Dockerfile` and pushes `ghcr.io/pierresqs/bharath-ecommerce-api:latest` and `:sha-<sha>` using `GITHUB_TOKEN` with `packages: write`. The image is named after the repository (`github.repository`), not the Maven artifact.
- A newly created GHCR package is private by default; making it public is a one-time action in the repository's Packages settings.
