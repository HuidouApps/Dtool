package dev.huidou.util.provider

import android.content.Context
import android.util.Log
import dev.huidou.util.service.DatabaseServiceClient

/**
 * 通用数据库客户端 - 通过 AIDL DatabaseService 访问 aqq 的数据库
 * 支持完整的数据库管理功能
 */
class UniversalDatabaseClient(private val context: Context) {
    
    companion object {
        private val TAG = "UniversalDBClient"
    }
    
    private val serviceClient = DatabaseServiceClient(context)
    
    init {
        Log.d(TAG, "=== UniversalDatabaseClient created ===")
        Log.d(TAG, "Context: ${context.javaClass.name}")
        // 自动绑定服务
        Log.d(TAG, "Calling bindService...")
        serviceClient.bindService()
        Log.d(TAG, "bindService called, waiting for connection...")
    }
    
    /**
     * 检查服务连接状态
     */
    fun isServiceConnected(): Boolean {
        return serviceClient.isConnected()
    }
    
    /**
     * 解绑服务(在适当的时候调用,如 Activity onDestroy)
     */
    fun unbind() {
        serviceClient.unbindService()
    }
    
    // ==================== 数据库操作 ====================
    
    /**
     * 获取所有数据库列表
     */
    fun getDatabases(): List<Map<String, Any?>> {
        return try {
            val dbNames = serviceClient.getDatabases()
            dbNames.map { dbName ->
                mapOf(
                    "name" to dbName,
                    "size" to 0L,  // AIDL 不直接提供文件大小,需要时可以扩展
                    "last_modified" to 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying databases", e)
            emptyList()
        }
    }
    
    /**
     * 创建数据库
     */
    fun createDatabase(dbName: String): Boolean {
        return try {
            serviceClient.createDatabase(dbName)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database: $dbName", e)
            false
        }
    }
    
    /**
     * 删除数据库
     */
    fun deleteDatabase(dbName: String): Boolean {
        return try {
            serviceClient.deleteDatabase(dbName)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting database: $dbName", e)
            false
        }
    }
    
    // ==================== 表操作 ====================
        
    /**
     * 获取指定数据库的所有表
     */
    fun getTables(dbName: String): List<String> {
        return try {
            serviceClient.getTables(dbName)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying tables in database: $dbName", e)
            emptyList()
        }
    }
        
    /**
     * 创建表
     * @param dbName 数据库名
     * @param tableName 表名
     * @param columns 列定义,格式:"id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, age INTEGER"
     */
    fun createTable(dbName: String, tableName: String, columns: String): Boolean {
        return try {
            serviceClient.createTable(dbName, tableName, columns)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating table: $tableName", e)
            false
        }
    }
        
    /**
     * 删除表
     */
    fun dropTable(dbName: String, tableName: String): Boolean {
        return try {
            serviceClient.dropTable(dbName, tableName)
        } catch (e: Exception) {
            Log.e(TAG, "Error dropping table: $tableName", e)
            false
        }
    }
    
    // ==================== 表结构查询 ====================
    
    /**
     * 获取表结构
     */
    fun getTableStructure(dbName: String, tableName: String): List<Map<String, Any?>> {
        return try {
            serviceClient.getTableStructure(dbName, tableName)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying table structure", e)
            emptyList()
        }
    }
    
    // ==================== 数据操作 ====================
    
    /**
     * 查询数据
     */
    fun queryData(
        dbName: String,
        tableName: String,
        columns: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        orderBy: String? = null
    ): List<Map<String, Any?>> {
        return try {
            serviceClient.queryData(dbName, tableName, columns, selection, selectionArgs, orderBy)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying data from $tableName", e)
            emptyList()
        }
    }
    
    /**
     * 插入数据
     */
    fun insertData(dbName: String, tableName: String, values: Map<String, Any?>): Boolean {
        return try {
            serviceClient.insertData(dbName, tableName, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting data into $tableName", e)
            false
        }
    }
    
    /**
     * 更新数据
     */
    fun updateData(
        dbName: String,
        tableName: String,
        values: Map<String, Any?>,
        whereClause: String,
        whereArgs: Array<String>
    ): Boolean {
        return try {
            serviceClient.updateData(dbName, tableName, values, whereClause, whereArgs)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating data in $tableName", e)
            false
        }
    }
    
    /**
     * 删除数据
     */
    fun deleteData(
        dbName: String,
        tableName: String,
        whereClause: String,
        whereArgs: Array<String>
    ): Boolean {
        return try {
            serviceClient.deleteData(dbName, tableName, whereClause, whereArgs)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting data from $tableName", e)
            false
        }
    }
}
