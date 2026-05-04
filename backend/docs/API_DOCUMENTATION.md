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

## User Profile APIs

### 4. Get Current User Profile
- **Method:** `GET`
- **Path:** `/users/me`
- **Auth:** Required
- **Description:** Returns profile data required to render profile screen on load, including name, profile image URL, jurisdiction and district details.

Success response (`200`):
```json
{
  "user": {
    "id": "user-uuid",
    "name": "Citizen User",
    "email": "citizen@example.com",
    "phone": "9876543210",
    "gender": "Male",
    "profileImageUrl": "https://example.com/profile.jpg",
    "district": {
      "id": "district-uuid",
      "name": "New Delhi"
    },
    "jurisdiction": {
      "id": "ward-uuid",
      "name": "Ward 12",
      "type": "WARD"
    },
    "address": {
      "addressAreaType": "URBAN",
      "addressDistrict": "New Delhi",
      "addressCityOrPanchayat": "Karol Bagh",
      "addressWard": "Ward 12",
      "addressLocality": "Ajmal Khan Road",
      "addressLandmark": "Near Metro Gate 3",
      "addressText": "Ajmal Khan Road, Ward 12, New Delhi",
      "pinCode": "110005",
      "addressLat": 28.6519,
      "addressLng": 77.1909
    },
    "jurisdictionIds": {
      "districtId": "district-uuid",
      "cityOrPanchayatId": "city-or-panchayat-uuid",
      "wardId": "ward-uuid",
      "jurisdictionId": "ward-uuid"
    },
    "registrationStatus": {
      "onboardingCompleted": true,
      "profileCompleted": true,
      "locationCaptured": true
    }
  }
}
```

Possible error responses:
- `401`: Missing auth token
- `403`: Invalid/expired token
- `404`: User not found

---

### 5. Update Current User Profile
- **Method:** `PATCH`
- **Path:** `/users/me`
- **Auth:** Required
- **Description:** Updates editable account settings fields. Immutable fields are rejected.

Editable fields:
- `phone`
- `gender`
- `addressAreaType` (`URBAN`/`RURAL`)
- Jurisdiction hierarchy IDs (preferred):
- `districtId`
- `cityId` (URBAN only)
- `wardId` (URBAN only)
- `blockId` (RURAL only)
- `panchayatId` (RURAL only)
- `jurisdictionId` (optional direct override, backward compatibility)
- `addressDistrict`
- `addressCityOrPanchayat`
- `addressWard`
- `addressLocality`
- `addressLandmark`
- `addressText`
- `pinCode`
- `addressLat`
- `addressLng`
- `profileImageUrl`

Immutable fields (cannot be updated):
- `name`
- `email`
- `idType`
- `idNumber`
- `aadhaarNumber`

Request example:
```json
{
  "phone": "9990011223",
  "addressAreaType": "URBAN",
  "districtId": "20000001-0000-0000-0000-000000000000",
  "cityId": "20000001-1000-0000-0000-000000000000",
  "wardId": "20000001-1110-0000-0000-000000000000",
  "addressLocality": "Block A",
  "addressLandmark": "Near Park",
  "addressText": "Block A, Ward 14, Karol Bagh",
  "pinCode": "110005",
  "addressLat": 28.6521,
  "addressLng": 77.1912,
  "profileImageUrl": "https://example.com/new-profile.jpg"
}
```

Success response (`200`):
```json
{
  "message": "Profile updated successfully",
  "user": {
    "id": "user-uuid",
    "name": "Citizen User",
    "email": "citizen@example.com",
    "phone": "9990011223",
    "gender": "Male",
    "profileImageUrl": "https://example.com/new-profile.jpg",
    "district": {
      "id": "district-uuid",
      "name": "New Delhi"
    },
    "jurisdiction": {
      "id": "ward-uuid",
      "name": "Ward 14",
      "type": "WARD"
    },
    "address": {
      "addressAreaType": "URBAN",
      "addressDistrict": "20000001-0000-0000-0000-000000000000",
      "addressCityOrPanchayat": "20000001-1000-0000-0000-000000000000",
      "addressWard": "20000001-1110-0000-0000-000000000000",
      "addressLocality": "Block A",
      "addressLandmark": "Near Park",
      "addressText": "Block A, Ward 14, Karol Bagh",
      "pinCode": "110005",
      "addressLat": 28.6521,
      "addressLng": 77.1912
    },
    "jurisdictionIds": {
      "districtId": "20000001-0000-0000-0000-000000000000",
      "cityOrPanchayatId": "20000001-1000-0000-0000-000000000000",
      "wardId": "20000001-1110-0000-0000-000000000000",
      "jurisdictionId": "20000001-1110-0000-0000-000000000000"
    },
    "registrationStatus": {
      "onboardingCompleted": true,
      "profileCompleted": true,
      "locationCaptured": true
    }
  }
}
```

Validation error response (`400`) example:
```json
{
  "error": "Validation failed",
  "details": [
    "name cannot be updated",
    "addressAreaType must be URBAN or RURAL"
  ]
}
```

Possible error responses:
- `400`: Invalid fields or immutable field update attempt
- `401`: Missing auth token
- `403`: Invalid/expired token
- `404`: User not found

---

### 6. Get My Activity Summary
- **Method:** `GET`
- **Path:** `/users/me/activity-summary`
- **Auth:** Required
- **Description:** Returns aggregate counts of the authenticated user’s issues by status for profile activity summary cards/widgets.

Success response (`200`):
```json
{
  "summary": {
    "total": 24,
    "open": 5,
    "assigned": 4,
    "inProgress": 3,
    "resolved": 10,
    "closed": 2,
    "rejected": 0,
    "lastActivityAt": "2026-05-04T09:10:00.000Z"
  }
}
```

Possible error responses:
- `401`: Missing auth token
- `403`: Invalid/expired token
- `404`: User not found

---

### 7. Get My Activity Feed
- **Method:** `GET`
- **Path:** `/users/me/activity`
- **Auth:** Required
- **Description:** Returns a paginated citizen-visible activity timeline based on `issue_updates` for issues reported by the authenticated user.
- **Description:** Returns a unified timeline with user activity events like issue reported, profile updated, and issue progress updates.

Query params:
- `page` (optional, default `1`)
- `limit` (optional, default `20`, max `100`)

Success response (`200`):
```json
{
  "page": 1,
  "limit": 20,
  "events": [
    {
      "eventId": "issue-update-1201",
      "eventType": "STATUS_CHANGED",
      "title": "Issue update: #302",
      "message": "Issue has been resolved and proof uploaded.",
      "issue": {
        "id": 302,
        "title": "Streetlight not working",
        "currentStatus": "resolved"
      },
      "oldStatus": "in_progress",
      "newStatus": "resolved",
      "proofImageUrl": "https://example.com/proof.jpg",
      "createdAt": "2026-05-04T09:10:00.000Z"
    },
    {
      "eventId": "user-event-45",
      "eventType": "PROFILE_UPDATED",
      "title": "Profile updated",
      "message": "Your account settings were updated successfully.",
      "issue": null,
      "oldStatus": null,
      "newStatus": null,
      "proofImageUrl": null,
      "metadata": {
        "updatedFields": ["phone", "addressText", "profileImageUrl"]
      },
      "createdAt": "2026-05-04T09:00:00.000Z"
    },
    {
      "eventId": "issue-created-302",
      "eventType": "ISSUE_REPORTED",
      "title": "Issue reported: #302",
      "message": "Streetlight not working",
      "issue": {
        "id": 302,
        "title": "Streetlight not working",
        "currentStatus": "open"
      },
      "oldStatus": null,
      "newStatus": "open",
      "proofImageUrl": null,
      "createdAt": "2026-05-03T18:25:00.000Z"
    }
  ]
}
```

Typical `eventType` values:
- `ISSUE_REPORTED`
- `PROFILE_UPDATED`
- `STATUS_CHANGED`
- Other admin workflow event types from `issue_updates` (for example `ASSIGNED`, `FORWARDED`, `RESOLUTION_SUBMITTED`)

Possible error responses:
- `400`: Invalid pagination query parameter
- `401`: Missing auth token
- `403`: Invalid/expired token
- `404`: User not found

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
