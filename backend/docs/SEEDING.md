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
- `districtId`: `22222222-2222-2222-2222-222222222222`
- `cityId`: `33333333-3333-3333-3333-333333333333`
- `zoneId`: `44444444-4444-4444-4444-444444444444`
- `wardId`: `55555555-5555-5555-5555-555555555555`

Rural path:
- `districtId`: `66666666-6666-6666-6666-666666666666`
- `blockId`: `77777777-7777-7777-7777-777777777777`
- `panchayatId`: `88888888-8888-8888-8888-888888888888`

## Category IDs

Categories are seeded by name (`Road`, `Garbage`, `Water`, `Electricity`, `Streetlight`).
After running seed, use the printed output to map category names to IDs for API requests.

## Why seed script over admin route?

- Seeding is a deployment/bootstrap concern, not regular runtime API usage.
- Current backend does not yet enforce admin role authorization on write routes.
- A seed script avoids exposing dangerous setup endpoints to clients.

You can add admin CRUD routes later for managing master data after role-based middleware is in place.
