package dev.huidou.util.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import dev.huidou.db.aidl.IDatabaseService
import org.json.JSONArray
import org.json.JSONObject

/**
 * 数据库服务客户端 - 通过 AIDL 连接到 aqq 应用的 DatabaseService
 */
class DatabaseServiceClient(private val context: Context) {
    
    companion object {
        private const val TAG = "DatabaseServiceClient"
        private const val SERVICE_ACTION = "dev.huidou.db.action.BIND_DATABASE_SERVICE"
        private const val SERVICE_PACKAGE = "dev.huidou.db"
    }
    
    private var databaseService: IDatabaseService? = null
    private var serviceConnection: ServiceConnection? = null
    private var isBound = false
    
    /**
     * 绑定服务
     */
    fun bindService(onConnected: (() -> Unit)? = null) {
        Log.d(TAG, "=== bindService called ===")
        Log.d(TAG, "isBound: $isBound")
        
        if (isBound) {
            Log.d(TAG, "Already bound, calling onConnected")
            onConnected?.invoke()
            return
        }
        
        val intent = Intent(SERVICE_ACTION).apply {
            setPackage(SERVICE_PACKAGE)
        }
        
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent package: ${intent.`package`}")
        
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(TAG, "=== onServiceConnected ===")
                Log.d(TAG, "ComponentName: $name")
                Log.d(TAG, "IBinder: $service")
                databaseService = IDatabaseService.Stub.asInterface(service)
                isBound = true
                Log.d(TAG, "Service bound successfully")
                onConnected?.invoke()
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(TAG, "=== onServiceDisconnected ===")
                Log.w(TAG, "ComponentName: $name")
                databaseService = null
                isBound = false
            }
            
            override fun onBindingDied(name: ComponentName?) {
                Log.e(TAG, "=== onBindingDied ===")
                Log.e(TAG, "ComponentName: $name")
                databaseService = null
                isBound = false
            }
            
            override fun onNullBinding(name: ComponentName?) {
                Log.e(TAG, "=== onNullBinding ===")
                Log.e(TAG, "ComponentName: $name")
                Log.e(TAG, "The service was not found or could not be bound")
                databaseService = null
                isBound = false
            }
        }
        
        Log.d(TAG, "Calling context.bindService...")
        val result = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "bindService result: $result")
        
        if (!result) {
            Log.e(TAG, "=== Failed to bind service ===")
            Log.e(TAG, "This usually means:")
            Log.e(TAG, "1. The service is not declared in the manifest")
            Log.e(TAG, "2. The intent filter doesn't match")
            Log.e(TAG, "3. The package name is incorrect")
            Log.e(TAG, "4. The target app is not installed")
        } else {
            Log.d(TAG, "bindService returned true, waiting for connection...")
        }
    }
    
    /**
     * 解绑服务
     */
    fun unbindService() {
        if (isBound && serviceConnection != null) {
            try {
                context.unbindService(serviceConnection!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service", e)
            }
            isBound = false
            databaseService = null
            serviceConnection = null
        }
    }
    
    /**
     * 检查服务是否已连接
     */
    fun isConnected(): Boolean {
        return isBound && databaseService != null
    }
    
    // ==================== 数据库操作 ====================
    
    /**
     * 获取所有数据库列表
     */
    fun getDatabases(): List<String> {
        return try {
            databaseService?.getDatabases() ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting databases", e)
            emptyList()
        }
    }
    
    /**
     * 创建数据库
     */
    fun createDatabase(dbName: String): Boolean {
        return try {
            databaseService?.createDatabase(dbName) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Error creating database: $dbName", e)
            false
        }
    }
    
    /**
     * 删除数据库
     */
    fun deleteDatabase(dbName: String): Boolean {
        return try {
            databaseService?.deleteDatabase(dbName) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Error deleting database: $dbName", e)
            false
        }
    }

    /**
     * 重命名数据库
     */
    fun renameDatabase(oldName: String, newName: String): Boolean {
        return try {
            databaseService?.renameDatabase(oldName, newName) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Error renaming database: $oldName to $newName", e)
            false
        }
    }

    /**
     * 获取所有数据库文件的总大小
     */
    fun getDatabasesTotalSize(): Long {
        return try {
            databaseService?.getDatabasesTotalSize() ?: 0L
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting databases total size", e)
            0L
        }
    }

    // ==================== 表操作 ====================
    
    /**
     * 获取指定数据库的所有表
     */
    fun getTables(dbName: String): List<String> {
        return try {
            databaseService?.getTables(dbName) ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting tables in database: $dbName", e)
            emptyList()
        }
    }
    
    /**
     * 创建表
     */
    fun createTable(dbName: String, tableName: String, columns: String): Boolean {
        return try {
            databaseService?.createTable(dbName, tableName, columns) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Error creating table: $tableName", e)
            false
        }
    }
    
    /**
     * 删除表
     */
    fun dropTable(dbName: String, tableName: String): Boolean {
        return try {
            databaseService?.dropTable(dbName, tableName) ?: false
        } catch (e: RemoteException) {
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
            val jsonStr = databaseService?.getTableStructure(dbName, tableName) ?: "[]"
            val jsonArray = JSONArray(jsonStr)
            val columns = mutableListOf<Map<String, Any?>>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val col = mapOf(
                    "cid" to obj.getInt("cid"),
                    "name" to obj.getString("name"),
                    "type" to obj.getString("type"),
                    "notnull" to obj.getInt("notnull"),
                    "dflt_value" to if (obj.isNull("dflt_value")) null else obj.getString("dflt_value"),
                    "pk" to obj.getInt("pk"),
                    "autoincrement" to obj.optBoolean("autoincrement", false)
                )
                columns.add(col)
            }
            columns
        } catch (e: Exception) {
            Log.e(TAG, "Error getting table structure", e)
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
            val columnsStr = columns?.joinToString(",")
            val argsStr = selectionArgs?.joinToString(",")
            
            val jsonStr = databaseService?.queryData(
                dbName, tableName, columnsStr, selection, argsStr, orderBy
            ) ?: "[]"
            
            val jsonArray = JSONArray(jsonStr)
            val data = mutableListOf<Map<String, Any?>>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val row = mutableMapOf<String, Any?>()
                
                obj.keys().forEach { key ->
                    val value = obj.get(key)
                    row[key] = if (value == JSONObject.NULL) null else value
                }
                data.add(row)
            }
            data
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
            val json = JSONObject()
            values.forEach { (key, value) ->
                json.put(key, value)
            }
            databaseService?.insertData(dbName, tableName, json.toString()) ?: false
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
            val json = JSONObject()
            values.forEach { (key, value) ->
                json.put(key, value)
            }
            val argsStr = whereArgs.joinToString(",")
            databaseService?.updateData(dbName, tableName, json.toString(), whereClause, argsStr) ?: false
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
            val argsStr = whereArgs.joinToString(",")
            databaseService?.deleteData(dbName, tableName, whereClause, argsStr) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting data from $tableName", e)
            false
        }
    }
}
