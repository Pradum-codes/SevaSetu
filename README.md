# SevaSetu

## Overview

**SevaSetu** is a civic issue reporting platform designed to connect citizens with local authorities. It enables users to report real-world problems such as potholes, garbage overflow, and infrastructure failures, while providing administrators with tools to manage, prioritize, and resolve these issues efficiently.

The system is designed with an **offline-first approach**, geo-based querying, and a scalable backend architecture.

---

## Project Structure

```
.
├── android/        # Android application (Kotlin)
├── backend/        # Node.js + Express backend API
├── docs/           # Architecture diagrams, API specs, design docs
└── README.md       # Project entry point
```

---

## Features

### Citizen Features

* Report issues with image, description, and location
* View nearby issues on map
* Upvote issues to increase priority
* Track issue status
* Offline-first support with background sync

### Admin Features

* View and filter reported issues
* Assign issues to departments
* Update issue status
* Monitor trends and system activity

---

## System Architecture

```
Android App (Offline-First)
        ↓
Local Storage (Room DB)
        ↓
Sync Layer (WorkManager)
        ↓
REST API (Node.js + Express)
        ↓
PostgreSQL Database
        ↓
Cloudinary (Media Storage)
```

---

## Tech Stack

### Frontend (Android)

* Kotlin
* Room Database
* WorkManager
* Google Maps SDK

### Backend

* Node.js
* Express.js
* PostgreSQL
* Prisma / Knex
* Cloudinary
* JWT Authentication

---

## Key Concepts

### Offline-First Design

* Data stored locally on device
* Sync happens when network is available
* Backend supports batch and delta sync

### Geo-Based Querying

* Issues fetched using bounding box queries
* Optimized for map-based visualization

### Priority System

* Issues ranked using votes and recency
* Helps authorities focus on critical problems

---

## Getting Started

### Clone Repository

```bash
git clone <repo-url>
cd sevasetu
```

### Backend Setup

```bash
cd backend
npm install
npm run dev
```

### Android Setup

Open the `android/` folder in Android Studio and run the project.

---

## Documentation

Additional documentation can be found in the `docs/` folder:

* System design
* API specifications
* Database schema

---

## Future Enhancements

* Heatmap visualization for dense issue areas
* AI-based issue classification
* SLA tracking for departments
* Advanced analytics dashboard

---

## Contribution

Contributions are welcome. Please open an issue or submit a pull request.

---

## License

MIT License

---

## Author

Developed as part of a civic technology initiative to improve public service delivery and community engagement.
