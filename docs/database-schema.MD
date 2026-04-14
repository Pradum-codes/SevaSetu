# SevaSetu Documentation

## database-schema.md

### 1. Overview

The database is designed using PostgreSQL to support relational data, geo-based queries, and strong consistency.

---

### 2. Entity Relationships

```
Users ────< Issues ────< Votes
   │            │
   │            └── Issue_Updates
   │
   └── (role: admin/citizen)

Issues ────> Departments
```

---

### 3. Tables

#### 3.1 Users

| Column     | Type      | Description     |
| ---------- | --------- | --------------- |
| id         | UUID (PK) | Unique user ID  |
| name       | TEXT      | User name       |
| email      | TEXT      | Unique email    |
| password   | TEXT      | Hashed password |
| role       | TEXT      | citizen/admin   |
| created_at | TIMESTAMP | Created time    |

---

#### 3.2 Issues

| Column        | Type      | Description                       |
| ------------- | --------- | --------------------------------- |
| id            | UUID (PK) | Issue ID                          |
| client_id     | UUID      | Client-generated ID (idempotency) |
| user_id       | UUID (FK) | Reporter                          |
| title         | TEXT      | Short title                       |
| description   | TEXT      | Detailed description              |
| image_url     | TEXT      | Cloudinary URL                    |
| lat           | DOUBLE    | Latitude                          |
| lng           | DOUBLE    | Longitude                         |
| status        | TEXT      | reported/in-progress/resolved     |
| priority      | INT       | Computed priority score           |
| department_id | UUID (FK) | Assigned department               |
| created_at    | TIMESTAMP | Creation time                     |
| updated_at    | TIMESTAMP | Last update                       |

---

#### 3.3 Votes

| Column     | Type      | Description       |
| ---------- | --------- | ----------------- |
| user_id    | UUID (FK) | User who voted    |
| issue_id   | UUID (FK) | Issue being voted |
| created_at | TIMESTAMP | Vote time         |

Constraints:

* UNIQUE(user_id, issue_id)

---

#### 3.4 Departments

| Column | Type      | Description     |
| ------ | --------- | --------------- |
| id     | UUID (PK) | Department ID   |
| name   | TEXT      | Department name |

---

#### 3.5 Issue_Updates

| Column     | Type      | Description       |
| ---------- | --------- | ----------------- |
| id         | UUID (PK) | Update ID         |
| issue_id   | UUID (FK) | Related issue     |
| status     | TEXT      | New status        |
| updated_by | UUID (FK) | Admin who updated |
| timestamp  | TIMESTAMP | Update time       |

---

### 4. Indexing Strategy

* Index on (lat, lng) for geo queries
* Index on status for filtering
* Index on created_at for sorting

Example:

```
CREATE INDEX idx_issues_location ON issues(lat, lng);
```

---

### 5. Data Integrity

* Foreign key constraints for relationships
* Unique constraint for votes
* NOT NULL constraints for critical fields

---

### 6. Design Considerations

#### Why PostgreSQL?

* Strong relational model
* Supports geo queries
* Reliable for analytics and aggregation

#### Why separate Votes table?

* Prevent duplicate votes
* Efficient counting and aggregation

#### Why store image URLs?

* Keeps DB lightweight
* Offloads storage to Cloudinary

---

### 7. Future Extensions

* Add audit logs
* Add SLA tracking fields
* Introduce soft deletes (is_deleted flag)
