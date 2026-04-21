# SevaSetu Backend API Documentation

Base URL (local): `http://localhost:3000`

## Authentication
This backend uses email OTP authentication.

### 1. Register User
- **Method:** `POST`
- **Path:** `/auth/register/onboarding`
- **Description:** Registers a user before OTP login. No JWT required.

Request body:
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "phone": "9876543210"
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
    "phone": "9876543210"
  },
  "registrationStatus": {
    "onboardingCompleted": true,
    "profileCompleted": false,
    "locationCaptured": false
  }
}
```

Possible error responses:
- `400`: Invalid name/email/phone
- `409`: User already exists

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

### 4. Complete Profile
- **Method:** `POST`
- **Path:** `/auth/register/profile`
- **Auth:** Bearer token required
- **Description:** Saves structured address and Aadhaar details. Coordinates are optional; if sent, both latitude and longitude must be sent and valid.

Request body:
```json
{
  "district": "Bhopal",
  "areaType": "URBAN",
  "cityOrPanchayat": "Bhopal",
  "ward": "Ward 12",
  "locality": "Arera Colony",
  "landmark": "Near Hanuman Mandir",
  "fullAddress": "Ward 12, Arera Colony, Bhopal, MP",
  "aadhaarNumber": "123412341234",
  "latitude": 23.233,
  "longitude": 77.433,
  "jurisdictionId": "optional-jurisdiction-uuid"
}
```

Success response (`200`):
```json
{
  "message": "Profile completed successfully",
  "user": {
    "id": "uuid",
    "email": "rahul@example.com",
    "aadhaarNumber": "123412341234",
    "addressDistrict": "Bhopal",
    "addressAreaType": "URBAN",
    "addressCityOrPanchayat": "Bhopal",
    "addressWard": "Ward 12",
    "addressLocality": "Arera Colony",
    "addressLandmark": "Near Hanuman Mandir",
    "addressText": "Ward 12, Arera Colony, Bhopal, MP",
    "addressLat": 23.233,
    "addressLng": 77.433
  },
  "registrationStatus": {
    "onboardingCompleted": true,
    "profileCompleted": true,
    "locationCaptured": true
  }
}
```

Possible error responses:
- `400`: Missing required profile fields / invalid Aadhaar / invalid coordinate input
- `401/403`: Missing or invalid token

Coordinate rules:
- Send both `latitude` and `longitude` together, or omit both.
- If omitted, profile is still considered complete.
- If omitted, `locationCaptured` will be `false`.

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
