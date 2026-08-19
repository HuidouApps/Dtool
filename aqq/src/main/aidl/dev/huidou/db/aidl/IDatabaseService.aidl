package dev.huidou.db.aidl;

interface IDatabaseService {
    // 数据库操作
    List<String> getDatabases();
    boolean createDatabase(String dbName);
    boolean deleteDatabase(String dbName);
    boolean renameDatabase(String oldName, String newName);
    long getDatabasesTotalSize();

    // 表操作
    List<String> getTables(String dbName);
    boolean createTable(String dbName, String tableName, String columns);
    boolean dropTable(String dbName, String tableName);
    
    // 表结构查询
    String getTableStructure(String dbName, String tableName);
    
    // 数据操作
    String queryData(String dbName, String tableName, String columns, 
                     String selection, String selectionArgs, String orderBy);
    boolean insertData(String dbName, String tableName, String values);
    boolean updateData(String dbName, String tableName, String values, 
                       String whereClause, String whereArgs);
    boolean deleteData(String dbName, String tableName, 
                       String whereClause, String whereArgs);
}
