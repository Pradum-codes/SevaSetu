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


## Seeding for Admin Users

### Quick Admin (Single Account)

Create a single admin account:

```bash
npm run create-admin -- --email admin@example.com --password "secret" --name "Admin Name"
```

### Admin Hierarchy (Multiple Accounts with Jurisdiction/Department Structure)

For production deployments, seed a complete admin hierarchy with role-based jurisdiction and department assignments:

```bash
npm run seed:admins
```

This script creates:

1. **State Head** - Full state-level jurisdiction access
   - Email: `state.admin@punjab.gov.in`
   - Password: `StateAdmin@123`
   - Scope: All Punjab districts and descendants

2. **District Heads** - District-level jurisdiction access
   - Email: `kapurthala.district@gov.in`
   - Password: `DistrictAdmin@123`
   - Scope: Kapurthala District and descendants
   - Email: `amritsar.district@gov.in`
   - Password: `DistrictAdmin@123`
   - Scope: Amritsar District and descendants

3. **Department Heads** - Department-specific access within seeded districts
   - Road: `road.head@kapurthala.gov.in` / `DeptHead@123`
   - Water: `water.head@kapurthala.gov.in` / `DeptHead@123`
   - Electricity: `electricity.head@kapurthala.gov.in` / `DeptHead@123`
   - Sanitation: `sanitation.head@kapurthala.gov.in` / `DeptHead@123`
   - Road: `road.head@amritsar.gov.in` / `DeptHead@123`
   - Water: `water.head@amritsar.gov.in` / `DeptHead@123`
   - Electricity: `electricity.head@amritsar.gov.in` / `DeptHead@123`
   - Sanitation: `sanitation.head@amritsar.gov.in` / `DeptHead@123`

**Location:** `/backend/scripts/seed-admin-hierarchy.js`

**Customization:** Modify the script to add more jurisdictions (districts, blocks) or department heads as needed. Jurisdiction IDs are imported from the base seeding (`seed.js`).

### Admin Flow Seed Expectations

For the three-level admin portal, seed data should support these checks:

- State admin can see all seeded Punjab districts and all descendant issues.
- District admins can see only their own district descendants.
- Department heads can see only issues assigned to their department.
- At least one seeded district should have Road, Water, Electricity, and Sanitation department heads.
- The admin hierarchy seed includes Kapurthala and Amritsar district heads so district-wise filtering and cross-district isolation can be tested without manual setup.
