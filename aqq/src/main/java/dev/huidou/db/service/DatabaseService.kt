package dev.huidou.db.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dev.huidou.db.aidl.IDatabaseService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 数据库服务 - 通过 AIDL 提供跨应用数据库访问
 */
class DatabaseService : Service() {
    
    companion object {
        private const val TAG = "DatabaseService"
        const val ACTION_BIND = "dev.huidou.db.action.BIND_DATABASE_SERVICE"
    }
    
    private val binder = object : IDatabaseService.Stub() {
        
        // ==================== 数据库操作 ====================
        
        override fun getDatabases(): List<String> {
            return try {
                // 使用 Android 标准 API 获取数据库列表
                val context = this@DatabaseService
                val dbList = context.databaseList()
                    .filter { it.endsWith(".db") || it.endsWith(".sqlite") }
                    .sorted()
                
                Log.d(TAG, "Found ${dbList.size} databases: ${dbList.joinToString()}")
                dbList
            } catch (e: Exception) {
                Log.e(TAG, "Error getting databases", e)
                emptyList()
            }
        }
        
        override fun createDatabase(dbName: String): Boolean {
            return try {
                val dbFile = getDatabasePath(dbName)
                dbFile.parentFile?.mkdirs()
                val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                db.close()
                Log.d(TAG, "Database created: $dbName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error creating database: $dbName", e)
                false
            }
        }
        
        override fun deleteDatabase(dbName: String): Boolean {
            return try {
                // 必须用 this@DatabaseService 限定，否则这里会递归调用 binder 自身的
                // deleteDatabase（AIDL 方法），造成无限递归直到 StackOverflowError。
                val result = this@DatabaseService.deleteDatabase(dbName)
                Log.d(TAG, "Database deleted: $dbName, result=$result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting database: $dbName", e)
                false
            }
        }
        
        // ==================== 表操作 ====================
        
        override fun getTables(dbName: String): List<String> {
            return try {
                val dbFile = getDatabasePath(dbName)
                if (!dbFile.exists()) {
                    return emptyList()
                }
                
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                
                val cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
                    null
                )
                
                val tables = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
                cursor.close()
                db.close()
                tables
            } catch (e: Exception) {
                Log.e(TAG, "Error getting tables in database: $dbName", e)
                emptyList()
            }
        }
        
        override fun createTable(dbName: String, tableName: String, columns: String): Boolean {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                val sql = "CREATE TABLE IF NOT EXISTS `$tableName` ($columns)"
                db.execSQL(sql)
                db.close()
                Log.d(TAG, "Table created: $tableName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error creating table: $tableName", e)
                false
            }
        }
        
        override fun dropTable(dbName: String, tableName: String): Boolean {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    null, 
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                db.execSQL("DROP TABLE IF EXISTS `$tableName`")
                db.close()
                Log.d(TAG, "Table dropped: $tableName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error dropping table: $tableName", e)
                false
            }
        }
        
        // ==================== 表结构查询 ====================
        
        override fun getTableStructure(dbName: String, tableName: String): String {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                
                // 检查该表是否使用了 AUTOINCREMENT（sqlite_sequence 表中存在记录）
                val isAutoIncrement = try {
                    val seqCursor = db.rawQuery(
                        "SELECT COUNT(*) FROM sqlite_sequence WHERE name = ?",
                        arrayOf(tableName)
                    )
                    seqCursor.moveToFirst()
                    val count = seqCursor.getInt(0)
                    seqCursor.close()
                    count > 0
                } catch (e: Exception) {
                    false
                }
                
                val cursor = db.rawQuery("PRAGMA table_info(`$tableName`)", null)
                
                val result = JSONArray()
                while (cursor.moveToNext()) {
                    val pkValue = cursor.getInt(5)
                    val column = JSONObject().apply {
                        put("cid", cursor.getInt(0))
                        put("name", cursor.getString(1))
                        put("type", cursor.getString(2))
                        put("notnull", cursor.getInt(3))
                        put("dflt_value", if (cursor.isNull(4)) null else cursor.getString(4))
                        put("pk", pkValue)
                        // 只有同时满足：表有AUTOINCREMENT + 该列是INTEGER类型主键，才标记为自增列
                        put("autoincrement", isAutoIncrement && pkValue > 0 && cursor.getString(2).uppercase().contains("INTEGER"))
                    }
                    result.put(column)
                }
                cursor.close()
                db.close()
                result.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting table structure", e)
                "[]"
            }
        }
        
        // ==================== 数据操作 ====================
        
        override fun queryData(
            dbName: String,
            tableName: String,
            columns: String?,
            selection: String?,
            selectionArgs: String?,
            orderBy: String?
        ): String {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                
                val projection = columns?.split(",")?.map { it.trim() }?.toTypedArray()
                val args = selectionArgs?.split(",")?.map { it.trim() }?.toTypedArray()
                
                val cursor = db.query(
                    tableName,
                    projection,
                    selection,
                    args,
                    null,
                    null,
                    orderBy
                )
                
                val result = JSONArray()
                val columnNames = cursor.columnNames
                
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    columnNames.forEach { colName ->
                        val value = when (cursor.getType(cursor.getColumnIndexOrThrow(colName))) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(cursor.getColumnIndexOrThrow(colName))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(cursor.getColumnIndexOrThrow(colName))
                            android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(cursor.getColumnIndexOrThrow(colName))
                            android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            else -> cursor.getString(cursor.getColumnIndexOrThrow(colName))
                        }
                        row.put(colName, value)
                    }
                    result.put(row)
                }
                cursor.close()
                db.close()
                result.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error querying data", e)
                "[]"
            }
        }
        
        override fun insertData(dbName: String, tableName: String, values: String): Boolean {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    null, 
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                
                val contentValues = android.content.ContentValues()
                val json = JSONObject(values)
                json.keys().forEach { key ->
                    val value = json.get(key)
                    if (value == JSONObject.NULL) {
                        contentValues.putNull("`$key`")
                    } else {
                        when (value) {
                            is String -> contentValues.put("`$key`", value)
                            is Int -> contentValues.put("`$key`", value)
                            is Long -> contentValues.put("`$key`", value)
                            is Double -> contentValues.put("`$key`", value)
                            is Float -> contentValues.put("`$key`", value)
                            is Boolean -> contentValues.put("`$key`", value)
                            else -> contentValues.put("`$key`", value.toString())
                        }
                    }
                }
                
                val id = db.insert("`$tableName`", null, contentValues)
                db.close()
                Log.d(TAG, "Data inserted with id: $id")
                id > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting data", e)
                false
            }
        }
        
        override fun updateData(
            dbName: String,
            tableName: String,
            values: String,
            whereClause: String?,
            whereArgs: String?
        ): Boolean {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    null, 
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                
                val contentValues = android.content.ContentValues()
                val json = JSONObject(values)
                json.keys().forEach { key ->
                    val value = json.get(key)
                    when (value) {
                        is String -> contentValues.put("`$key`", value)
                        is Int -> contentValues.put("`$key`", value)
                        is Long -> contentValues.put("`$key`", value)
                        is Double -> contentValues.put("`$key`", value)
                        is Float -> contentValues.put("`$key`", value)
                        else -> contentValues.put("`$key`", value.toString())
                    }
                }
                
                val args = whereArgs?.split(",")?.map { it.trim() }?.toTypedArray()
                val rows = db.update("`$tableName`", contentValues, whereClause, args)
                db.close()
                Log.d(TAG, "Data updated: $rows rows")
                rows > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error updating data", e)
                false
            }
        }
        
        override fun deleteData(
            dbName: String,
            tableName: String,
            whereClause: String?,
            whereArgs: String?
        ): Boolean {
            return try {
                val dbFile = getDatabasePath(dbName)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    null, 
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                
                val args = whereArgs?.split(",")?.map { it.trim() }?.toTypedArray()
                val rows = db.delete(tableName, whereClause, args)
                db.close()
                Log.d(TAG, "Data deleted: $rows rows")
                rows > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting data", e)
                false
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== DatabaseService created ===")
        Log.d(TAG, "ApplicationInfo: ${applicationInfo.packageName}")
    }
    
    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "DatabaseService bound")
        return binder
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "DatabaseService unbound")
        return super.onUnbind(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DatabaseService destroyed")
    }
}
