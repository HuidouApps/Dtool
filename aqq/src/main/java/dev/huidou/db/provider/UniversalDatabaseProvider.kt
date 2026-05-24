package dev.huidou.db.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File

/**
 * 通用数据库管理 ContentProvider
 * 支持：
 * - 数据库列表查询
 * - 数据表 CRUD
 * - 表结构管理（字段定义）
 * - 数据记录 CRUD
 */
class UniversalDatabaseProvider : ContentProvider() {
    
    companion object {
        const val AUTHORITY = "dev.huidou.db.universal"
        
        // URI 路径
        const val PATH_DATABASES = "databases"
        const val PATH_TABLES = "tables"
        const val PATH_TABLE_STRUCTURE = "table_structure"
        const val PATH_DATA = "data"
        
        // URI Matcher 代码
        private const val DATABASES = 1
        private const val TABLES = 2
        private const val TABLES_IN_DB = 3
        private const val TABLE_STRUCTURE = 4
        private const val DATA_QUERY = 5
        private const val DATA_INSERT = 6
        private const val DATA_UPDATE = 7
        private const val DATA_DELETE = 8
        
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_DATABASES, DATABASES)
            addURI(AUTHORITY, PATH_TABLES, TABLES)
            addURI(AUTHORITY, "$PATH_DATABASES/*/tables", TABLES_IN_DB)
            addURI(AUTHORITY, "$PATH_DATABASES/*/tables/*", TABLE_STRUCTURE)
            addURI(AUTHORITY, "$PATH_DATA/*/*", DATA_QUERY)
        }
    }
    
    override fun onCreate(): Boolean {
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        
        return when (uriMatcher.match(uri)) {
            DATABASES -> {
                // 查询所有数据库
                queryDatabases(context)
            }
            TABLES_IN_DB -> {
                // 查询指定数据库的所有表
                val dbName = uri.pathSegments[1]
                queryTables(context, dbName)
            }
            TABLE_STRUCTURE -> {
                // 查询表结构
                val dbName = uri.pathSegments[1]
                val tableName = uri.pathSegments[3]
                queryTableStructure(context, dbName, tableName)
            }
            DATA_QUERY -> {
                // 查询数据
                val dbName = uri.pathSegments[1]
                val tableName = uri.pathSegments[2]
                queryData(context, dbName, tableName, projection, selection, selectionArgs, sortOrder)
            }
            else -> null
        }
    }
    
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            DATABASES -> "vnd.android.cursor.dir/vnd.$AUTHORITY.databases"
            TABLES_IN_DB -> "vnd.android.cursor.dir/vnd.$AUTHORITY.tables"
            TABLE_STRUCTURE -> "vnd.android.cursor.dir/vnd.$AUTHORITY.structure"
            DATA_QUERY -> "vnd.android.cursor.dir/vnd.$AUTHORITY.data"
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val context = context ?: return null
        
        return when (uriMatcher.match(uri)) {
            DATABASES -> {
                // 创建数据库
                val dbName = values?.getAsString("name") ?: return null
                createDatabase(context, dbName)
            }
            TABLES_IN_DB -> {
                // 创建表
                val dbName = uri.pathSegments[1]
                val tableName = values?.getAsString("table_name") ?: return null
                val columns = values?.getAsString("columns") ?: return null
                createTable(context, dbName, tableName, columns)
            }
            DATA_QUERY -> {
                // 插入数据
                val dbName = uri.pathSegments[1]
                val tableName = uri.pathSegments[2]
                insertData(context, dbName, tableName, values)
            }
            else -> null
        }
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val context = context ?: return 0
        
        return when (uriMatcher.match(uri)) {
            DATABASES -> {
                // 删除数据库
                val dbName = selectionArgs?.getOrNull(0) ?: return 0
                if (deleteDatabase(context, dbName)) 1 else 0
            }
            TABLES_IN_DB -> {
                // 删除表
                val dbName = uri.pathSegments[1]
                val tableName = selectionArgs?.getOrNull(0) ?: return 0
                if (dropTable(context, dbName, tableName)) 1 else 0
            }
            DATA_QUERY -> {
                // 删除数据
                val dbName = uri.pathSegments[1]
                val tableName = uri.pathSegments[2]
                deleteData(context, dbName, tableName, selection, selectionArgs)
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
        val context = context ?: return 0
        
        return when (uriMatcher.match(uri)) {
            DATA_QUERY -> {
                // 更新数据
                val dbName = uri.pathSegments[1]
                val tableName = uri.pathSegments[2]
                updateData(context, dbName, tableName, values, selection, selectionArgs)
            }
            else -> 0
        }
    }
    
    // ==================== 数据库操作 ====================
    
    private fun queryDatabases(context: android.content.Context): Cursor {
        val dbPath = context.getDatabasePath("dummy").parentFile
        val databases = dbPath?.listFiles { file ->
            file.name.endsWith(".db") || file.name.endsWith(".sqlite")
        }?.map { it.name }?.sorted() ?: emptyList()
        
        val cursor = MatrixCursor(arrayOf("name", "size", "last_modified"))
        databases.forEach { dbName ->
            val dbFile = context.getDatabasePath(dbName)
            val row = cursor.newRow()
            row.add("name", dbName)
            row.add("size", dbFile.length())
            row.add("last_modified", dbFile.lastModified())
        }
        return cursor
    }
    
    private fun createDatabase(context: android.content.Context, dbName: String): Uri? {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            db.close()
            Uri.parse("content://$AUTHORITY/$PATH_DATABASES/$dbName")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun deleteDatabase(context: android.content.Context, dbName: String): Boolean {
        return try {
            context.deleteDatabase(dbName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ==================== 表操作 ====================
    
    private fun queryTables(context: android.content.Context, dbName: String): Cursor {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                return MatrixCursor(arrayOf("name"))
            }
            
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
                null
            )
            
            val result = MatrixCursor(arrayOf("name"))
            while (cursor.moveToNext()) {
                val row = result.newRow()
                row.add("name", cursor.getString(0))
            }
            cursor.close()
            db.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            MatrixCursor(arrayOf("name"))
        }
    }
    
    private fun createTable(
        context: android.content.Context,
        dbName: String,
        tableName: String,
        columns: String
    ): Uri? {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            val sql = "CREATE TABLE IF NOT EXISTS $tableName ($columns)"
            db.execSQL(sql)
            db.close()
            Uri.parse("content://$AUTHORITY/$PATH_DATABASES/$dbName/tables/$tableName")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun dropTable(context: android.content.Context, dbName: String, tableName: String): Boolean {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            db.execSQL("DROP TABLE IF EXISTS $tableName")
            db.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ==================== 表结构查询 ====================
    
    private fun queryTableStructure(
        context: android.content.Context,
        dbName: String,
        tableName: String
    ): Cursor {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            
            val result = MatrixCursor(arrayOf("cid", "name", "type", "notnull", "dflt_value", "pk"))
            while (cursor.moveToNext()) {
                val row = result.newRow()
                row.add("cid", cursor.getInt(0))
                row.add("name", cursor.getString(1))
                row.add("type", cursor.getString(2))
                row.add("notnull", cursor.getInt(3))
                row.add("dflt_value", cursor.getString(4))
                row.add("pk", cursor.getInt(5))
            }
            cursor.close()
            db.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            MatrixCursor(arrayOf("cid", "name", "type", "notnull", "dflt_value", "pk"))
        }
    }
    
    // ==================== 数据操作 ====================
    
    private fun queryData(
        context: android.content.Context,
        dbName: String,
        tableName: String,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            db.query(
                tableName,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun insertData(
        context: android.content.Context,
        dbName: String,
        tableName: String,
        values: ContentValues?
    ): Uri? {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            val id = db.insert(tableName, null, values)
            db.close()
            if (id > 0) {
                Uri.parse("content://$AUTHORITY/$PATH_DATA/$dbName/$tableName/$id")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun updateData(
        context: android.content.Context,
        dbName: String,
        tableName: String,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            val rows = db.update(tableName, values, selection, selectionArgs)
            db.close()
            rows
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun deleteData(
        context: android.content.Context,
        dbName: String,
        tableName: String,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            val rows = db.delete(tableName, selection, selectionArgs)
            db.close()
            rows
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
