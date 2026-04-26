# SevaSetu Dashboard API Documentation

Base URL (local): `http://localhost:3000`

## Endpoint

- Method: `GET`
- Path: `/dashboard`
- Auth: Required (`Authorization: Bearer <jwt_token>`)

This endpoint returns a dashboard summary for the logged-in user and nearby area context.

---

## Search Mode Behavior

Backend chooses one of two modes:

1. Location mode
- Used when both `lat` and `lng` are provided.
- Uses `radiusKm` (default `5`).

2. District fallback mode
- Used when both `lat` and `lng` are missing.
- Requires `districtId`.

Validation:
- If only one of `lat` or `lng` is provided, request fails.
- If both `lat` and `lng` are missing and `districtId` is missing, request fails.

---

## Query Parameters

- `lat` (optional, required with `lng`)
- `lng` (optional, required with `lat`)
- `radiusKm` (optional, default `5`, used in location mode)
- `districtId` (optional in location mode, required in district fallback mode)
- `insightWindowDays` (optional, default `30`, used for nearby insights period)
- `tz` (optional, default client expectation `Asia/Kolkata`; currently accepted but not used for date bucketing in this aggregate response)

---

## Example Requests

### Location mode

```http
GET /dashboard?lat=23.2599&lng=77.4126&radiusKm=5&districtId=22222222-2222-2222-2222-222222222222&insightWindowDays=30
Authorization: Bearer <token>
```

### District fallback mode

```http
GET /dashboard?districtId=22222222-2222-2222-2222-222222222222
Authorization: Bearer <token>
```

---

## Response Shape

```json
{
  "searchContext": {
    "mode": "location",
    "location": { "lat": 23.2599, "lng": 77.4126, "radiusKm": 5 },
    "district": {
      "id": "22222222-2222-2222-2222-222222222222",
      "name": "Bhopal District",
      "category": "URBAN"
    }
  },
  "myReportsSnapshot": {
    "open": 6,
    "inProgress": 3,
    "resolved": 14,
    "rejected": 1,
    "total": 24
  },
  "myPendingAction": {
    "unresolved": 9,
    "cta": {
      "type": "OPEN_REPORTS",
      "filters": ["OPEN", "IN_PROGRESS"],
      "label": "Open Reports"
    }
  },
  "nearbyRiskSummary": {
    "highPriority": 4,
    "open": 11,
    "totalNearby": 27,
    "coverageText": "within 5 km"
  },
  "nearbyInsights": {
    "open": 11,
    "inProgress": 4,
    "closed": 12
  },
  "generatedAt": "2026-04-26T13:15:42Z"
}
```

---

## Field Rules and Guarantees

- `searchContext.mode` is always either `"location"` or `"district"`.
- `searchContext.mode` always matches the actual backend mode used.
- `myPendingAction.unresolved = myReportsSnapshot.open + myReportsSnapshot.inProgress`.
- `nearbyInsights.closed = resolved + rejected` for the insights window.
- All numeric keys are always returned (0 when no data), so frontend does not need null checks for counters.

Search context keys are always present:
- In location mode, `searchContext.location` has coordinates and `radiusKm`.
- In district mode, `searchContext.location` is present with null fields.
- If district was not part of location query, `searchContext.district` is present with null fields.

---

## Error Responses

### lat/lng mismatch

```json
{
  "message": "lat and lng must be provided together"
}
```

### district fallback missing districtId

```json
{
  "message": "districtId is required when lat/lng are missing"
}
```

Other possible errors:
- `401`: Missing auth token.
- `403`: Invalid or expired auth token.
- `400`: Validation error (for example invalid coordinates or invalid district type/id).
- `500`: Internal Server Error.
