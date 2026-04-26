# SevaSetu Backend API Documentation

Base URL (local): `https://sevasetu-zqa6.onrender.com`

## Authentication
This backend uses email OTP authentication.

### 1. Register User (Complete Onboarding)
- **Method:** `POST`
- **Path:** `/auth/register/onboarding`
- **Description:** Complete user registration with all profile information and jurisdiction hierarchy. No JWT required. All mandatory fields must be provided.

#### Request Fields

**Personal Information (Required):**
- `name` (string): User's full name
- `email` (string): User's email (must be unique)
- `phone` (string): 10-digit phone number
- `gender` (string): "Male", "Female", or "Other"

**Identity Verification (Required):**
- `idType` (string): "Aadhaar Card", "PAN Card", or "Voter ID"
- `idNumber` (string): ID document number

**Jurisdiction Hierarchy (Required):**
- `addressDistrict` (string): District UUID
- `addressAreaType` (enum): "URBAN" or "RURAL"
- `jurisdictionId` (string): Ward UUID (for URBAN) or Panchayat UUID (for RURAL)

**For URBAN Areas (Required if addressAreaType is "URBAN"):**
- `addressCity` (string): City UUID
- `addressWard` (string): Ward UUID

**For RURAL Areas (Required if addressAreaType is "RURAL"):**
- `addressBlock` (string): Block UUID
- `addressPanchayat` (string): Panchayat UUID

**Address Details (Required):**
- `addressText` (string): Full address description
- `pinCode` (string): 6-digit postal code

**Address Details (Optional):**
- `addressLocality` (string): Locality/neighborhood name
- `addressLandmark` (string): Nearby landmark

#### Example - Urban Registration
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "phone": "9876543210",
  "gender": "Male",
  "idType": "Aadhaar Card",
  "idNumber": "123456789012",
  "addressDistrict": "20000001-0000-0000-0000-000000000000",
  "addressAreaType": "URBAN",
  "addressCity": "20000001-1000-0000-0000-000000000000",
  "addressWard": "20000001-1110-0000-0000-000000000000",
  "jurisdictionId": "20000001-1110-0000-0000-000000000000",
  "addressLocality": "Hall Bazaar",
  "addressLandmark": "Near Temple",
  "addressText": "House No. 123, Arera Colony, Amritsar",
  "pinCode": "143001"
}
```

#### Example - Rural Registration
```json
{
  "name": "Priya Singh",
  "email": "priya@example.com",
  "phone": "9876543211",
  "gender": "Female",
  "idType": "Voter ID",
  "idNumber": "987654321098",
  "addressDistrict": "20000004-0000-0000-0000-000000000000",
  "addressAreaType": "RURAL",
  "addressBlock": "20000004-2000-0000-0000-000000000000",
  "addressPanchayat": "20000004-2100-0000-0000-000000000000",
  "jurisdictionId": "20000004-2100-0000-0000-000000000000",
  "addressLocality": "Village Center",
  "addressLandmark": "Near School",
  "addressText": "Dasuya Village, Hoshiarpur District",
  "pinCode": "144101"
}
```

Success response (`200`):
```json
{
  "message": "Registration completed successfully. Please verify OTP to login",
  "user": {
    "id": "a0b973d4-df3a-4064-8dd5-162a9a7d969e",
    "name": "Rahul Sharma",
    "email": "rahul@example.com",
    "phone": "9876543210",
    "gender": "Male",
    "idType": "Aadhaar Card",
    "idNumber": "123456789012",
    "addressText": "House No. 123, Arera Colony, Amritsar",
    "addressCityOrPanchayat": "20000001-1000-0000-0000-000000000000",
    "pinCode": "143001"
  },
  "registrationStatus": {
    "onboardingCompleted": true,
    "profileCompleted": false,
    "locationCaptured": false
  }
}
```

Possible error responses:
- `400`: Missing required fields, invalid format, or invalid jurisdiction hierarchy
  - Example: "addressCity is required for URBAN area"
  - Example: "addressBlock is required for RURAL area"
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
