package com.example.hostelfix.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val photoUrl: String?
)

@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentEmail: String,
    val title: String,
    val description: String,
    val status: String, // "OPEN", "IN_PROGRESS", "RESOLVED"
    val timeAgo: String
)

@Dao
interface HostelDao {
    // User SQL Queries
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Issue SQL Queries with Flow (Unit IV)
    @Query("SELECT * FROM issues WHERE studentEmail = :email ORDER BY id DESC")
    fun getIssuesForStudent(email: String): Flow<List<IssueEntity>>

    @Query("SELECT COUNT(*) FROM issues WHERE studentEmail = :email AND status = 'OPEN'")
    fun getOpenCount(email: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM issues WHERE studentEmail = :email AND status = 'IN_PROGRESS'")
    fun getInProgressCount(email: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM issues WHERE studentEmail = :email AND status = 'RESOLVED'")
    fun getResolvedCount(email: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity)
}

@Database(entities = [UserEntity::class, IssueEntity::class], version = 1, exportSchema = false)
abstract class HostelDatabase : RoomDatabase() {
    abstract fun hostelDao(): HostelDao

    companion object {
        @Volatile
        private var INSTANCE: HostelDatabase? = null

        fun getDatabase(context: android.content.Context): HostelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HostelDatabase::class.java,
                    "hostel_fix_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}