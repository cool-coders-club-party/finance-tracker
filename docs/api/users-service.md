# Users API Documentation

**Owner:** Dave Swords  
**Service:** `users-service`  
**Local URL:** `http://localhost:8081`

The users service registers accounts, logs users in and issues the JWT access tokens and refresh tokens that every other service relies on. It is the only service that handles passwords.

All endpoints are under `/api/auth`. Request and response bodies are JSON.

|Method|Path|Auth|Purpose|
|---|---|---|---|
|`GET`|`/api/auth/ping`|none|Liveness check|
|`POST`|`/api/auth/register`|none|Create an account|
|`POST`|`/api/auth/login`|none|Log in and receive tokens|
|`GET`|`/api/auth/me`|Bearer access token|Identify the caller|
|`POST`|`/api/auth/refresh`|none|Swap a refresh token for new tokens|
|`POST`|`/api/auth/logout`|none|Revoke a refresh token|

`refresh` and `logout` need no `Authorization` header, because the access token may already have expired. The refresh token in the body is the credential.

## Register

```http
POST /api/auth/register
```

```json
{
  "email": "sam@example.com",
  "password": "Sup3rSecret"
}
```

- `email` — required, must be a valid email address.
- `password` — required, 8 to 72 characters. Stored only as a BCrypt hash.

### Success response

```http
201 Created
```

```json
{
  "id": "3f2b8c1e-5a7d-4e0b-9c64-1d2f7a9b0e11",
  "email": "sam@example.com"
}
```

Registering does not log the user in. Call `login` to receive tokens.

### Validation failure — `400 Bad Request`

Returned when `email` is missing or malformed, or `password` is missing or outside 8 to 72 characters. This is Spring's standard validation response.

### Email already registered — `409 Conflict`

```json
{
  "status": 409,
  "message": "An account already exists for sam@example.com."
}
```

## Log in

```http
POST /api/auth/login
```

```json
{
  "email": "sam@example.com",
  "password": "Sup3rSecret"
}
```

### Success response

```http
200 OK
```

```json
{
  "id": "3f2b8c1e-5a7d-4e0b-9c64-1d2f7a9b0e11",
  "email": "sam@example.com",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "q8Zk3...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- `accessToken` — a signed JWT. Send it as `Authorization: Bearer <accessToken>` to every other service.
- `refreshToken` — an opaque random value used only with `refresh` and `logout`. Never send it to other endpoints.
- `tokenType` — always `Bearer`.
- `expiresIn` — access token lifetime in seconds (15 minutes).

### Wrong credentials — `401 Unauthorized`

```json
{
  "status": 401,
  "message": "Invalid email or password"
}
```

The same response is returned for an unknown email and a wrong password, so a caller cannot tell which accounts exist.

A malformed body returns `400 Bad Request`.

## Get the current user

```http
GET /api/auth/me
Authorization: Bearer <accessToken>
```

### Success response

```http
200 OK
```

```json
{
  "id": "3f2b8c1e-5a7d-4e0b-9c64-1d2f7a9b0e11",
  "email": "sam@example.com"
}
```

A missing, expired or tampered token returns `401 Unauthorized`.

## Refresh tokens

```http
POST /api/auth/refresh
```

```json
{
  "refreshToken": "q8Zk3..."
}
```

### Success response

```http
200 OK
```

The body has the same shape as the login response, with a **new** access token and a **new** refresh token. The client must store the new refresh token and discard the old one.

Each refresh token works once. Presenting a used token again returns 401.

### Invalid refresh token — `401 Unauthorized`

```json
{
  "status": 401,
  "message": "Invalid refresh token"
}
```

The message is the same whether the token is unknown, expired or already used, so a caller cannot tell whether it ever existed.

## Log out

```http
POST /api/auth/logout
```

```json
{
  "refreshToken": "q8Zk3..."
}
```

```http
204 No Content
```

Revokes the refresh token. The response is the same whether or not the token existed. Access tokens already issued stay valid until they expire (15 minutes at most).

## Tokens

### Access token

| Property | Value                                               |
| -------- | --------------------------------------------------- |
| Format   | JWT, signed with HS256                              |
| Lifetime | 15 minutes (`app.jwt.expiry-minutes`)               |
| Claims   | `sub` (user id, UUID string), `email`, `iat`, `exp` |

Other services verify the signature and expiry themselves, without calling the users service, and use `sub` to find the caller's data. They never trust a user id sent by the client. See [JWT verification](https://claude.ai/jwt-verification.md).

### Refresh token

|Property|Value|
|---|---|
|Format|Random 256-bit value|
|Lifetime|7 days (`app.jwt.refresh-expiry-days`)|
|Storage|Only a SHA-256 hash is stored, in the `refresh_tokens` table|

## Current implementation

The service follows clean architecture: `domain` (no Spring), `application` (use cases and ports), `infrastructure` (JPA, BCrypt, JWT, Spring Security) and `api` (controllers and DTOs). Dependencies point inward.

Users and refresh tokens are stored in PostgreSQL through Spring Data JPA (`users` and `refresh_tokens` tables, unique email). Data survives a backend restart. The service is stateless: no HTTP session, no cookies, and CSRF protection is disabled.

Refresh tokens rotate: a refresh deletes the old token's row and creates a new one. Reuse detection (revoking all of a user's tokens when a used token returns) is not implemented.

## Service responsibilities

The users service owns accounts, credentials and token issuing.

Every other service owns its own data and identifies the caller only by the `sub` claim of the access token.

---

## Run locally

Requirements: Java 21 and Docker.

Start each command from the repository root.

### Database

```bash
# Start the users database (PostgreSQL on port 5435).
docker compose up -d users-postgres
```

### Backend

```bash
# Enter the backend folder.
cd backend

# The signing secret must be at least 32 bytes. Every service must use the same one.
export JWT_SECRET="replace-with-a-secret-of-at-least-32-bytes"

# Start the users service.
./mvnw -pl services/users-service spring-boot:run
```

The service runs on `http://localhost:8081`.

Stop the database with `docker compose stop users-postgres`. Data is kept in the `users-db-data` volume.

## Demonstration checklist

### Registration

1. Register a valid user. The response is `201` with the user's `id` and `email`.
2. Register the same email again. The response is `409`.
3. Register with a 3-character password. The response is `400`.

```bash
# Register.
curl -i -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"sam@example.com","password":"Sup3rSecret"}'
```

### Persistence

Check the database directly:

```bash
# The password column holds a BCrypt hash, never the raw password.
docker exec -it finance-tracker-users-postgres-1 psql -U finance -d users \
  -c "SELECT id, email, left(password_hash, 10) FROM users;"
```

Restart the backend and log in again. The user is still there.

### Login and `/me`

1. Log in with the correct password. The response is `200` with both tokens.
2. Log in with a wrong password, then with an unknown email. Both return `401` with the same message.
3. Call `/api/auth/me` with the access token. It returns the user's `id` and `email`.
4. Call `/api/auth/me` with no token. It returns `401`.

### Refresh and logout

```bash
# Log in and capture the refresh token.
LOGIN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sam@example.com","password":"Sup3rSecret"}')
OLD=$(echo "$LOGIN" | sed 's/.*"refreshToken":"\([^"]*\)".*/\1/')
```

1. Refresh with `$OLD`. The response is `200` with a different `refreshToken`.
2. Refresh with `$OLD` again. The response is `401`, because rotation consumed it.
3. Log out with the new refresh token. The response is `204`.
4. Refresh with the logged-out token. The response is `401`.

The `refresh_tokens` table holds hashes and expiry dates about 7 days out, and no raw tokens.

### Tests

From the backend folder:

```bash
# Run the users service tests.
./mvnw -pl services/users-service test
```