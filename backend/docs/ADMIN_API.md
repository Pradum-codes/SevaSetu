# Admin API

This document describes the admin authentication endpoints and usage for the SevaSetu backend.

## Authentication
- Admins authenticate with email + password.
- Admin tokens are JWTs signed with the same `JWT_SECRET` as citizen tokens, but admin tokens include the claim `authType: 'admin'`.
- Provide the token in the `Authorization` header for protected admin routes:

  Authorization: Bearer <token>

## Endpoints

### POST /admin/auth/login
- Description: Sign in an admin with `email` and `password`.
- Request body (application/json):
```
  {
    "email": "admin@example.com",
    "password": "secret-password"
  }
```
- Success (200):
```
  {
    "token": "<jwt>",
    "admin": {
      "id": "uuid",
      "email": "admin@example.com",
      "name": "Admin Name",
      "isActive": true,
      "createdAt": "2026-04-30T12:00:00.000Z"
    }
  }
```
- Errors:
  - 400: missing fields
  - 401: invalid credentials or inactive admin
  - 500: server error / configuration

### GET /admin/me
- Description: Return authenticated admin profile. Requires admin JWT with `authType: 'admin'`.
- Headers:
  - `Authorization: Bearer <token>`
- Success (200):

  {
    "admin": { /* same shape as /auth/login admin */ },
    "auth": { "type": "admin" }
  }

- Errors:
  - 401: missing/invalid token or admin not found/inactive
  - 403: token present but not an admin token

## Notes and Implementation
- The admin login uses the `users` table fields `email`, `passwordHash` and `isActive`.
- The citizen OTP flow is unchanged and continues to use `/auth/*` endpoints; admin auth is additive under `/admin/*`.
- Admin JWTs include `authType: 'admin'`. Use middleware `admin-auth.middleware.js` to protect admin routes.

## Example curl

Login:

```bash
curl -X POST https://your-api.example.com/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"secret"}'
```

Get profile:

```bash
curl https://your-api.example.com/admin/me \
  -H 'Authorization: Bearer <token>'
```

## Follow-ups
- Phase 3 will add admin issue actions (assign / forward / remarks / close) and should reuse the `admin-auth.middleware.js` guard.

## Creating an initial admin (seed)

If you don't have an admin user yet, create one with the included script.

From the `backend` folder run:

```bash
# install dependencies and generate client if needed
npm install
npm run build

# create admin (interactive via args)
npm run create-admin -- --email admin@example.com --password "secret" --name "Admin Name"
```

Notes:
- The script will `upsert` a `User` by email, hash the provided password using bcrypt, and assign the `ADMIN` role.
- Ensure `DATABASE_URL` is set in your environment or in a `.env` file.

