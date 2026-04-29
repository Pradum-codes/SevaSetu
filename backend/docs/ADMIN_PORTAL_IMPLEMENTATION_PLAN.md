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

## Deployment Checklist

1. Add schema fields in a migration.
2. Generate Prisma client.
3. Add timeline read route.
4. Add admin auth routes.
5. Add admin issue write routes.
6. Test existing mobile routes for unchanged responses.
7. Deploy backend.
8. Deploy `/admin` portal separately or serve it as a static app later.

