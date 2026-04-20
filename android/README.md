

### Folder Structure

```
com/sevasetu/
├── data/
│   ├── local/        # Room (Entity, DAO, DB)
│   ├── remote/       # API calls
│   └── repository/   # Combines local + remote
│
├── ui/
│   ├── screen/
│   ├── theme/
│   └── common/
│
├── sync/
│   └── SyncWorker.kt
│
├── network/
│   └── ApiService.kt
│
├── database/
│   └── AppDatabase.kt
│
└── utils/
```
