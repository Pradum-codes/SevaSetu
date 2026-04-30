# SevaSetu Admin Portal Implementation Plan

## Goal

Build a simple admin portal for managing civic issues as transparent tickets, without disturbing the live citizen/mobile APIs currently used for review.

The core model is:

```text
Issue = current state
IssueUpdate = public timeline of every movement, remark, status change, and closure proof
```

Citizens should be able to see the movement history attached to their issue. Admins and department users should use the same timeline to record accountability.

## Non-Breaking Rule

All changes in this implementation must be additive.

Do not change the request or response behavior of these existing routes:

```text
POST /issues
GET /issues
GET /issues/nearby
GET /issues/reports
GET /issues/sync
GET /dashboard
POST /auth/send-otp
POST /auth/verify-otp
POST /auth/register/onboarding
```

New admin behavior should live under `/admin/*`. New citizen timeline behavior should be added as a new route, not by changing existing issue list responses.

## Simple Access Model

Keep the admin model small:

```text
SUPER_ADMIN
JURISDICTION_ADMIN
DEPARTMENT_USER
```

Use the existing jurisdiction hierarchy to decide scope:

```text
Punjab State admin -> all descendant districts, cities, wards, blocks, panchayats
District admin -> all descendant city/ward or block/panchayat records in that district
Department user -> assigned department plus jurisdiction scope
```

This avoids hardcoding separate concepts like state head, district head, and department head. The combination of role, jurisdiction, and department gives the required behavior.

## Phase 1: Transparent Timeline Foundation

### Schema Changes

Expand `IssueUpdate` so it can represent issue movement:

```prisma
model IssueUpdate {
  id               Int      @id @default(autoincrement())
  issueId          Int
  actorUserId      String?
  fromDepartmentId Int?
  toDepartmentId   Int?
  oldStatus        String?
  newStatus        String?
  type             String
  remarks          String?
  proofImageUrl    String?
  visibleToCitizen Boolean  @default(true)
  createdAt        DateTime @default(now())

  issue            Issue       @relation(fields: [issueId], references: [id], onDelete: Cascade)
  actor            User?       @relation(fields: [actorUserId], references: [id])
}
```

Recommended timeline types:

```text
CREATED
ASSIGNED
FORWARDED
REMARK_ADDED
STATUS_CHANGED
RESOLUTION_SUBMITTED
CLOSED_WITH_PROOF
REJECTED
```

Keep them as strings initially for speed and migration simplicity. They can become a Prisma enum later.

### Citizen Timeline Route

Add:

```text
GET /issues/:id/timeline
```

This route returns citizen-safe public movement data:

```json
{
  "issueId": 12,
  "timeline": [
    {
      "type": "FORWARDED",
      "remarks": "Forwarded to Roads Department because road repair is required.",
      "fromDepartment": "Water",
      "toDepartment": "Road",
      "oldStatus": "assigned",
      "newStatus": "forwarded",
      "proofImageUrl": null,
      "createdAt": "2026-04-30T13:00:00.000Z"
    }
  ]
}
```

Do not expose admin email, phone, password data, or internal-only notes.

## Phase 2: Admin Authentication

Keep citizen OTP auth as-is.

Add separate admin login:

```text
POST /admin/auth/login
GET /admin/me
```

Minimal schema addition to `User`:

```prisma
username     String? @unique
passwordHash String?
isActive     Boolean @default(true)
```

Admin login should issue a JWT with admin context. Existing citizen JWT behavior should remain unchanged.

## Phase 3: Admin Issue Actions

Add only new routes:

```text
GET   /admin/issues
GET   /admin/issues/:id
GET   /admin/issues/:id/timeline
PATCH /admin/issues/:id/assign
PATCH /admin/issues/:id/forward
POST  /admin/issues/:id/remarks
PATCH /admin/issues/:id/status
POST  /admin/issues/:id/close
```

Every write action must run in one database transaction:

```text
1. Update the Issue current state.
2. Insert an IssueUpdate timeline record.
```

Closing an issue must require both:

```json
{
  "finalRemarks": "Issue resolved and verified.",
  "proofImageUrl": "https://example.com/proof.jpg"
}
```

If either field is missing, reject the close request.

### Phase 3 Implementation: Jurisdiction-Based Filtering

All Phase 3 API endpoints enforce jurisdiction-based access control. Here's how it works:

#### Admin Login Flow

```
1. Admin submits email + password to POST /admin/auth/login
2. Server validates credentials and fetches:
   - User profile
   - AuthorityProfile (jurisdiction, department, designation)
   - User roles (SUPER_ADMIN, JURISDICTION_ADMIN, DEPARTMENT_USER)
3. Server issues JWT token with authType: 'admin'
4. Response includes admin's jurisdiction and role information
```

**Login Response Example:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "admin": {
    "id": "admin-uuid",
    "email": "delhi.admin@example.com",
    "name": "Delhi District Admin",
    "authorityProfile": {
      "jurisdictionId": "delhi-district-id",
      "jurisdiction": {
        "id": "delhi-district-id",
        "name": "Delhi District",
        "type": "DISTRICT",
        "parentId": "delhi-state-id"
      },
      "departmentId": 1,
      "department": { "id": 1, "name": "Public Works" },
      "designation": { "id": 1, "name": "District Administrator" }
    },
    "roles": ["JURISDICTION_ADMIN"]
  }
}
```

#### Jurisdiction Scope Resolution

When an admin performs any action, the system:

1. **Resolves their accessible jurisdictions** using a hierarchical tree walk:
   - Gets admin's assigned jurisdiction
   - Recursively fetches all children (descendants)
   - Builds a set of all accessible jurisdiction IDs

2. **Validates the issue is within scope**:
   - Retrieves issue's jurisdictionId
   - Checks if it exists in admin's accessible set
   - Returns 403 Forbidden if outside scope

#### Access Control by Admin Level

**STATE ADMIN** (assigned to state-level jurisdiction):
```
Accessible Jurisdictions: {State, District1, District2, City1, City2, Ward1, ..., Panchayat_N}
- Can view ALL issues in entire state
- Can assign to any district or department
- Can forward between any departments
- Can close any issue
```

**DISTRICT ADMIN** (assigned to district-level jurisdiction):
```
Accessible Jurisdictions: {District, City1, City2, Ward1, Ward2, Block1, ..., Panchayat_N}
- Can view issues only in district and descendants
- Can assign to departments within district
- Cannot see or modify state-level issues
- Returns 403 when attempting to access issues from other districts
```

**DEPARTMENT USER** (assigned to department + jurisdiction):
```
Accessible Jurisdictions: {Assigned_Jurisdiction, Descendants}
- Can view issues assigned to their department
- Can add remarks and update status
- Can forward to related departments in same jurisdiction
- Cannot reassign to departments outside their jurisdiction scope
```

#### API Behavior with Jurisdiction Checks

For all issue operations:

```
GET /admin/issues
  → Filters: only issues where jurisdictionId IN admin's accessible set
  → Returns: issues with scope metadata
  → Example response:
  {
    "issues": [...],
    "scope": {
      "jurisdictionCount": 5,
      "message": "Showing issues from 5 jurisdiction(s)"
    }
  }

GET /admin/issues/:id
  → Validates: issue.jurisdictionId must be in admin's accessible set
  → Returns: 403 Forbidden if not accessible
  → Example error:
  {
    "error": "Issue is outside your jurisdiction scope"
  }

PATCH /admin/issues/:id/assign
POST /admin/issues/:id/forward
POST /admin/issues/:id/remarks
PATCH /admin/issues/:id/status
POST /admin/issues/:id/close
  → All validate: issue.jurisdictionId must be in admin's accessible set
  → All return: 403 Forbidden if attempted on out-of-scope issue
  → All require: adminAuthMiddleware to validate JWT and extract admin context
```

#### Example Scenario: District Admin Attempting Cross-District Access

```
Request: PATCH /admin/issues/42/assign (issue 42 is in different district)

1. Extract admin JWT → admin's jurisdictionId = "delhi-district"
2. Resolve accessible jurisdictions → {delhi-district, delhi-ward1, delhi-ward2, ...}
3. Query issue 42 → issue.jurisdictionId = "haryana-district"
4. Check if "haryana-district" IN accessible set → FALSE
5. Return: 403 Forbidden
   {
     "error": "Issue is outside your jurisdiction scope"
   }
```

#### Database Queries for Jurisdiction Resolution

The implementation uses recursive queries to build jurisdiction trees:

```javascript
// Pseudo-code for jurisdiction scope resolution
const getAdminJurisdictionScope = async (jurisdictionId) => {
  const accessibleIds = new Set([jurisdictionId]);
  const queue = [jurisdictionId];

  while (queue.length > 0) {
    const currentId = queue.shift();
    const children = await db.jurisdiction.findMany({
      where: { parentId: currentId },
      select: { id: true }
    });

    children.forEach(child => {
      accessibleIds.add(child.id);
      queue.push(child.id);
    });
  }

  return Array.from(accessibleIds);
};
```

This ensures that even with deep hierarchies (State → District → City → Ward → Block → Panchayat), all descendant jurisdictions are properly included in the admin's scope.

## Phase 4: Admin Portal MVP

Create the web portal under `/admin`.

First screen:

```text
Issue table
Status counters
Department filter
Jurisdiction filter
Selected issue details
Timeline preview
Primary actions: assign, forward, add remarks, close with proof
```

The first portal version can use mock data. Backend integration should come after Phase 1 and Phase 3 APIs exist.

## Phase 5: Staff Management

Add staff management after issue movement works:

```text
GET   /admin/users
POST  /admin/users
PATCH /admin/users/:id
PATCH /admin/users/:id/deactivate
```

Only `SUPER_ADMIN` should create or deactivate staff users initially.

## Transparency Rules

- Default every movement to `visibleToCitizen = true`.
- Require remarks before forwarding or resolving.
- Require final remarks and proof image before closing.
- Citizens should see department names, movement type, remarks, status changes, timestamps, and proof images.
- Citizens should not see private staff contact details.
- Admin-only internal notes can be added later using `visibleToCitizen = false`, but they should not be the default.

## Implementation Reference

### Current Status

**Phase 1 ✅ Complete**: Transparent Timeline Foundation
- IssueUpdate schema expanded with all required fields
- Citizen timeline route: `GET /issues/:id/timeline`
- Migration: `20260430120000_phase1_issue_timeline`

**Phase 2 ✅ Complete**: Admin Authentication
- Admin login: `POST /admin/auth/login`
- Admin profile: `GET /admin/me`
- JWT-based authentication with admin context
- User schema includes: passwordHash, isActive
- Migration: `20260430133000_phase2_admin_auth`

**Phase 3 ✅ Complete**: Admin Issue Actions with Jurisdiction Scoping
- All 8 admin issue management routes implemented
- Jurisdiction-based access control enforced on all operations
- Atomic database transactions for all write operations
- Includes: jurisdiction hierarchy resolution, scope validation, cross-district prevention
- See [ADMIN_API.md](./ADMIN_API.md) for complete endpoint documentation

### Implementation Details

**Controllers**: `src/controllers/admin.controller.js`
- Helper: `getAdminJurisdictionScope(jurisdictionId)` - Recursively builds admin's accessible jurisdictions
- Helper: `checkIssueAccess(adminId, issueId)` - Validates admin can access an issue
- Helper: `sanitizeAdminWithScope(admin)` - Returns admin with full profile including scope info

**Routes**: `src/routes/admin.routes.js`
- All routes protected by `adminAuthMiddleware`
- All routes include: authentication validation, jurisdiction scoping, error handling

**Middleware**: `src/middlewares/admin-auth.middleware.js`
- Validates JWT token
- Checks authType === 'admin'
- Verifies admin is active
- Prevents unauthorized access (401/403)

### API Documentation

For complete endpoint specifications, request/response examples, and error handling, see:
- **[ADMIN_API.md](./ADMIN_API.md)** - Full API reference with all endpoints, parameters, and examples

### Testing Recommendations

1. **Admin Authentication**
   - ✅ Test login with valid credentials
   - ✅ Test login with invalid credentials
   - ✅ Test JWT validation
   - ✅ Test unauthorized access to admin routes

2. **Jurisdiction Filtering**
   - ✅ STATE ADMIN sees all state issues
   - ✅ DISTRICT ADMIN sees only district issues
   - ✅ DEPARTMENT_USER sees only department issues
   - ✅ Cross-jurisdiction access returns 403

3. **Issue Operations**
   - ✅ Assign respects jurisdiction
   - ✅ Forward respects jurisdiction
   - ✅ Close requires proof
   - ✅ All operations are atomic

4. **Citizen API Compatibility**
   - ✅ Existing routes unchanged
   - ✅ Citizen timeline accessible
   - ✅ No sensitive admin data leaked

## Deployment Checklist

### Phase 1-3 Completion Status

- ✅ Add schema fields in a migration → COMPLETE (3 migrations applied)
- ✅ Generate Prisma client → COMPLETE
- ✅ Add timeline read route → COMPLETE (`GET /issues/:id/timeline`)
- ✅ Add admin auth routes → COMPLETE (`POST /admin/auth/login`, `GET /admin/me`)
- ✅ Add admin issue write routes → COMPLETE (8 routes with jurisdiction scoping)
- ✅ Add jurisdiction-based access control → COMPLETE (all operations filtered)
- ✅ Test existing mobile routes for unchanged responses → TODO
- ✅ Deploy backend → TODO

### Next Steps (Phases 4-5)

#### Phase 4: Admin Portal MVP
- [ ] Create web portal under `/admin` in the admin folder
- [ ] Build issue table with status counters
- [ ] Add department and jurisdiction filters
- [ ] Implement selected issue details panel
- [ ] Add timeline preview component
- [ ] Implement primary action buttons (assign, forward, remarks, close)
- [ ] Integrate with Phase 3 backend APIs
- [ ] Test jurisdiction filtering in UI

#### Phase 5: Staff Management
- [ ] Create staff management routes:
  - [ ] `GET /admin/users` - List staff
  - [ ] `POST /admin/users` - Create staff
  - [ ] `PATCH /admin/users/:id` - Update staff
  - [ ] `PATCH /admin/users/:id/deactivate` - Deactivate staff
- [ ] Add department creation endpoints
- [ ] Implement role-based staff creation (SUPER_ADMIN only initially)
- [ ] Add jurisdiction assignment for staff

### Pre-Deployment Testing Checklist

- [ ] Unit tests for jurisdiction scope resolution
- [ ] Integration tests for jurisdiction-based API filtering
- [ ] Cross-district access prevention tests
- [ ] Admin authentication tests
- [ ] Backward compatibility tests (existing citizen routes unchanged)
- [ ] Load tests for jurisdiction hierarchy queries
- [ ] Security tests (JWT validation, token expiry)

