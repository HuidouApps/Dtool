package dev.huidou.db.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dev.huidou.database.AppDatabase
import dev.huidou.database.entity.User
import dev.huidou.database.repository.DatabaseRepository
import kotlinx.coroutines.runBlocking

class DatabaseService : Service(), IDatabaseService {
    
    private val binder = LocalBinder()
    private lateinit var repository: DatabaseRepository
    
    inner class LocalBinder : Binder() {
        fun getService(): DatabaseService = this@DatabaseService
    }
    
    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = DatabaseRepository(database)
    }
    
    override fun getAllUsers(): List<User> {
        return runBlocking {
            var users: List<User> = emptyList()
            repository.getAllUsers().collect { userList ->
                users = userList
            }
            users
        }
    }
    
    override suspend fun getUserById(userId: Int): User? {
        return repository.getUserById(userId)
    }
    
    override suspend fun insertUser(user: User): Boolean {
        return try {
            repository.insertUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun updateUser(user: User): Boolean {
        return try {
            repository.updateUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deleteUser(userId: Int): Boolean {
        return try {
            val user = repository.getUserById(userId)
            if (user != null) {
                repository.deleteUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deleteAllUsers(): Boolean {
        return try {
            repository.deleteAllUsers()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
