# SevaSetu Backend API Documentation

Base URL (local): `http://localhost:3000`

## Authentication
This backend uses email OTP authentication.

### 1. Send OTP
- **Method:** `POST`
- **Path:** `/auth/send-otp`
- **Description:** Generates a 6-digit OTP, stores only hashed OTP with expiry, and sends OTP to email.

Request body:
```json
{
  "email": "user@example.com"
}
```

Success response (`200`):
```json
{
  "message": "OTP sent"
}
```

Possible error responses:
- `400`: Invalid email
- `500`: Email service not configured / failed to send OTP

---

### 2. Verify OTP (Login/Register)
- **Method:** `POST`
- **Path:** `/auth/verify-otp`
- **Description:** Verifies OTP and returns JWT token. Creates user if email is new.

Request body:
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

Success response (`200`):
```json
{
  "token": "<jwt_token>",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "createdAt": "2026-04-20T00:00:00.000Z"
  }
}
```

Possible error responses:
- `400`: No OTP found / OTP expired / Invalid OTP / Invalid input
- `500`: JWT secret missing / verification failed

---

## Protected Route Auth Header
For protected APIs, send JWT in Authorization header:

```http
Authorization: Bearer <jwt_token>
```

Middleware behavior:
- Missing token: `401 Unauthorized`
- Invalid/expired token: `403 Forbidden`

---

## Utility Endpoints

### Health Check
- **Method:** `GET`
- **Path:** `/health`
- **Response:**
```json
{
  "status": "OK",
  "message": "Server is running"
}
```

### Root
- **Method:** `GET`
- **Path:** `/`
- **Response:**
```json
{
  "message": "Welcome to SevaSetu Backend API"
}
```

---

## Environment Variables
Required environment variables:

- `PORT` (default: `3000`)
- `DATABASE_URL`
- `JWT_SECRET`
- `JWT_EXPIRY` (default fallback in code: `7d`)
- `EMAIL_USER`
- `EMAIL_PASS`

---

## Android Integration Notes
- Save the `token` securely on device (for example, EncryptedSharedPreferences / Keystore-backed storage).
- Attach `Authorization: Bearer <token>` on every protected API request.
- OTP is valid for 5 minutes.
- OTP is never stored in plaintext on backend.
