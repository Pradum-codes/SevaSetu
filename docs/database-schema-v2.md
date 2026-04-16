# SevaSetu Database Schema v2

## 1. Core Entities

### User

Represents all users: citizens, authorities, admins.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| name | String | NOT NULL |
| email | String | UNIQUE, NOT NULL |
| phone | String | NOT NULL |
| createdAt | Timestamp | NOT NULL |

### Role

Defines system-level permissions.

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PK |
| name | String | CITIZEN, AUTHORITY, ADMIN |

### UserRole

Maps users to roles (many-to-many).

| Column | Type | Constraints |
|--------|------|-------------|
| userId | UUID | FK → User, PK |
| roleId | Int | FK → Role, PK |

---

## 2. Jurisdiction System

### Jurisdiction

Hierarchical administrative structure for geographic organization.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| name | String | NOT NULL |
| type | Enum | STATE, DISTRICT, CITY, ZONE, WARD, PANCHAYAT |
| category | Enum | URBAN, RURAL |
| parentId | UUID | FK → Jurisdiction (self-referential) |
| pincode | String | OPTIONAL |

#### Example Hierarchy

**Urban Structure:**
```
State → District → City → Ward
```

**Rural Structure:**
```
State → District → Block → Panchayat
```

---

## 3. Authority Structure

### Department

Represents administrative departments.

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PK |
| name | String | Road, Water, Electricity, etc. |

### Designation

Represents job titles/positions.

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PK |
| name | String | Ward Officer, Zonal Engineer, etc. |

### AuthorityProfile

Links users to the governance structure.

| Column | Type | Constraints |
|--------|------|-------------|
| userId | UUID | FK → User, PK |
| designationId | Int | FK → Designation |
| departmentId | Int | FK → Department |
| jurisdictionId | UUID | FK → Jurisdiction |
| isActive | Boolean | DEFAULT true |

---

## 4. Issue Management

### Category

Issue categories for classification.

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PK |
| name | String | Road, Garbage, Water, etc. |

### Issue

Core issue entity - the central data model.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| title | String | NOT NULL |
| description | Text | NOT NULL |
| categoryId | Int | FK → Category |
| userId | UUID | FK → User (Creator) |
| jurisdictionId | UUID | FK → Jurisdiction |
| addressText | Text | NOT NULL |
| landmark | String | OPTIONAL |
| locality | String | OPTIONAL |
| status | Enum | OPEN, IN_PROGRESS, RESOLVED, REJECTED |
| priority | Enum | LOW, NORMAL, HIGH |
| createdAt | Timestamp | NOT NULL |
| updatedAt | Timestamp | NOT NULL |

### IssueImage

Attachments for issues.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| issueId | UUID | FK → Issue |
| imageUrl | String | NOT NULL |
| createdAt | Timestamp | NOT NULL |

---

## 5. Assignment & Workflow

### IssueAssignment

Tracks current ownership and assignment history.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| issueId | UUID | FK → Issue |
| assignedTo | UUID | FK → User |
| assignedBy | UUID | FK → User |
| status | Enum | ACTIVE, TRANSFERRED, COMPLETED |
| createdAt | Timestamp | NOT NULL |
| closedAt | Timestamp | OPTIONAL |

**Rule:** Only one ACTIVE assignment per issue at any time.

---

## 6. Activity Log (Audit Trail)

### IssueActivityLog

Immutable log of all actions on issues.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| issueId | UUID | FK → Issue |
| actorId | UUID | FK → User |
| action | Enum | CREATED, ASSIGNED, FORWARDED, STATUS_UPDATED, COMMENT |
| message | Text | NOT NULL |
| metadata | JSON | OPTIONAL |
| createdAt | Timestamp | NOT NULL |

#### Example Metadata

**Forwarding:**
```json
{
  "from": "Ward Officer",
  "to": "Zonal Engineer"
}
```

**Status Update:**
```json
{
  "oldStatus": "OPEN",
  "newStatus": "IN_PROGRESS"
}
```

---

## 7. Community Support

### IssueVote

Community engagement tracking.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| issueId | UUID | FK → Issue |
| userId | UUID | FK → User |
| createdAt | Timestamp | NOT NULL |
| | | UNIQUE(issueId, userId) |

---

## 8. Workflow Processes

### Issue Lifecycle

```
OPEN → IN_PROGRESS → RESOLVED
  ↓
REJECTED
```

### Assignment Flow

1. Citizen creates issue
2. System assigns to Authority (based on jurisdiction + department)
3. Authority forwards/reassigns if needed
4. Issue resolved or closed

### Activity Timeline Example

```
[Citizen] Reported issue
    ↓
[Admin] Assigned to Ward Officer
    ↓
[Ward Officer] Forwarded to Zonal Engineer
    ↓
[Zonal Engineer] Marked as In Progress
    ↓
[Zonal Engineer] Marked as Resolved
```

---

## 9. Input Model (Manual Address-Based)

### User Provides:
- District
- Area Type (Urban / Rural)
- City / Panchayat
- Ward (if urban)
- Locality (optional)
- Landmark
- Full address

### System Stores:
- **jurisdictionId** ✅ (source of truth)
- **addressText** (display)
- **landmark, locality** (supporting info)

---

## 10. Key Design Principles

- **Jurisdiction-driven assignment:** Issues routed based on jurisdiction hierarchy
- **Role vs. Designation:** Role defines system permissions; Designation defines organizational role
- **Structured data over free text:** Maintain referential integrity through foreign keys
- **Immutable audit logs:** Activity logs are never deleted, only read
- **Single active assignment:** Only one active assignment per issue at any time
- **Hierarchical jurisdictions:** Support both urban and rural administrative structures

---

## 11. Future Enhancements

- Geo-mapping (latitude/longitude + polygon boundaries)
- Automatic jurisdiction detection from coordinates
- Real-time notification system
- SLA-based escalation triggers
- Duplicate issue detection and merging
- Analytics and reporting dashboard
- Mobile app optimization for offline support