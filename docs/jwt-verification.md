## Verifying user tokens (all services)

The users service issues the tokens. Every other service **verifies them itself**, without
calling the users service.

| | |
|---|---|
| Header | `Authorization: Bearer <accessToken>` |
| Algorithm | HS256 |
| Lifetime | 15 minutes |
| Claims | `sub` = user id (UUID string), `email`, `iat`, `exp` |
| Bad, missing or expired token | 401 |

**Always use `sub` to find the caller's data. Never trust a user id sent by the client.**

### 1. Dependencies (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. Config (`application.yml`)

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:dev-only-secret-change-me-32-bytes-minimum}
```
The secret must be **identical in every service** and at least 32 bytes. The fallback is for
local development only. Anywhere shared or deployed, set `JWT_SECRET` and never commit it.

### 3. Security config

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes but was " + length);
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
```
Add `.requestMatchers(...).permitAll()` before `anyRequest()` for any public endpoints.

### 4. Finding out who is calling

```java
@GetMapping("/transactions")
public List<TransactionResponse> mine(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return service.findByUserId(userId);
}
```
### 5. Calling another service on the user's behalf

A service that calls another service for a user (the dashboard calling transactions and
portfolio) must **forward the caller's `Authorization` header unchanged**, so the other service
can identify the user from the same token. Do not call internal endpoints without it: they will
answer 401.

```java
@GetMapping("/dashboard")
public DashboardResponse dashboard(@RequestHeader("Authorization") String authorization) {
    // pass 'authorization' on to the transactions and portfolio calls
}
```
### 6. Testing

In `@WebMvcTest` tests, import your `SecurityConfig` and send a pretend token:

```java
mvc.perform(get("/transactions")
        .with(jwt().jwt(t -> t.subject("11111111-1111-1111-1111-111111111111"))))
    .andExpect(status().isOk());
```