# SevaSetu Issue API Documentation

Base URL (local): `http://localhost:3000`

## Overview

This document covers issue reporting and sync APIs:

- `POST /issues` (create issue, idempotent via `clientId`)
- `GET /issues` (list issues with optional map/status filters)
- `GET /issues/nearby` (issues under a district)
- `GET /issues/reports` (issues reported by logged-in user)
- `GET /issues/sync` (delta sync since `lastSync`)

---

## Authentication

### Protected endpoints

These endpoints require JWT:

- `POST /issues`
- `GET /issues/sync`

Header format:

```http
Authorization: Bearer <jwt_token>
```

### Profile completion requirement for issue creation

`POST /issues` also requires completed user profile (Aadhaar + structured address fields).  
If incomplete, API returns `403`.

---

## 1. Create Issue

- **Method:** `POST`
- **Path:** `/issues`
- **Auth:** Required
- **Description:** Creates a new issue. Uses `clientId` for idempotency (offline-first safe).

### Request body

Urban example:

```json
{
  "clientId": "2f09c339-b6f5-4d52-a7a9-a651f4894a16",
  "categoryId": 2,
  "title": "Garbage pile near main market",
  "description": "Garbage has not been collected for 3 days.",
  "addressText": "Market Road, Ward 12, Bhopal",
  "locality": "Old Market",
  "landmark": "Near Hanuman Mandir",
  "lat": 23.2599,
  "lng": 77.4126,
  "imageUrls": [
    "https://example.com/image.jpg",
    "https://example.com/image-2.jpg"
  ],
  "priority": "medium",
  "districtId": "district-uuid",
  "cityId": "city-uuid",
  "wardId": "ward-uuid"
}
```

Rural example:

```json
{
  "clientId": "9b8ce1e9-57c0-472c-a3f2-6c797f43a641",
  "categoryId": 3,
  "title": "Handpump leakage",
  "description": "Water leakage near school road.",
  "addressText": "School Road, Ichhawar",
  "locality": "School Road",
  "landmark": "Near Primary School",
  "lat": 23.1599,
  "lng": 77.2126,
  "imageUrls": ["https://example.com/image2.jpg"],
  "priority": "medium",
  "districtId": "district-uuid",
  "blockId": "block-uuid",
  "panchayatId": "panchayat-uuid"
}
```

### Notes

- `clientId` and `client_id` are both accepted.
- `categoryId` is required and must exist in `categories`.
- `addressText` is required.
- `imageUrls` (or single `imageUrl`) must include at least one URL.
- `priority` allowed values: `low`, `normal`, `medium`, `high` (`medium` maps to `NORMAL`).
- Coordinates are optional, but if sent then both `lat` and `lng` are required together.
- `districtId` is required and must be a valid `DISTRICT`.
- Backend reads district category to decide branch:
  - `URBAN`: `cityId` and `wardId` required.
  - `RURAL`: `blockId` and `panchayatId` required.
- Urban and rural fields cannot be mixed in one request.
- Backend derives and stores final `jurisdictionId` from the validated hierarchy.
- `departmentId` is system-managed/admin workflow and should not be sent by citizen client.
- If same `clientId` is sent again by same user, no duplicate row is created.

### Success response (new issue, `201`)

```json
{
  "message": "Issue reported successfully",
  "idempotent": false,
  "issue": {
    "id": "issue-uuid",
    "clientId": "2f09c339-b6f5-4d52-a7a9-a651f4894a16",
    "userId": "user-uuid",
    "categoryId": 2,
    "title": "Garbage pile near main market",
    "description": "Garbage has not been collected for 3 days.",
    "addressText": "Market Road, Ward 12, Bhopal",
    "landmark": "Near Hanuman Mandir",
    "locality": "Old Market",
    "lat": 23.2599,
    "lng": 77.4126,
    "status": "OPEN",
    "priority": "NORMAL",
    "departmentId": null,
    "jurisdictionId": "required-jurisdiction-uuid",
    "category": {
      "id": 2,
      "name": "Garbage"
    },
    "images": [
      {
        "id": "image-uuid-1",
        "imageUrl": "https://example.com/image.jpg",
        "createdAt": "2026-04-20T10:00:00.000Z"
      }
    ],
    "createdAt": "2026-04-20T10:00:00.000Z",
    "updatedAt": "2026-04-20T10:00:00.000Z"
  }
}
```

### Success response (duplicate `clientId` for same user, `200`)

```json
{
  "message": "Issue already exists for this clientId",
  "idempotent": true,
  "issue": {
    "id": "issue-uuid",
    "clientId": "2f09c339-b6f5-4d52-a7a9-a651f4894a16",
    "userId": "user-uuid",
    "categoryId": 2,
    "title": "Garbage pile near main market",
    "description": "Garbage has not been collected for 3 days.",
    "addressText": "Market Road, Ward 12, Bhopal",
    "landmark": "Near Hanuman Mandir",
    "locality": "Old Market",
    "lat": 23.2599,
    "lng": 77.4126,
    "status": "OPEN",
    "priority": "NORMAL",
    "departmentId": null,
    "jurisdictionId": "required-jurisdiction-uuid",
    "category": {
      "id": 2,
      "name": "Garbage"
    },
    "images": [
      {
        "id": "image-uuid-1",
        "imageUrl": "https://example.com/image.jpg",
        "createdAt": "2026-04-20T10:00:00.000Z"
      }
    ],
    "createdAt": "2026-04-20T10:00:00.000Z",
    "updatedAt": "2026-04-20T10:00:00.000Z"
  }
}
```

### Possible error responses

- `400`: Validation error (missing/invalid fields)
- `401`: Missing/invalid auth token
- `403`: Profile incomplete
- `409`: `clientId` exists for another user
- `500`: Internal server error

---

## 2. Get Issues (List / Map Query)

- **Method:** `GET`
- **Path:** `/issues`
- **Auth:** Not required
- **Description:** Returns paginated issues with optional viewport and status filtering.

### Query params

- `page` (optional, default `1`)
- `limit` (optional, default `50`, max `100`)
- `status` (optional, lowercase recommended, e.g. `open`)
- `bbox` (optional): `minLat,minLng,maxLat,maxLng`

Example:

```http
GET /issues?page=1&limit=20&status=open&bbox=23.0,77.0,23.5,77.7
```

### Success response (`200`)

```json
{
  "issues": [
    {
      "id": 101,
      "categoryId": 2,
      "clientId": "2f09c339-b6f5-4d52-a7a9-a651f4894a16",
      "userId": "user-uuid",
      "title": "Garbage pile near main market",
      "description": "Garbage has not been collected for 3 days.",
      "addressText": "Market Road, Ward 12, Bhopal",
      "landmark": "Near Hanuman Mandir",
      "locality": "Old Market",
      "lat": 23.2599,
      "lng": 77.4126,
      "status": "OPEN",
      "priority": "NORMAL",
      "departmentId": null,
      "jurisdictionId": "required-jurisdiction-uuid",
      "category": { "id": 2, "name": "Garbage" },
      "images": [{ "id": "image-uuid-1", "imageUrl": "https://example.com/image.jpg", "createdAt": "2026-04-20T10:00:00.000Z" }],
      "createdAt": "2026-04-20T10:00:00.000Z",
      "updatedAt": "2026-04-20T10:00:00.000Z"
    }
  ],
  "page": 1,
  "limit": 20
}
```

### Possible error responses

- `400`: Invalid query params (for example invalid `page`, `limit`, or `bbox`)
- `500`: Internal server error

---

## 3. Sync Issues (Delta Pull)

- **Method:** `GET`
- **Path:** `/issues/sync`
- **Auth:** Required
- **Description:** Returns issues created or updated after `lastSync`.

### Query params

- `lastSync` (required, ISO date-time)
- `bbox` (optional): `minLat,minLng,maxLat,maxLng`

Example:

```http
GET /issues/sync?lastSync=2026-04-20T08:00:00.000Z&bbox=23.0,77.0,23.5,77.7
```

### Success response (`200`)

```json
{
  "new": [
    {
      "id": "issue-uuid-2",
      "categoryId": 4,
      "clientId": "b9dc9f66-8b09-46fe-bf09-b35c7ec43d82",
      "userId": "user-uuid",
      "title": "Street light not working",
      "description": "Pole #17 is off since yesterday.",
      "addressText": "Main Street, Ward 12",
      "landmark": null,
      "locality": "Main Street",
      "lat": 23.28,
      "lng": 77.41,
      "status": "OPEN",
      "priority": "NORMAL",
      "departmentId": null,
      "jurisdictionId": "required-jurisdiction-uuid",
      "category": { "id": 4, "name": "Electricity" },
      "images": [{ "id": "image-uuid-2", "imageUrl": "https://example.com/street-light.jpg", "createdAt": "2026-04-20T09:30:00.000Z" }],
      "createdAt": "2026-04-20T09:30:00.000Z",
      "updatedAt": "2026-04-20T09:30:00.000Z"
    }
  ],
  "updated": [
    {
      "id": "issue-uuid-3",
      "categoryId": 3,
      "clientId": "c6c02895-f96a-4f5c-b756-4fd90dfadf7f",
      "userId": "user-uuid",
      "title": "Water leakage near ward office",
      "description": "Pipeline leakage on main road.",
      "addressText": "Ward Office Road, Bhopal",
      "landmark": "Ward Office",
      "locality": "Ward Office Road",
      "lat": 23.21,
      "lng": 77.43,
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "departmentId": null,
      "jurisdictionId": "required-jurisdiction-uuid",
      "category": { "id": 3, "name": "Water" },
      "images": [],
      "createdAt": "2026-04-19T07:00:00.000Z",
      "updatedAt": "2026-04-20T09:10:00.000Z"
    }
  ],
  "deleted": []
}
```

### Possible error responses

- `400`: Missing/invalid `lastSync`, invalid `bbox`
- `401`: Missing token
- `403`: Invalid/expired token
- `500`: Internal server error

---

## 4. Nearby Issues By District

- **Method:** `GET`
- **Path:** `/issues/nearby`
- **Auth:** Not required
- **Description:** Returns issues whose `jurisdictionId` belongs to the given district subtree.

### Query params

- `districtId` (required, must be `DISTRICT`)
- `page` (optional, default `1`)
- `limit` (optional, default `50`, max `100`)
- `status` (optional: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`)

Example:

```http
GET /issues/nearby?districtId=22222222-2222-2222-2222-222222222222&page=1&limit=20
```

### Success response (`200`)

```json
{
  "district": {
    "id": "22222222-2222-2222-2222-222222222222",
    "name": "Bhopal District",
    "category": "URBAN"
  },
  "page": 1,
  "limit": 20,
  "issues": []
}
```

---

## 5. My Reported Issues

- **Method:** `GET`
- **Path:** `/issues/reports`
- **Auth:** Required
- **Description:** Returns issues created by authenticated user.

### Query params

- `page` (optional, default `1`)
- `limit` (optional, default `50`, max `100`)
- `status` (optional: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`)

Example:

```http
GET /issues/reports?page=1&limit=20
Authorization: Bearer <jwt_token>
```

### Success response (`200`)

```json
{
  "page": 1,
  "limit": 20,
  "issues": []
}
```

---

## Field Validation Summary

- `clientId`: required for create
- `categoryId`: required positive integer (must exist)
- `title`: required, non-empty string
- `description`: required, non-empty string
- `addressText`: required, non-empty string
- `imageUrls` or `imageUrl`: at least one URL required
- `lat/lng`: optional; if one is sent, both are required; valid bounds apply
- `priority`: optional, `low | normal | medium | high` (default `medium` => `NORMAL`)
- `districtId`: required, must reference a `DISTRICT`
- Urban districts: `cityId` + `wardId` required
- Rural districts: `blockId` + `panchayatId` required
- `jurisdictionId` is derived server-side from the chain and returned in response

Note:
- `departmentId` is intentionally not accepted from citizen input. Assignment/routing is handled by admin/system workflow.

---

## Offline-First Notes

- Use device-generated UUID as `clientId` for each locally created issue.
- Retrying `POST /issues` with same `clientId` is safe and won’t create duplicates for same user.
- Use `GET /issues/sync?lastSync=...` for incremental pull into local DB.
