package dev.huidou.util.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * 数据库客户端 - 通过 ContentProvider 访问 aqq 的数据库
 */
class DatabaseClient(private val context: Context) {
    
    companion object {
        const val AUTHORITY = "dev.huidou.db.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/users")
        
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_AGE = "age"
        const val COLUMN_CITY = "city"
        
        private val TAG = "DatabaseClient"
    }
    
    /**
     * 查询所有用户
     */
    fun getAllUsers(): List<Map<String, Any?>> {
        return try {
            val cursor: Cursor? = context.contentResolver.query(
                CONTENT_URI,
                null,
                null,
                null,
                null
            )
            
            val users = mutableListOf<Map<String, Any?>>()
            cursor?.use {
                while (it.moveToNext()) {
                    val user = mapOf(
                        COLUMN_ID to it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                        COLUMN_NAME to it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        COLUMN_EMAIL to it.getString(it.getColumnIndexOrThrow(COLUMN_EMAIL)),
                        COLUMN_AGE to it.getInt(it.getColumnIndexOrThrow(COLUMN_AGE)),
                        COLUMN_CITY to it.getString(it.getColumnIndexOrThrow(COLUMN_CITY))
                    )
                    users.add(user)
                }
            }
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error querying users", e)
            emptyList()
        }
    }
    
    /**
     * 插入用户
     */
    fun insertUser(name: String, email: String, age: Int, city: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COLUMN_NAME, name)
                put(COLUMN_EMAIL, email)
                put(COLUMN_AGE, age)
                put(COLUMN_CITY, city)
            }
            
            val uri = context.contentResolver.insert(CONTENT_URI, values)
            uri != null
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user", e)
            false
        }
    }
    
    /**
     * 更新用户
     */
    fun updateUser(id: Int, name: String?, email: String?, age: Int?, city: String?): Boolean {
        return try {
            val values = ContentValues().apply {
                name?.let { put(COLUMN_NAME, it) }
                email?.let { put(COLUMN_EMAIL, it) }
                age?.let { put(COLUMN_AGE, it) }
                city?.let { put(COLUMN_CITY, it) }
            }
            
            val uri = Uri.withAppendedPath(CONTENT_URI, id.toString())
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            rowsUpdated > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            false
        }
    }
    
    /**
     * 删除用户
     */
    fun deleteUser(id: Int): Boolean {
        return try {
            val uri = Uri.withAppendedPath(CONTENT_URI, id.toString())
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            false
        }
    }
    
    /**
     * 删除所有用户
     */
    fun deleteAllUsers(): Boolean {
        return try {
            val rowsDeleted = context.contentResolver.delete(CONTENT_URI, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all users", e)
            false
        }
    }
}
