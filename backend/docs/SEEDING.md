# Backend Seeding Guide

Use seeding to populate deterministic master data (jurisdictions, roles, departments).

## Run Seed

From `backend/`:

```bash
npm run seed
```

This script is idempotent (`upsert`), so you can run it multiple times safely.
It also prints category IDs to use as `categoryId` in `POST /issues`.

## Seeded Jurisdiction IDs

Urban path:
- `districtId`: `20000001-0000-0000-0000-000000000000`
- `cityId`: `20000001-1000-0000-0000-000000000000`
- `wardId`: `20000001-1110-0000-0000-000000000000`

Rural path:
- `districtId`: `20000004-0000-0000-0000-000000000000`
- `blockId`: `20000004-2000-0000-0000-000000000000`
- `panchayatId`: `20000004-2100-0000-0000-000000000000`

## Category IDs

Categories are seeded by name (`Road`, `Garbage`, `Water`, `Electricity`, `Streetlight`).
After running seed, use the printed output to map category names to IDs for API requests.

## Why seed script over admin route?

- Seeding is a deployment/bootstrap concern, not regular runtime API usage.
- Current backend does not yet enforce admin role authorization on write routes.
- A seed script avoids exposing dangerous setup endpoints to clients.

You can add admin CRUD routes later for managing master data after role-based middleware is in place.


## Seeding for admin
```
npm install
npm run build
npm run create-admin -- --email admin@example.com --password "secret" --name "Admin Name"
```