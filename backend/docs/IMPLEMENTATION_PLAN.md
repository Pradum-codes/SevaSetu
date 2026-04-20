# Civic Issue Reporting Backend – Implementation Plan

## 1. Overview

This document outlines the backend implementation plan for the Civic Issue Reporting system. The backend will be built using Node.js (Express), PostgreSQL, and Cloudinary for media storage. The system is designed to support offline-first mobile clients, geo-based querying, and scalable issue management.

---

## 2. Tech Stack

* **Runtime:** Node.js
* **Framework:** Express.js
* **Database:** PostgreSQL
* **ORM/Query Builder:** Prisma / Knex (recommended: Prisma)
* **Media Storage:** Cloudinary
* **Authentication:** JWT-based auth
* **Background Jobs (optional):** BullMQ / simple cron jobs
* **Caching (optional):** Redis

---

## 3. High-Level Architecture

```
Client (Android)
      ↓
REST API (Express)
      ↓
Service Layer
      ↓
Database (PostgreSQL)
      ↓
Cloudinary (Images)
```

---

## 4. Project Structure

```
src/
 ├── controllers/
 ├── routes/
 ├── services/
 ├── repositories/
 ├── models/
 ├── middlewares/
 ├── utils/
 ├── config/
 ├── jobs/
 └── app.js
```

---

## 5. Database Design

### Tables

#### Users

* id (PK)
* name
* email
* password
* role (citizen/admin)
* created_at

#### Issues

* id (PK)
* client_id (unique)
* user_id (FK)
* title
* description
* image_url
* lat
* lng
* status
* priority
* department_id
* created_at
* updated_at

#### Votes

* user_id (FK)
* issue_id (FK)
* created_at

Constraints:

* UNIQUE(user_id, issue_id)

#### Departments

* id
* name

#### Issue_Updates

* id
* issue_id
* status
* updated_by
* timestamp

---

## 6. Core Features Implementation

### 6.1 Authentication

* Register/Login endpoints
* JWT token issuance
* Middleware for protected routes

---

### 6.2 Issue Creation (Offline-Friendly)

Endpoint:

```
POST /issues
```

Key Points:

* Accept `client_id` (UUID)
* Ensure idempotency
* Upload image to Cloudinary
* Store returned URL

---

### 6.3 Sync API

Endpoint:

```
GET /issues/sync
```

Query Params:

* lastSync
* bbox (lat1,lng1,lat2,lng2)

Response:

* new
* updated
* deleted

---

### 6.4 Batch Sync (Offline Uploads)

Endpoint:

```
POST /sync/batch
```

Payload:

* issues[]
* votes[]

---

### 6.5 Map Query (Geo-Based)

Endpoint:

```
GET /issues
```

Query:

* bbox

Implementation:

* Filter using lat/lng range
* Limit results (pagination)

---

### 6.6 Voting System

Endpoint:

```
POST /issues/:id/vote
```

Logic:

* Insert vote
* Enforce unique constraint
* Update issue priority asynchronously

---

### 6.7 Priority Calculation

Basic Formula:

```
priority = votes + recency_factor
```

Can be computed:

* On write (simple)
* Or via background job (scalable)

---

## 7. Image Handling (Cloudinary)

Flow:

1. Client uploads image
2. Backend uploads to Cloudinary
3. Store secure URL in DB

Considerations:

* Compress images before upload
* Limit size

---

## 8. Error Handling

* Central error middleware
* Standard response format

Example:

```
{
  "success": false,
  "message": "Error message"
}
```

---

## 9. Security

* JWT authentication
* Input validation (Joi/Zod)
* Rate limiting
* SQL injection prevention (ORM)

---

## 10. Performance Optimization

* Index on (lat, lng)
* Pagination for queries
* Use caching for frequent reads (optional Redis)
* Batch operations for sync

---

## 11. Deployment

* Use Docker
* Host on:

    * AWS / GCP / Render
* PostgreSQL via managed service

---

## 12. Development Phases

### Phase 1 (MVP)

* Auth
* Issue CRUD
* Image upload
* Basic geo queries

### Phase 2

* Sync APIs
* Voting system
* Priority calculation

### Phase 3

* Optimization
* Caching
* Analytics

---

## 13. Future Enhancements

* ML-based issue classification
* Heatmaps
* SLA tracking
* Admin dashboard

---

## 14. Conclusion

This backend is designed to be scalable, offline-friendly, and efficient for geo-based civic issue reporting. The modular structure allows future migration to microservices if needed.
