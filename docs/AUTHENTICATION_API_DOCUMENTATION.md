# Authentication API Documentation

## Overview

The Authentication API provides a complete JWT-based authentication system for the game server. It supports user registration, login, token introspection, token refresh, and logout functionality. The system uses HMAC-SHA256 (HS256) for JWT signing and BCrypt for password hashing.

### Base URL
```
${api.prefix}/auth
```

**Note:** The `api.prefix` is configured in `application.properties` and defaults to `/api/v1`.

---

## Authentication Flow

1. **Registration**: User creates an account with username and password
2. **Login**: User authenticates and receives a JWT token
3. **Token Usage**: Client includes JWT token in Authorization header for protected endpoints
4. **Token Refresh**: Client can refresh expired tokens
5. **Logout**: Client can invalidate tokens
6. **Token Introspection**: Verify token validity without using it

---

## 1. User Registration

### Endpoint
```
POST /auth/register
```

### Description
Creates a new user account with the provided username and password. The password is automatically hashed using BCrypt before storage.

### Request Body
```json
{
  "username": "string",
  "password": "string"
}
```

#### Parameters
| Parameter | Type | Required | Description | Validation Rules |
|-----------|------|----------|-------------|------------------|
| username | String | ✅ | Username for the account | 3-50 characters, unique |
| password | String | ✅ | Password for the account | Minimum 8 characters |

### Response Success (201 Created)
```json
{
  "status": 201,
  "message": "User created successfully",
  "data": {
    "id": "user-uuid-123"
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response Error (409 Conflict)
```json
{
  "status": 409,
  "message": "User with username example already exists",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response Error (400 Bad Request)
```json
{
  "status": 400,
  "message": "Username must be between 3 and 50 characters",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 2. User Login

### Endpoint
```
POST /auth/login
```

### Description
Authenticates a user with username and password, returning a JWT token upon successful authentication.

### Request Body
```json
{
  "username": "string",
  "password": "string"
}
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| username | String | ✅ | Username for authentication |
| password | String | ✅ | Password for authentication |

### Response Success (200 OK)
```json
{
  "status": 200,
  "message": "Authentication successful",
  "data": {
    "id": "user-uuid-123",
    "username": "example_user",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response Error (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Username does not exist",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "status": 401,
  "message": "Incorrect password",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 3. Token Introspection

### Endpoint
```
POST /auth/introspect
```

### Description
Validates a JWT token without consuming it. Useful for checking token validity before making authenticated requests.

### Request Body
```json
{
  "token": "string"
}
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | String | ✅ | JWT token to validate |

### Response Success (200 OK)
```json
{
  "status": 200,
  "message": "Token introspection successful",
  "data": {
    "valid": true
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response Error (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Token is invalid or expired",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "status": 401,
  "message": "Token has been invalidated",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 4. Token Refresh

### Endpoint
```
POST /auth/refresh
```

### Description
Refreshes an expired JWT token. The old token is invalidated and a new token is issued with updated expiration time.

### Request Body
```json
{
  "token": "string"
}
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | String | ✅ | Expired JWT token to refresh |

### Response Success (200 OK)
```json
{
  "status": 200,
  "message": "Token refreshed successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response Error (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Token is invalid or expired",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "status": 401,
  "message": "Token has been invalidated",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 5. User Logout

### Endpoint
```
POST /auth/logout
```

### Description
Invalidates a JWT token by adding it to the invalidated tokens repository. The token cannot be used for subsequent requests.

### Request Body
```json
{
  "token": "string"
}
```

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | String | ✅ | JWT token to invalidate |

### Response Success (200 OK)
```json
{
  "status": 200,
  "message": "Logout successful",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response Error (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Token is invalid or expired",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## JWT Token Structure

### Header
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### Payload
```json
{
  "sub": "user-uuid-123",
  "iss": "myapp.example.com",
  "iat": 1704067200,
  "jti": "token-uuid-456",
  "scope": "USER",
  "exp": 1704070800
}
```

#### Claims
| Claim | Type | Description |
|-------|------|-------------|
| sub | String | Subject (User ID) |
| iss | String | Issuer |
| iat | Number | Issued At (timestamp) |
| jti | String | JWT ID (unique token identifier) |
| scope | String | User scope/role |
| exp | Number | Expiration Time (timestamp) |

---

## Using JWT Tokens

### Authorization Header
Include the JWT token in the Authorization header for protected endpoints:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Protected Endpoints
The following endpoints require authentication:
- `/api/matchmaking/queue/**` - Matchmaking operations

### WebSocket Authentication
For WebSocket connections, include the token as a query parameter:

```
ws://localhost:8386/ws?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Configuration

### Environment Variables
The following environment variables must be set:

| Variable | Description | Example |
|----------|-------------|---------|
| JWT_SECRET | Secret key for JWT signing | `your-secret-key-here` |
| JWT_VALID_DURATION | Token validity duration in seconds | `3600` (1 hour) |
| JWT_REFRESH_DURATION | Token refresh window in seconds | `86400` (24 hours) |

### Application Properties
```properties
jwt.secret=${JWT_SECRET}
jwt.valid-duration=${JWT_VALID_DURATION}
jwt.refresh-duration=${JWT_REFRESH_DURATION}
```

---

## Security Features

### Password Security
- **BCrypt Hashing**: All passwords are hashed using BCrypt with default strength
- **Minimum Length**: Passwords must be at least 8 characters long
- **No Plain Text Storage**: Passwords are never stored in plain text

### Token Security
- **HMAC-SHA256**: JWT tokens are signed using HMAC-SHA256
- **Token Invalidation**: Logged out tokens are stored in MongoDB and checked on each request
- **Expiration**: Tokens have configurable expiration times
- **Unique JTI**: Each token has a unique identifier for tracking

### Authentication Flow
- **Stateless**: JWT tokens are stateless and don't require server-side sessions
- **Refresh Mechanism**: Expired tokens can be refreshed without re-authentication
- **Introspection**: Token validity can be checked without consuming the token

---

## Error Handling

### Common Error Responses

#### 400 Bad Request
- Invalid request body
- Validation errors (username/password length, format)

#### 401 Unauthorized
- Invalid or expired JWT token
- Token has been invalidated
- Incorrect username/password

#### 409 Conflict
- Username already exists during registration

#### 500 Internal Server Error
- Database connection issues
- JWT signing/verification errors

### Error Response Format
```json
{
  "status": 400,
  "message": "Detailed error message",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## Rate Limiting & Security Considerations

### Recommendations
1. **Rate Limiting**: Implement rate limiting on authentication endpoints
2. **HTTPS**: Always use HTTPS in production
3. **Token Storage**: Store tokens securely on the client side
4. **Token Rotation**: Consider implementing token rotation for enhanced security
5. **Audit Logging**: Log authentication events for security monitoring

### Best Practices
- Use strong, unique JWT secrets
- Set appropriate token expiration times
- Implement proper error handling
- Validate all input data
- Use HTTPS for all API communications
- Regularly rotate JWT secrets

---

## Example Usage

### Complete Authentication Flow

1. **Register a new user:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "password123"}'
```

2. **Login and get token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "password123"}'
```

3. **Use token for authenticated requests:**
```bash
curl -X POST http://localhost:8080/api/v1/matchmaking/queue \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

4. **Refresh token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"token": "expired-token-here"}'
```

5. **Logout:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"token": "token-to-invalidate"}'
``` 