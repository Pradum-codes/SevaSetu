# SevaSetu Local Database Schema (Android Room)

## Overview

To provide a local-first experience on Android, the device acts as a temporary source of truth while the backend serves as the eventual source of truth. This requires:

- **Local DB (Room)** mirrors core entities
- **Sync layer** reconciles local ↔ server data
- **UI** always reads from local DB, never directly from network

---

## 1. Local-First Architecture (Android)

### Architecture Diagram

```
UI Layer
  ↓
ViewModel
  ↓
Repository Pattern
  ↓
Room Database (Local Source of Truth)
  ↓
Sync Worker (WorkManager)
  ↓
Backend API
```

### Key Rules

1. **Reads** → Always from Room DB
2. **Writes** → First to Room, then sync to server
3. **Network** → Asynchronous with automatic retry logic

---

## 2. Local Storage Strategy

### What to Store Locally

Only store data required for:
- Creating issues offline
- Viewing issues locally
- Syncing with backend

### Why Minimal?

- Reduces storage footprint
- Simplifies sync conflicts
- Faster app performance
- Easier offline UX

---

## 3. Room Entity Schemas

### LocalIssue

Core issue entity stored locally.

```kotlin
@Entity
data class LocalIssue(
    @PrimaryKey val id: String,              // UUID (generated locally)
    
    val title: String,
    val description: String,
    
    val categoryId: Int,
    val jurisdictionId: String,
    
    val addressText: String,
    val landmark: String?,
    val locality: String?,
    
    val status: String,                      // OPEN, IN_PROGRESS, RESOLVED, REJECTED
    
    val syncState: String,                   // PENDING, SYNCED, FAILED
    val isDirty: Boolean,                    // Indicates local changes not synced
    
    val createdAt: Long,
    val updatedAt: Long
)
```

| Field | Type | Purpose |
|-------|------|---------|
| id | String | Primary key (UUID generated on device) |
| title | String | Issue title |
| description | String | Issue details |
| categoryId | Int | Reference to LocalCategory |
| jurisdictionId | String | Reference to LocalJurisdiction |
| addressText | String | Full address text |
| landmark | String? | Landmark reference (optional) |
| locality | String? | Locality/area (optional) |
| status | String | Current issue status |
| syncState | String | Sync status (PENDING, SYNCED, FAILED) |
| isDirty | Boolean | Flag for local modifications |
| createdAt | Long | Creation timestamp (ms) |
| updatedAt | Long | Last update timestamp (ms) |

---

### LocalIssueImage

Attachment storage with offline/online path handling.

```kotlin
@Entity
data class LocalIssueImage(
    @PrimaryKey val id: String,
    
    val issueId: String,
    val localPath: String,                   // File path on device
    val remoteUrl: String?,                  // URL after server upload
    
    val syncState: String
)
```

| Field | Type | Purpose |
|-------|------|---------|
| id | String | Primary key (UUID) |
| issueId | String | Reference to LocalIssue |
| localPath | String | Local file path (for offline access) |
| remoteUrl | String? | Remote URL (populated after sync) |
| syncState | String | Sync status |

**Storage Location:** `/storage/emulated/0/app/issues/`

---

### LocalJurisdiction

Cached jurisdiction hierarchy for offline access.

```kotlin
@Entity
data class LocalJurisdiction(
    @PrimaryKey val id: String,
    
    val name: String,
    val type: String,                       // STATE, DISTRICT, CITY, WARD, etc.
    val parentId: String?
)
```

| Field | Type | Purpose |
|-------|------|---------|
| id | String | Primary key (UUID) |
| name | String | Jurisdiction name |
| type | String | Hierarchy level |
| parentId | String? | Reference to parent jurisdiction |

---

### LocalCategory

Reference data for issue categories.

```kotlin
@Entity
data class LocalCategory(
    @PrimaryKey val id: Int,
    
    val name: String
)
```

| Field | Type | Purpose |
|-------|------|---------|
| id | Int | Primary key |
| name | String | Category name (Road, Water, Garbage, etc.) |

---

## 4. Sync State Management

### SyncState Enum

```kotlin
enum class SyncState {
    PENDING,   // Not yet sent to server
    SYNCED,    // Confirmed by server
    FAILED     // Failed sync, retry needed
}
```

### Sync State Transitions

```
         ┌─────────────┐
         │   PENDING   │
         └────┬────────┘
              │
        ┌─────┴──────┐
        │            │
    (success)    (failure)
        │            │
    ┌───▼──┐     ┌────▼──┐
    │SYNCED│     │FAILED │
    └──────┘     └────┬──┘
                      │
                 (retry logic)
                      │
                 ┌────▼──────┐
                 │ Back to   │
                 │ PENDING   │
                 └───────────┘
```

---

## 5. Write Flow (Offline-First)

### Step-by-Step Process

```
1. User creates/edits issue on device
   ↓
2. Generate UUID locally (if new)
   ↓
3. Save to Room DB immediately:
   - syncState = PENDING
   - isDirty = true
   ↓
4. ✅ UI reflects change instantly
   ↓
5. Background Worker (WorkManager):
   - Periodically checks PENDING issues
   - Attempts API call
   - Updates syncState (SYNCED or FAILED)
```

### Background Sync Logic

```kotlin
// Pseudo-code for sync worker
for each PENDING issue:
    try:
        response = api.createIssue(issue)
        dao.update(issue.copy(syncState = SYNCED))
    catch error:
        dao.update(issue.copy(syncState = FAILED))
        // Schedule retry
```

### Retry Strategy

- **Exponential backoff** recommended
- **Maximum retries:** 3-5 attempts
- **Retry interval:** 1 min → 5 min → 15 min

---

## 6. Conflict Resolution

### Conflict Scenario

> Same issue updated locally on device + simultaneously updated on server

### Resolution Strategy (MVP)

**Last-Write-Wins:** Compare timestamps, server timestamp takes precedence.

```
Local update time:  2026-04-16 10:30:00
Server time:        2026-04-16 10:30:05
→ Server version wins
```

### Future Enhancements

- Field-level merge (reconcile individual fields)
- Server versioning (track change history)
- Operational transformation (3-way merge)
- User notification for conflicts

---

## 7. Data Sync Strategy

### Pull Sync (Server → Local)

**Direction:** Backend → Device

**Data synced:**
- Issues
- Jurisdictions
- Categories

**Implementation:**
```
Fetch all data from API
→ UPSERT (insert or update) into Room
→ Timestamp-based incremental sync (optional)
```

### Push Sync (Local → Server)

**Direction:** Device → Backend

**Data synced:**
- Issues with `syncState = PENDING`
- Associated images
- Activity log entries

**Implementation:**
```
Query all PENDING issues from Room
→ Upload with images
→ Update syncState on success
```

### Sync Frequency

| Type | Trigger | Frequency |
|------|---------|-----------|
| **Pull** | App startup, periodic | Every 15 mins (configurable) |
| **Push** | After issue creation, periodic | Every 5 mins (configurable) |
| **Manual** | User action | On-demand via button |

---

## 8. Image Handling

### Offline Flow

1. User selects image from camera/gallery
2. Save to local storage: `/storage/emulated/0/app/issues/{issueId}/`
3. Store reference in `LocalIssueImage.localPath`
4. UI displays image from local path

### Sync Flow

```
1. Detect PENDING image (localPath populated, remoteUrl null)
   ↓
2. Upload to backend API
   ↓
3. Receive remoteUrl from server
   ↓
4. Update LocalIssueImage:
   - localPath → keep for offline access
   - remoteUrl → populate with server URL
   - syncState → SYNCED
   ↓
5. Clean up local file after TTL (optional)
```

### Storage Management

| Action | Condition | Storage |
|--------|-----------|---------|
| **Create** | User captures image | Local file |
| **Available offline** | localPath populated | Yes |
| **Synced** | remoteUrl available | Server + Optional local |
| **Delete** | SYNCED + TTL expired | Optional cleanup |

---

## 9. Repository Pattern

### Implementation Overview

```kotlin
class IssueRepository(private val dao: IssueDao, private val api: ApiService) {
    
    // Always read from local DB
    fun getIssues(): Flow<List<LocalIssue>> {
        return dao.getAllIssues()
    }
    
    // Write locally first, then queue sync
    suspend fun createIssue(issue: LocalIssue) {
        val localIssue = issue.copy(
            syncState = SyncState.PENDING.name,
            isDirty = true
        )
        dao.insert(localIssue)
        
        // Trigger sync worker
        enqueueSyncWork()
    }
    
    // Update local issue
    suspend fun updateIssue(issue: LocalIssue) {
        val updated = issue.copy(
            syncState = SyncState.PENDING.name,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        enqueueSyncWork()
    }
    
    // Pull sync: fetch and UPSERT
    suspend fun pullSync() {
        try {
            val remoteIssues = api.getIssues()
            remoteIssues.forEach { remoteIssue ->
                dao.upsert(remoteIssue.toLocalEntity())
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Push sync: upload pending data
    suspend fun pushSync() {
        val pendingIssues = dao.getPendingIssues()
        pendingIssues.forEach { issue ->
            try {
                val response = api.createIssue(issue.toServerEntity())
                // Update with server ID if needed
                dao.update(issue.copy(syncState = SyncState.SYNCED.name))
            } catch (e: Exception) {
                dao.update(issue.copy(syncState = SyncState.FAILED.name))
            }
        }
    }
}
```

---

## 10. Best Practices

### Storage
- ✅ Store minimal required data locally
- ✅ Use appropriate data types (Long for timestamps, String for UUIDs)
- ✅ Keep images in separate directory, use relative paths

### Sync
- ✅ Always read from local DB first
- ✅ Use background workers (WorkManager) for sync
- ✅ Implement exponential backoff for retries
- ✅ Include sync state in all entities

### Offline Support
- ✅ Generate UUIDs on client (java.util.UUID)
- ✅ Use local timestamps for ordering
- ✅ Don't rely on server responses for initial UX

### Performance
- ✅ Use Flow for reactive UI updates
- ✅ Batch sync operations
- ✅ Limit local database size with cleanup policies

---

## 11. Future Enhancements

- Delta sync (only fetch changed records)
- Smart local cache invalidation
- Image compression before upload
- Queue priority for sync (critical issues first)
- Offline-first analytics
- Background upload with file size optimization
