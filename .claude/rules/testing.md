# Testing Rules
- Test class naming: `[Controller Name]MvcTest.java`.
- Use `@WebMvcTest` for controller tests; mock all dependencies with `@MockitoBean`.
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
