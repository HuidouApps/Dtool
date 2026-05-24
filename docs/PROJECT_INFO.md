# Dtool 项目说明

## 项目概述
这是一个基于 Android + Kotlin + Jetpack Compose 的数据库管理应用，采用多模块架构设计。

## 模块结构

### 1. database 模块（共享数据库库）
- **类型**: Android Library
- **功能**: 提供 Room 数据库的核心功能
- **包含**:
  - User 数据实体
  - UserDao 数据访问对象
  - AppDatabase 数据库实例
  - DatabaseRepository 数据仓库

### 2. app 模块（主应用）
- **包名**: dev.huidou.util
- **功能**: 调用 database 模块，使用 Compose DataTable 展示用户数据
- **特点**: 
  - 表格形式展示数据
  - 支持添加新用户
  - 实时数据更新

### 3. aqq 模块（独立数据库应用）
- **包名**: dev.huidou.db
- **功能**: 独立的 Android 应用，同样使用 database 模块
- **特点**:
  - 卡片列表形式展示数据
  - 支持添加新用户
  - 可以独立运行

## 技术栈
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **数据库**: Room Persistence Library
- **异步处理**: Kotlin Coroutines + Flow
- **依赖注入**: 手动依赖管理
- **构建工具**: Gradle Kotlin DSL

## 主要功能
1. 用户数据管理（增删改查）
2. 实时数据同步（通过 Flow）
3. 响应式 UI 更新
4. 模块化架构设计

## 运行方式

### 编译整个项目
```bash
./gradlew assembleDebug
```

### 运行 app 模块
在 Android Studio 中选择 `app` 配置运行

### 运行 aqq 模块
在 Android Studio 中选择 `aqq` 配置运行

## 数据表结构

**users 表**
- id: Int (主键，自增)
- name: String (姓名)
- email: String (邮箱)
- age: Int (年龄)
- city: String (城市)

## 架构说明
```
app (Application) ──┐
                     ├──> database (Library) <── Room Database
aqq (Application) ──┘
```

两个应用模块都依赖 database 库模块，共享相同的数据库结构和业务逻辑，但可以有各自不同的 UI 实现。
