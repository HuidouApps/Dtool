package dev.huidou.db.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import dev.huidou.database.AppDatabase
import dev.huidou.database.entity.User
import dev.huidou.database.repository.DatabaseRepository
import kotlinx.coroutines.runBlocking

class DatabaseContentProvider : ContentProvider() {
    
    companion object {
        const val AUTHORITY = "dev.huidou.db.provider"
        const val PATH_USERS = "users"
        
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_USERS")
        
        private const val USERS = 1
        private const val USERS_ID = 2
        
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_USERS, USERS)
            addURI(AUTHORITY, "$PATH_USERS/#", USERS_ID)
        }
    }
    
    private lateinit var repository: DatabaseRepository
    
    override fun onCreate(): Boolean {
        val context = context ?: return false
        val database = AppDatabase.getDatabase(context)
        repository = DatabaseRepository(database)
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            USERS -> {
                // 查询所有用户
                val users = runBlocking {
                    var userList: List<User> = emptyList()
                    repository.getAllUsers().collect { users ->
                        userList = users
                    }
                    userList
                }
                
                val columns = projection ?: arrayOf("id", "name", "email", "age", "city")
                val cursor = MatrixCursor(columns)
                
                users.forEach { user ->
                    val row = cursor.newRow()
                    row.add("id", user.id)
                    row.add("name", user.name)
                    row.add("email", user.email)
                    row.add("age", user.age)
                    row.add("city", user.city)
                }
                
                cursor
            }
            USERS_ID -> {
                // 查询单个用户
                val userId = uri.lastPathSegment?.toIntOrNull() ?: return null
                val user = runBlocking { repository.getUserById(userId) }
                
                if (user != null) {
                    val columns = projection ?: arrayOf("id", "name", "email", "age", "city")
                    val cursor = MatrixCursor(columns)
                    val row = cursor.newRow()
                    row.add("id", user.id)
                    row.add("name", user.name)
                    row.add("email", user.email)
                    row.add("age", user.age)
                    row.add("city", user.city)
                    cursor
                } else {
                    null
                }
            }
            else -> null
        }
    }
    
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            USERS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_USERS"
            USERS_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_USERS"
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != USERS) {
            throw IllegalArgumentException("Invalid URI: $uri")
        }
        
        val name = values?.getAsString("name") ?: return null
        val email = values.getAsString("email") ?: return null
        val age = values.getAsInteger("age") ?: 0
        val city = values.getAsString("city") ?: ""
        
        val user = User(name = name, email = email, age = age, city = city)
        
        runBlocking {
            repository.insertUser(user)
        }
        
        context?.contentResolver?.notifyChange(uri, null)
        return CONTENT_URI
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            USERS -> {
                // 删除所有用户
                runBlocking {
                    repository.deleteAllUsers()
                }
                context?.contentResolver?.notifyChange(uri, null)
                1
            }
            USERS_ID -> {
                // 删除单个用户
                val userId = uri.lastPathSegment?.toIntOrNull() ?: return 0
                runBlocking {
                    val user = repository.getUserById(userId)
                    if (user != null) {
                        repository.deleteUser(user)
                        context?.contentResolver?.notifyChange(uri, null)
                        1
                    } else {
                        0
                    }
                }
            }
            else -> 0
        }
    }
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        if (uriMatcher.match(uri) != USERS_ID) {
            throw IllegalArgumentException("Update only supports single user by ID")
        }
        
        val userId = uri.lastPathSegment?.toIntOrNull() ?: return 0
        
        val name = values?.getAsString("name")
        val email = values?.getAsString("email")
        val age = values?.getAsInteger("age")
        val city = values?.getAsString("city")
        
        if (name == null && email == null && age == null && city == null) {
            return 0
        }
        
        return runBlocking {
            val user = repository.getUserById(userId)
            if (user != null) {
                val updatedUser = User(
                    id = user.id,
                    name = name ?: user.name,
                    email = email ?: user.email,
                    age = age ?: user.age,
                    city = city ?: user.city
                )
                repository.updateUser(updatedUser)
                context?.contentResolver?.notifyChange(uri, null)
                1
            } else {
                0
            }
        }
    }
}
