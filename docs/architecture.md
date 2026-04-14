# SevaSetu Documentation

## architecture.md

### 1. Overview

SevaSetu is a civic issue reporting platform built with an offline-first mobile client and a REST-based backend. The system enables citizens to report issues with location and images, while administrators manage and resolve them via a web interface.

The architecture is designed to be:

* Offline-first (client-side resilience)
* Scalable (modular backend)
* Efficient for geo-based queries (map-driven UI)

---

### 2. High-Level Architecture

```
[ Android App ]        [ Admin Panel (Web) ]
        \                     /
         \                   /
          ---- REST API -----
                 |
        [ Service Layer ]
                 |
        [ PostgreSQL DB ]
                 |
         [ Cloudinary ]
```

---

### 3. Core Components

#### 3.1 Android Client (Offline-First)

* Stores data locally using Room DB
* Uses WorkManager for background sync
* Captures images, location, and user input
* Displays issues on map using geo queries

#### 3.2 Admin Panel (Web)

* Built as a separate client (React)
* Allows issue management, filtering, and status updates
* Uses role-based authentication

#### 3.3 Backend (Node.js + Express)

* Exposes REST APIs
* Handles authentication (JWT)
* Implements business logic (issues, voting, routing)
* Supports batch sync and delta sync

#### 3.4 Database (PostgreSQL)

* Stores structured relational data
* Handles geo-based queries using indexed lat/lng
* Enforces constraints (votes, relationships)

#### 3.5 Media Storage (Cloudinary)

* Stores uploaded images
* Backend saves only URLs

---

### 4. Data Flow

#### 4.1 Issue Creation (Offline-First)

```
User creates issue (offline)
        ↓
Stored in local DB (status = pending)
        ↓
Sync worker triggers
        ↓
POST /issues (with client_id)
        ↓
Backend stores issue
        ↓
Response updates local DB
```

---

#### 4.2 Sync Flow

```
Client → GET /issues/sync?lastSync=...
        ↓
Backend returns:
  - new issues
  - updated issues
  - deleted issues
        ↓
Client updates local DB
```

---

#### 4.3 Map Data Flow

```
User opens map
        ↓
Load cached issues from local DB
        ↓
GET /issues?bbox=...
        ↓
Backend returns limited geo-filtered data
        ↓
Client updates markers
```

---

### 5. Key Architectural Decisions

#### Offline-First Approach

* Ensures usability in low-connectivity areas
* Requires sync APIs and local persistence

#### REST over Real-Time

* Simpler implementation
* Reduced infrastructure complexity
* Sufficient for periodic updates

#### Bounding Box Queries

* Avoids fetching entire dataset
* Optimized for map viewport

#### Idempotent APIs

* Prevent duplicate issue creation
* Uses client-generated IDs

---

### 6. Scalability Considerations

* Modular service structure (can evolve to microservices)
* Use of indexing for geo queries
* Batch APIs to reduce network overhead
* Optional caching layer (Redis)

---

### 7. Security

* JWT-based authentication
* Role-based authorization (admin vs citizen)
* Input validation and sanitization

---
