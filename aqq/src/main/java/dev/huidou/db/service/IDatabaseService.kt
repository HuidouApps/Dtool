package dev.huidou.db.service

import dev.huidou.database.entity.User

/**
 * 数据库服务接口 - 用于跨应用通信
 */
interface IDatabaseService {
    fun getAllUsers(): List<User>
    suspend fun getUserById(userId: Int): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun deleteAllUsers(): Boolean
}
