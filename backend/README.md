# SevaSetu Backend

Civic Issue Reporting Backend built with Node.js, Express.js, PostgreSQL, and Prisma.

## Overview

This backend provides REST APIs for a civic issue reporting system that supports:
- Offline-first mobile clients
- Geo-based querying
- Scalable issue management
- User authentication with JWT
- Media storage via Cloudinary

## Tech Stack

- **Runtime:** Node.js
- **Framework:** Express.js
- **Database:** PostgreSQL
- **ORM:** Prisma
- **Media Storage:** Cloudinary
- **Authentication:** JWT

## Project Structure

```
src/
 ├── controllers/      # Route handlers
 ├── routes/          # API route definitions
 ├── services/        # Business logic
 ├── repositories/    # Data access layer
 ├── models/          # Data models
 ├── middlewares/     # Express middlewares
 ├── utils/           # Utility functions
 ├── config/          # Configuration
 ├── jobs/            # Background jobs
 └── app.js           # Express app setup
prisma/
 ├── schema.prisma    # Database schema
 └── migrations/      # Database migrations
```

## Installation

1. Clone the repository
2. Install dependencies:
   ```bash
   npm install
   ```

3. Set up environment variables:
   ```bash
   cp .env.example .env
   ```

4. Update `.env` with your configuration

5. Set up the database:
   ```bash
   npx prisma migrate dev --name init
   ```

## Running the Server

### Development
```bash
npm run dev
```

The server will run with hot-reload using nodemon.

### Production
```bash
node server.js
```

## Database Migrations

Create a new migration:
```bash
npx prisma migrate dev --name <migration_name>
```

Push migrations to database:
```bash
npx prisma db push
```

View database in Prisma Studio:
```bash
npx prisma studio
```

## API Endpoints

(To be documented based on route implementation)

## Environment Variables

See `.env.example` for all available configuration options.

## License

ISC
