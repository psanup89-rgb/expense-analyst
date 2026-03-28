## skill: new-domain-entity
agent: DataAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [room, hilt, domain, migration, architecture, clean-architecture]

# Adding a New Domain Entity End-to-End

## When to use this

When a new feature requires a new persisted concept (e.g. Bill, Budget, Receipt). This skill covers the full chain from domain model to UI-ready repository, including the DB migration and DI wiring. Missing any step causes KSP build failures or runtime crashes.

## What to do

Follow these steps **in order**. Each layer depends on the one before it.

**Step 1 — Domain model** (`domain/model/<Model>.kt`)
```kotlin
data class Foo(
    val id: Long = 0,
    val name: String,
    val status: FooStatus,           // add enum to Enums.kt if needed
    val createdAtMillis: Long,
    val isDeleted: Boolean = false   // always include soft-delete flag
)
```

**Step 2 — Repository interface** (`domain/repository/FooRepository.kt`)
```kotlin
interface FooRepository {
    fun getFoos(): Flow<List<Foo>>
    suspend fun saveFoo(foo: Foo): Long
    suspend fun updateFoo(foo: Foo)
    suspend fun softDeleteFoo(id: Long)
}
```

**Step 3 — Room entity** (`data/local/entity/FooEntity.kt`)
```kotlin
@Entity(tableName = "foos")
data class FooEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "status") val status: String,     // store enums as String
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
```

**Step 4 — DAO** (`data/local/dao/FooDao.kt`)
```kotlin
@Dao
interface FooDao {
    @Query("SELECT * FROM foos WHERE is_deleted = 0")
    fun getAll(): Flow<List<FooEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FooEntity): Long
    @Update
    suspend fun update(entity: FooEntity)
    @Query("UPDATE foos SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
```

**Step 5 — DB migration + entity registration** (`data/local/ExpenseAnalystDatabase.kt`)
```kotlin
@Database(
    entities = [..., FooEntity::class],  // add here
    version = N+1,                        // bump version
    ...
)
abstract class ExpenseAnalystDatabase : RoomDatabase() {
    abstract fun fooDao(): FooDao         // add abstract DAO

    companion object {
        private val MIGRATION_N_N1 = object : Migration(N, N+1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS foos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        status TEXT NOT NULL,
                        created_at_millis INTEGER NOT NULL,
                        is_deleted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
        // add MIGRATION_N_N1 to addMigrations(...)
    }
}
```

**Step 6 — Repository implementation** (`data/repository/FooRepositoryImpl.kt`)
```kotlin
@Singleton
class FooRepositoryImpl @Inject constructor(private val dao: FooDao) : FooRepository {
    override fun getFoos(): Flow<List<Foo>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }
    override suspend fun saveFoo(foo: Foo): Long = dao.upsert(foo.toEntity())
    override suspend fun updateFoo(foo: Foo) = dao.update(foo.toEntity())
    override suspend fun softDeleteFoo(id: Long) = dao.softDelete(id)
}
// add toDomain() / toEntity() extension functions
```

**Step 7 — DI wiring**

In `data/di/DatabaseModule.kt`:
```kotlin
@Provides @Singleton
fun provideFooDao(db: ExpenseAnalystDatabase): FooDao = db.fooDao()
```

In `data/di/RepositoryModule.kt`:
```kotlin
@Binds @Singleton
abstract fun bindFooRepository(impl: FooRepositoryImpl): FooRepository
```

**Step 8 — Build verification**
```bash
./gradlew clean assembleDebug
```
Check `data/schemas/` for the new `N+1.json` schema file — it should be auto-generated.

## Example

Bills entity added in commit `9516af4`:
- `domain/model/Bill.kt` (step 1)
- `domain/repository/BillRepository.kt` (step 2)
- `data/local/entity/BillEntity.kt` (step 3)
- `data/local/dao/BillDao.kt` (step 4)
- `ExpenseAnalystDatabase.kt` v10 + MIGRATION_9_10 (step 5)
- `data/repository/BillRepositoryImpl.kt` (step 6)
- `DatabaseModule` + `RepositoryModule` updated (step 7)

## Pitfalls

- **Forgetting to add the entity to `@Database(entities = [...])`** — Room silently ignores the DAO and the table is never created. Only caught at runtime when the query fails.
- **Forgetting `addMigrations(MIGRATION_N_N1)`** — the migration object exists but is never run. Room uses `fallbackToDestructiveMigration` if configured (this project does NOT use it — it will crash).
- **Storing enums as their Kotlin type** — Room does not know how to store enums. Always store as `String` in entities and convert in the mapper.
- **Forgetting `./gradlew clean`** — KSP generates Hilt and Room code. Incremental builds after adding new classes produce stale symbol errors. Always `clean`.
- **Smart cast on nullable cross-module properties** — see `ksp-cross-module-smart-cast.md`.

## Related skills

- `ksp-cross-module-smart-cast.md`
- `build-full-clean.md`
