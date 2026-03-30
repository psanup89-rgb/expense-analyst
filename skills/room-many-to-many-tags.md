---
skill: room-many-to-many-tags
agent: DataAgent
created: 2026-03-30
last_used: 2026-03-30
tags: [room, android, many-to-many, junction, relation, migration, tags]

# Room Many-to-Many Relationship (Tags Pattern)

## When to use this

When adding a reusable label/tag system to an existing Room entity where one record can have many tags and one tag can belong to many records. Covers the full stack: domain model, junction entity, DAO, `@Relation` with `@Junction`, migration, and repository wiring.

## What to do

### 1. Domain model
```kotlin
// domain/model/Tag.kt
data class Tag(val id: Long = 0, val name: String)
```

Add to the parent domain model:
```kotlin
// Remove: val note: String? = null
val tags: List<Tag> = emptyList()
```

### 2. Room entities

**Tag entity** — unique index on `name` to enforce deduplication:
```kotlin
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
```

**Junction table** — composite PK, FK to both parent tables with CASCADE delete:
```kotlin
@Entity(
    tableName = "expense_tags",
    primaryKeys = ["expense_id", "tag_id"],
    foreignKeys = [
        ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"],
            childColumns = ["expense_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"],
            childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["tag_id"])]  // index the FK for reverse lookups
)
data class ExpenseTagCrossRef(
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)
```

### 3. DAO
```kotlin
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<ExpenseTagCrossRef>)

    @Query("DELETE FROM expense_tags WHERE expense_id = :expenseId")
    suspend fun deleteTagsForExpense(expenseId: Long)

    @Transaction
    suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>) {
        deleteTagsForExpense(expenseId)
        insertCrossRefs(tagIds.map { ExpenseTagCrossRef(expenseId, it) })
    }
}
```

**Insert-or-get pattern** for `createTag()`:
```kotlin
suspend fun createTag(name: String): Tag {
    val trimmed = name.trim()
    val id = tagDao.insertTag(TagEntity(name = trimmed))
    return if (id == -1L) {
        // INSERT IGNORE — row already exists, fetch it
        tagDao.getTagByName(trimmed)!!.toDomain()
    } else {
        Tag(id = id, name = trimmed)
    }
}
```

### 4. `@Relation` with `@Junction` in the embedding class
```kotlin
// data/local/relation/ExpenseWithCategory.kt
data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseTagCrossRef::class,
            parentColumn = "expense_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
```

### 5. Mapper
```kotlin
fun ExpenseWithCategory.toDomain(): Expense = expense.toDomain(
    category = category.toDomain(),
    tags = tags.map { it.toDomain() }
)
```

### 6. Database registration
```kotlin
@Database(entities = [
    ExpenseEntity::class,
    CategoryEntity::class,
    // ... existing ...
    TagEntity::class,           // add
    ExpenseTagCrossRef::class,  // add
], version = 11)
abstract class ExpenseAnalystDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    // ...
}
```

### 7. Migration SQL
```sql
-- Create tags table
CREATE TABLE IF NOT EXISTS `tags` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`);

-- Create junction table
CREATE TABLE IF NOT EXISTS `expense_tags` (
    `expense_id` INTEGER NOT NULL,
    `tag_id` INTEGER NOT NULL,
    PRIMARY KEY(`expense_id`, `tag_id`),
    FOREIGN KEY(`expense_id`) REFERENCES `expenses`(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_expense_tags_tag_id` ON `expense_tags` (`tag_id`);

-- Pre-seed default tags
INSERT OR IGNORE INTO tags (name) VALUES
    ('Recurring'), ('One-time'), ('Reimbursable'), ('Tax Deductible'),
    ('Personal'), ('Business'), ('Shared'), ('Subscription'), ('Essential');

-- Migrate existing note values to tags (optional)
INSERT OR IGNORE INTO tags (name)
    SELECT DISTINCT TRIM(note) FROM expenses
    WHERE note IS NOT NULL AND TRIM(note) != '';
INSERT OR IGNORE INTO expense_tags (expense_id, tag_id)
    SELECT e.id, t.id FROM expenses e
    INNER JOIN tags t ON TRIM(e.note) = t.name
    WHERE e.note IS NOT NULL AND TRIM(e.note) != '';
```

### 8. Wire DI
```kotlin
// DatabaseModule
@Provides fun provideTagDao(db: ExpenseAnalystDatabase): TagDao = db.tagDao()

// RepositoryModule
@Binds abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
```

### 9. Call `setTagsForExpense` in repository after save/update
```kotlin
// ExpenseRepositoryImpl
suspend fun addExpense(expense: Expense): Long {
    val id = expenseDao.insert(expense.toEntity())
    tagDao.setTagsForExpense(id, expense.tags.map { it.id })
    return id
}
```

## Example

This pattern was used to replace `note: String?` on `Expense` with a many-to-many `tags: List<Tag>` in DB migration v10 → v11.

## Pitfalls

- **Column name mismatch in Junction**: `parentColumn` and `entityColumn` in `@Junction` must match the `@ColumnInfo(name = ...)` values in the junction entity exactly, not the Kotlin property names. Room generates incorrect SQL silently if they mismatch.
- **`@Embedded` + `@Relation` requires `@Transaction` on DAO queries** that return the embedding class. Without `@Transaction`, Room may return partial results.
- **Insert-or-ignore returns -1L on conflict**, not the existing row's id. Always follow with a SELECT by name when the result is -1.
- **Pre-seeding in two places**: `MIGRATION_X_Y` (for existing users upgrading) AND `SeedDatabaseCallback.onCreate` (for fresh installs). Missing either means one path has no default tags.
- **`ExpenseEntity` should NOT have a `note` column removed from Room's perspective** — if the SQLite column still exists, Room ignores it at runtime (no crash). Only remove it from the entity class; let the column remain in SQLite for the migration period.

## Related skills

- `new-domain-entity.md` — end-to-end checklist for adding any new Room entity
- `build-full-clean.md` — required after any Room schema change
