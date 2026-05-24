package dev.huidou.database.repository

import dev.huidou.database.AppDatabase
import dev.huidou.database.entity.User
import kotlinx.coroutines.flow.Flow

class DatabaseRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    
    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }
    
    suspend fun getUserById(userId: Int): User? {
        return userDao.getUserById(userId)
    }
    
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }
    
    suspend fun insertUsers(users: List<User>) {
        userDao.insertUsers(users)
    }
    
    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
    
    suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }
}
