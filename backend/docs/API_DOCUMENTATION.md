# SevaSetu Backend API Documentation

Base URL (local): `http://localhost:3000`

## Authentication
This backend uses email OTP authentication.

### 1. Register User (Complete Onboarding)
- **Method:** `POST`
- **Path:** `/auth/register/onboarding`
- **Description:** Complete user registration with all profile information. No JWT required. All fields are mandatory.

Request body:
```json
{
  "fullName": "Rahul Sharma",
  "email": "rahul@example.com",
  "phoneNumber": "9876543210",
  "gender": "Male",
  "idType": "Aadhaar Card",
  "idNumber": "123456789012",
  "address": "House No. 123, Arera Colony",
  "city": "Bhopal",
  "pinCode": "462001"
}
```

Success response (`200`):
```json
{
  "message": "Registration completed successfully. Please verify OTP to login",
  "user": {
    "id": "uuid",
    "name": "Rahul Sharma",
    "email": "rahul@example.com",
    "phone": "9876543210",
    "gender": "Male",
    "idType": "Aadhaar Card",
    "idNumber": "123456789012",
    "addressText": "House No. 123, Arera Colony",
    "addressCityOrPanchayat": "Bhopal",
    "pinCode": "462001"
  },
  "registrationStatus": {
    "onboardingCompleted": true,
    "profileCompleted": false,
    "locationCaptured": false
  }
}
```

Possible error responses:
- `400`: Missing required fields or invalid format
- `409`: User already exists. Please login

---

### 2. Send OTP
- **Method:** `POST`
- **Path:** `/auth/send-otp`
- **Description:** Generates a 6-digit OTP for an already-registered email, stores only hashed OTP with expiry, and sends OTP to email.

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
- `404`: User not found (register first)
- `500`: Email service not configured / failed to send OTP

---

### 3. Verify OTP (Login)
- **Method:** `POST`
- **Path:** `/auth/verify-otp`
- **Description:** Verifies OTP and returns JWT token for a registered user.

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
  },
  "registrationStatus": {
    "onboardingCompleted": false,
    "profileCompleted": false,
    "locationCaptured": false
  }
}
```

Possible error responses:
- `400`: No OTP found / OTP expired / Invalid OTP / Invalid input
- `404`: User not found (register first)
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
