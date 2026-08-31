# Testing Rules
- Test class naming: `[Controller Name]MvcTest.java`.
- Use `@WebMvcTest` for controller tests; mock all dependencies with `@MockitoBean`.
- If a mocked controller test with RestTestClient is required, name it `<ControllerName>RTClientTest.java`.
- Use the Jackson 3 ObjectMapper for JSON serialization/deserialization in tests.
- Use the BDDMockito API in all new tests — `given(...).willReturn(...)` / `willThrow(...)` for stubbing and
  `then(mock).should()` / `should(never())` for verification. Do not use `Mockito.when(...).thenReturn(...)` or
  `Mockito.verify(...)`. Import statically from `org.mockito.BDDMockito`.
- Follow the GWT pattern, commenting each section:
  ```
  // Given
  // When
  // Then
  ```
- Test names: `should_[expected]_when_[condition]`.
- One assertion block per test.
- Never test private methods.

## RestTestClient tests

### Wiring — always this, never a hand-built client
```java
@WebMvcTest(XController.class)
@AutoConfigureRestTestClient
class XControllerRTClientTest {

    @MockitoBean
    private XService xService;

    @Autowired
    private RestTestClient restTestClient;
```
- `@WebMvcTest` alone does **not** give you a `RestTestClient` bean — in Boot 4.1 it is meta-annotated
  with `@AutoConfigureWebMvc` + `@AutoConfigureMockMvc` only. `@AutoConfigureRestTestClient` is what
  supplies the bean; `RestTestClientTestAutoConfiguration` then binds it to the slice's `MockMvc`, so
  it is still the mock web layer — no server, no port.
- Do **not** build the client in a `@BeforeEach` via `RestTestClient.bindTo(mockMvc)` or
  `bindToApplicationContext(wac)`. Both work, but the project standardised on the annotation: it is
  less code and drops the extra `MockMvc`/`WebApplicationContext` field.
- Name the field `restTestClient`.
- Imports: `org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient` and
  `org.springframework.test.web.servlet.client.RestTestClient`.

### API constraints — RestTestClient is not MockMvc
- `StatusAssertions` has only `isOk/isCreated/isAccepted/isNoContent/isBadRequest/isUnauthorized/isForbidden/isNotFound`
  plus the redirect family. There is **no** `isConflict()` and no 422 matcher:
  - 409 → `.expectStatus().isEqualTo(HttpStatus.CONFLICT)`
  - 422 → `.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)`
- `JsonPathAssertions` has no Hamcrest `Matcher` overload — `hasSize(n)` is unavailable.
  Assert sizes as `.jsonPath("$.length()").isEqualTo(n)` / `.jsonPath("$.items.length()").isEqualTo(n)`.
- Headers: `.expectHeader().valueEquals("Location", ...)` and
  `.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)`.
- Body values use `.jsonPath("$.field").isEqualTo(value)` / `.exists()` / `.doesNotExist()`.
- Requests still resolve against `http://localhost`, so `Location` assertions match the MockMvc ones.
