# Admin API

This document describes the admin authentication endpoints and usage for the SevaSetu backend.

## Authentication
- Admins authenticate with email + password.
- Admin tokens are JWTs signed with the same `JWT_SECRET` as citizen tokens, but admin tokens include the claim `authType: 'admin'`.
- Provide the token in the `Authorization` header for protected admin routes:

  Authorization: Bearer <token>

## Endpoints

### POST /admin/auth/login
- Description: Sign in an admin with `email` and `password`.
- Request body (application/json):
```
  {
    "email": "admin@example.com",
    "password": "secret-password"
  }
```
- Success (200):
```
  {
    "token": "<jwt>",
    "admin": {
      "id": "uuid",
      "email": "admin@example.com",
      "name": "Admin Name",
      "isActive": true,
      "createdAt": "2026-04-30T12:00:00.000Z"
    }
  }
```
- Errors:
  - 400: missing fields
  - 401: invalid credentials or inactive admin
  - 500: server error / configuration

### GET /admin/me
- Description: Return authenticated admin profile. Requires admin JWT with `authType: 'admin'`.
- Headers:
  - `Authorization: Bearer <token>`
- Success (200):

  {
    "admin": { /* same shape as /auth/login admin */ },
    "auth": { "type": "admin" }
  }

- Errors:
  - 401: missing/invalid token or admin not found/inactive
  - 403: token present but not an admin token

## Notes and Implementation
- The admin login uses the `users` table fields `email`, `passwordHash` and `isActive`.
- The citizen OTP flow is unchanged and continues to use `/auth/*` endpoints; admin auth is additive under `/admin/*`.
- Admin JWTs include `authType: 'admin'`. Use middleware `admin-auth.middleware.js` to protect admin routes.

## Example curl

Login:

```bash
curl -X POST https://your-api.example.com/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"secret"}'
```

Get profile:

```bash
curl https://your-api.example.com/admin/me \
  -H 'Authorization: Bearer <token>'
```

## Phase 3: Admin Issue Management

All Phase 3 endpoints require the admin JWT token in the `Authorization` header.

### GET /admin/issues
- Description: List all issues with optional filters and pagination.
- Headers:
  - `Authorization: Bearer <token>`
- Query Parameters:
  - `status` - Filter by issue status (e.g., 'open', 'assigned', 'forwarded', 'closed')
  - `departmentId` - Filter by department ID
  - `categoryId` - Filter by category ID
  - `page` - Page number (default: 1)
  - `limit` - Results per page (default: 20)
- Success (200):
```json
{
  "issues": [
    {
      "id": 1,
      "clientId": "uuid",
      "title": "Pothole on Main Street",
      "description": "Large pothole needs repair",
      "status": "open",
      "lat": 28.7041,
      "lng": 77.1025,
      "user": {
        "id": "citizen-uuid",
        "email": "citizen@example.com",
        "name": "Citizen Name"
      },
      "category": {
        "id": 1,
        "name": "Roads"
      },
      "department": {
        "id": 1,
        "name": "Roads Department"
      },
      "updates": [
        {
          "type": "ASSIGNED",
          "newStatus": "assigned",
          "createdAt": "2026-04-30T10:00:00.000Z"
        }
      ],
      "createdAt": "2026-04-30T08:00:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "totalPages": 8
  }
}
```
- Errors:
  - 401: missing/invalid admin token
  - 500: server error

### GET /admin/issues/:issueId
- Description: Get detailed issue information including full timeline.
- Headers:
  - `Authorization: Bearer <token>`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Success (200):
```json
{
  "issue": {
    "id": 1,
    "clientId": "uuid",
    "title": "Pothole on Main Street",
    "status": "open",
    "user": {
      "id": "citizen-uuid",
      "email": "citizen@example.com",
      "name": "Citizen Name",
      "phone": "+91-1234567890"
    },
    "updates": [
      {
        "type": "ASSIGNED",
        "remarks": "Assigned to Roads Department",
        "oldStatus": "open",
        "newStatus": "assigned",
        "actor": {
          "id": "admin-uuid",
          "email": "admin@example.com",
          "name": "Admin Name"
        },
        "fromDepartment": null,
        "toDepartment": {
          "id": 1,
          "name": "Roads Department"
        },
        "createdAt": "2026-04-30T10:00:00.000Z"
      }
    ],
    "createdAt": "2026-04-30T08:00:00.000Z",
    "updatedAt": "2026-04-30T10:00:00.000Z"
  }
}
```
- Errors:
  - 400: invalid issue ID
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

### GET /admin/issues/:issueId/timeline
- Description: Get admin view of issue timeline with all details.
- Headers:
  - `Authorization: Bearer <token>`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Success (200):
```json
{
  "issueId": 1,
  "timeline": [
    {
      "type": "CREATED",
      "remarks": "Issue reported by citizen.",
      "oldStatus": null,
      "newStatus": "open",
      "proofImageUrl": null,
      "actor": null,
      "createdAt": "2026-04-30T08:00:00.000Z"
    },
    {
      "type": "ASSIGNED",
      "remarks": "Assigned to Roads Department",
      "oldStatus": "open",
      "newStatus": "assigned",
      "actor": {
        "id": "admin-uuid",
        "email": "admin@example.com",
        "name": "Admin Name"
      },
      "visibleToCitizen": true,
      "createdAt": "2026-04-30T10:00:00.000Z"
    }
  ]
}
```
- Errors:
  - 400: invalid issue ID
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

### PATCH /admin/issues/:issueId/assign
- Description: Assign issue to a department.
- Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Request body:
```json
{
  "departmentId": 1,
  "remarks": "Assigning to Roads Department for immediate action"
}
```
- Fields:
  - `departmentId` - **Required**: Department ID
  - `remarks` - Optional: Assignment reason
- Success (200):
```json
{
  "message": "Issue assigned successfully",
  "issue": {
    "id": 1,
    "status": "assigned",
    "departmentId": 1,
    "updatedAt": "2026-04-30T11:00:00.000Z"
  }
}
```
- Errors:
  - 400: missing departmentId or invalid issue ID
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

### PATCH /admin/issues/:issueId/forward
- Description: Forward issue to another department.
- Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Request body:
```json
{
  "fromDepartmentId": 1,
  "toDepartmentId": 2,
  "remarks": "Forwarding to Water Department - water leak issue detected"
}
```
- Fields:
  - `fromDepartmentId` - **Required**: Current department ID
  - `toDepartmentId` - **Required**: Target department ID
  - `remarks` - **Required**: Reason for forwarding (cannot be empty)
- Success (200):
```json
{
  "message": "Issue forwarded successfully",
  "issue": {
    "id": 1,
    "status": "forwarded",
    "departmentId": 2,
    "updatedAt": "2026-04-30T12:00:00.000Z"
  }
}
```
- Errors:
  - 400: missing required fields (fromDepartmentId, toDepartmentId, or remarks)
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

### POST /admin/issues/:issueId/remarks
- Description: Add remarks to an issue.
- Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Request body:
```json
{
  "remarks": "Site visited. Repair requires heavy machinery.",
  "visibleToCitizen": true
}
```
- Fields:
  - `remarks` - **Required**: Remark text
  - `visibleToCitizen` - Optional (default: true): Show to citizen in timeline
- Success (200):
```json
{
  "message": "Remark added successfully",
  "update": {
    "id": 5,
    "issueId": 1,
    "type": "REMARK_ADDED",
    "remarks": "Site visited. Repair requires heavy machinery.",
    "visibleToCitizen": true,
    "actorUserId": "admin-uuid",
    "createdAt": "2026-04-30T13:00:00.000Z"
  }
}
```
- Errors:
  - 400: missing remarks or invalid issue ID
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

### PATCH /admin/issues/:issueId/status
- Description: Change issue status.
- Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Request body:
```json
{
  "newStatus": "in-progress",
  "remarks": "Repair work has started"
}
```
- Fields:
  - `newStatus` - **Required**: New status value
  - `remarks` - Optional: Reason for status change
- Success (200):
```json
{
  "message": "Issue status updated successfully",
  "issue": {
    "id": 1,
    "status": "in-progress",
    "updatedAt": "2026-04-30T14:00:00.000Z"
  }
}
```
- Errors:
  - 400: missing newStatus or invalid issue ID
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

### POST /admin/issues/:issueId/close
- Description: Close issue with proof image.
- Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- URL Parameters:
  - `issueId` - Issue ID (required)
- Request body:
```json
{
  "finalRemarks": "Pothole has been filled and compacted. Road is now safe.",
  "proofImageUrl": "https://example.com/proof-images/issue-1-before-after.jpg"
}
```
- Fields:
  - `finalRemarks` - **Required**: Final remarks about resolution
  - `proofImageUrl` - **Required**: URL to proof image
- Success (200):
```json
{
  "message": "Issue closed successfully",
  "issue": {
    "id": 1,
    "status": "closed",
    "updatedAt": "2026-04-30T15:00:00.000Z"
  }
}
```
- Errors:
  - 400: missing finalRemarks or proofImageUrl
  - 401: missing/invalid admin token
  - 404: issue not found
  - 500: server error

## Role-Based Admin Issue APIs

These endpoints are the preferred portal APIs for the three-level admin flow.
They require an admin JWT and use `AuthorityProfile.designation` plus jurisdiction scope:

- `State Administrator` with `STATE` jurisdiction -> state admin.
- `District Administrator` with `DISTRICT` jurisdiction -> district admin.
- `Department Head` with `DISTRICT` jurisdiction and department -> department admin.

### GET /admin/state/issues

State-scoped issue list. Supports:

- `districtId`: optional district under the admin's state.
- `status`: exact status filter.
- `statusGroup`: `pending`, `resolved`, or `closed`. Ignored when `status` is provided.
- `departmentId`
- `categoryId`
- `dateFrom`
- `dateTo`
- `page`
- `limit`

Returns `availableDistricts` for the state filter dropdown.

### GET /admin/district/issues

District-scoped issue list. Supports:

- `status`
- `statusGroup`
- `departmentId`
- `categoryId`
- `dateFrom`
- `dateTo`
- `page`
- `limit`

Returns `availableDepartments` for assignment and filtering.

### GET /admin/department/issues

Department-scoped issue list. A department head only receives issues assigned to their department and district.
Supports:

- `status`
- `statusGroup`
- `categoryId`
- `dateFrom`
- `dateTo`
- `page`
- `limit`

### Role-Based Write Rules

- `PATCH /admin/district/issues/:issueId/assign-to-department` requires district-admin access, district scope, `departmentId`, and `remarks`.
- `POST /admin/district/issues/:issueId/close` requires district-admin access, district scope, `finalRemarks`, and `proofImageUrl`.
- `PATCH /admin/department/issues/:issueId/update-status` requires department-head access, assigned department scope, `newStatus`, and `remarks`.
- `PATCH /admin/department/issues/:issueId/submit-proof` requires department-head access, assigned department scope, `remarks`, and `proofImageUrl`.

`pending` is a filter group, not a stored status. It maps to `open`, `assigned`, and `in_progress`.

## Role-Based Admin Management APIs

### State Admin

```text
GET  /admin/state/districts
POST /admin/state/districts
GET  /admin/state/district-heads
POST /admin/state/district-heads
```

Create district body:

```json
{
  "name": "New District",
  "category": "URBAN",
  "pincode": "144001"
}
```

Create district head body:

```json
{
  "districtId": "district-uuid",
  "name": "District Head",
  "email": "district.head@gov.in",
  "phone": "9876543210",
  "password": "StrongPassword@123"
}
```

### District Admin

```text
GET  /admin/district/departments
POST /admin/district/departments
GET  /admin/district/department-heads
POST /admin/district/department-heads
```

Create department body:

```json
{
  "name": "Road"
}
```

Create department head body:

```json
{
  "departmentId": 1,
  "name": "Road Department Head",
  "email": "road.head@gov.in",
  "phone": "9876543210",
  "password": "StrongPassword@123"
}
```

### Department Admin Proof Upload

```text
POST /admin/department/proofs/upload
```

Body:

```json
{
  "proofImageUrl": "https://example.com/proof.jpg"
}
```

The endpoint currently validates and accepts `http(s)` image URLs or `data:image/...` URLs and returns the accepted URL for use with `submit-proof`.

## Timeline Entry Types

| Type | Description | Citizen Visible |
|------|-------------|-----------------|
| CREATED | Issue initially reported | Yes |
| ROUTED_TO_DISTRICT | Issue automatically routed to the concerned district after creation | Yes |
| ASSIGNED_TO_DEPARTMENT | District admin assigned the issue to a department | Yes |
| FORWARDED | Forwarded to another department, legacy/backward-compatible movement | Yes |
| REMARK_ADDED | Admin added remarks | Configurable, default should be public |
| STATUS_CHANGED | Status changed | Yes |
| PROOF_SUBMITTED | Department submitted completion remarks and proof | Yes |
| CLOSED | District admin verified proof and closed the issue | Yes |
| CLOSED_WITH_PROOF | Legacy close event type, still accepted for old records | Yes |
| REJECTED | Issue rejected | Yes |

New issue creation records `CREATED` and `ROUTED_TO_DISTRICT` as stored timeline rows. Timeline readers should synthesize `CREATED` only for older issues that do not already have a stored creation event.

## Creating an initial admin (seed)

If you don't have an admin user yet, create one with the included script.

From the `backend` folder run:

```bash
# install dependencies and generate client if needed
npm install
npm run build

# create admin (interactive via args)
npm run create-admin -- --email admin@example.com --password "secret" --name "Admin Name"
```

Notes:
- The script will `upsert` a `User` by email, hash the provided password using bcrypt, and assign the `ADMIN` role.
- Ensure `DATABASE_URL` is set in your environment or in a `.env` file.
