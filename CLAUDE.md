# ecommerce-api

## Stack
- Java 25
- Spring Boot 4.1.0
- Maven 3.9.16 (Maven Wrapper)
- MySQL 9.5 (Docker Compose)
- Spring MVC, Spring Data JPA, Bean Validation, Actuator, Flyway, and Lombok

## Official Spring Documentation
- For every implementation or code change, consult and follow exclusively the official Spring documentation, 
  especially its recommended practices, APIs, configuration, and testing guidance.
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

## API Documentation
- `springdoc-openapi-starter-webmvc-ui` (3.x, the Spring Boot 4 line) exposes the spec at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`.
- `config/OpenApiConfig` supplies the `OpenAPI` metadata bean (title, version, Markdown description). It deliberately declares no `servers`, so springdoc derives the server URL from the request and Swagger UI works locally, in Docker, and behind a proxy.
- Everything else is left on springdoc defaults — controllers, DTOs, and Bean Validation constraints are scanned automatically.

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
- Build and run the whole stack (MySQL + API) in Docker: `docker compose up --build` (add `-d` to detach). `--build` is required after any source change, otherwise Compose reuses the previously built `api` image.
- Build the API image only: `docker compose build api` (or `docker build -t ecommerce-api .`)
- The current test suite contains one `@SpringBootTest` application-context smoke test.

## Subagents
- `.claude/agents/resttestclient-controller-test-writer.md` is the only active subagent. It writes
  `RestTestClient` controller tests (`<ControllerName>RTClientTest.java`) for one or more controllers
  and verifies them by running only the classes it created. It never writes the MockMvc variant and
  never touches `src/main/`. For several controllers, launch one instance per controller in parallel
  in a single message.
- `.claude/agents-disabled/spring-controller-tester.md` is kept for reference only. Claude Code
  discovers agents from `.claude/agents/*.md`, so a file outside that directory is never loaded and
  the agent cannot be invoked. Do not assume it is available.
- Its content is still a useful written record of the controller-testing conventions (Boot 4 import
  paths, BDDMockito usage, the Spring 7 status-code gotchas). The authoritative rules remain
  `.claude/rules/testing.md` and `.claude/rules/api-design.md`.
- To re-enable it: `git mv .claude/agents-disabled/spring-controller-tester.md .claude/agents/`.

## Commit Messages

Follow Conventional Commits:

- feat: add saved jobs count to navbar
- fix: correct role guard on employer routes
- docs: update README with localStorage keys
- chore: upgrade react-router to v7.8
- refactor: extract job card into reusable component
- style: fix spacing on mobile job list
- build: update or add build scripts or dependencies
- 
