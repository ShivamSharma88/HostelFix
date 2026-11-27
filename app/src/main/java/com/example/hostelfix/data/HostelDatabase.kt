package com.example.hostelfix.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String,
    val name: String,
    val hostelNumber: String = "",
    val roomNumber: String = "",
    val phoneNumber: String = "",
    val department: String = "",
    val linkedGoogleEmail: String? = null,
    val photoUrl: String? = null
)

@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val syncKey: String = UUID.randomUUID().toString(),
    val studentEmail: String,
    val title: String,
    val description: String,
    val category: String, // "ACADEMICAL" or "MAINTENANCE"
    val status: String,   // "OPEN", "IN_PROGRESS", "RESOLVED"
    val timeAgo: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun canBeDeleted(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val oneHourMillis = 60 * 60 * 1000L
        return (nowMillis - createdAt) in 0..oneHourMillis
    }
}

@Dao
interface HostelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username)) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username)) AND password = :password LIMIT 1")
    suspend fun loginWithCredentials(username: String, password: String): UserEntity?

    @Query("UPDATE users SET linkedGoogleEmail = :googleEmail, photoUrl = :photoUrl WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username))")
    suspend fun linkGoogleAccount(username: String, googleEmail: String, photoUrl: String?)

    @Query("SELECT * FROM users WHERE LOWER(TRIM(linkedGoogleEmail)) = LOWER(TRIM(:email)) LIMIT 1")
    suspend fun getUserByLinkedGoogleEmail(email: String): UserEntity?

    @Query(
        """
        UPDATE users 
        SET name = :name,
            hostelNumber = :hostelNumber,
            roomNumber = :roomNumber,
            phoneNumber = :phoneNumber,
            department = :department,
            linkedGoogleEmail = :linkedGoogleEmail
        WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username))
        """
    )
    suspend fun updateUserDetails(
        username: String,
        name: String,
        hostelNumber: String,
        roomNumber: String,
        phoneNumber: String,
        department: String,
        linkedGoogleEmail: String?
    )

    @Query("SELECT * FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) ORDER BY id DESC")
    fun getIssuesForStudent(email: String): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) ORDER BY id ASC")
    suspend fun getIssuesOnceForStudent(email: String): List<IssueEntity>

    @Query("SELECT * FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) AND TRIM(syncKey) = TRIM(:syncKey) LIMIT 1")
    suspend fun getIssueBySyncKey(email: String, syncKey: String): IssueEntity?

    @Query("SELECT * FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) AND UPPER(TRIM(category)) = UPPER(TRIM(:category)) ORDER BY id DESC")
    fun getIssuesByCategory(email: String, category: String): Flow<List<IssueEntity>>

    @Query("SELECT COUNT(*) FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) AND UPPER(TRIM(status)) = 'OPEN'")
    fun getOpenCount(email: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) AND UPPER(TRIM(status)) = 'IN_PROGRESS'")
    fun getInProgressCount(email: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM issues WHERE LOWER(TRIM(studentEmail)) = LOWER(TRIM(:email)) AND UPPER(TRIM(status)) = 'RESOLVED'")
    fun getResolvedCount(email: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity): Long

    @Delete
    suspend fun deleteIssue(issue: IssueEntity)
}

@Database(entities = [UserEntity::class, IssueEntity::class], version = 5, exportSchema = false)
abstract class HostelDatabase : RoomDatabase() {
    abstract fun hostelDao(): HostelDao

    companion object {
        @Volatile
        private var INSTANCE: HostelDatabase? = null

        fun getDatabase(context: Context): HostelDatabase {
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