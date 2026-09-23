League Ticket 课程设计运行说明
================================

运行环境
--------
只需要：Java 17 或更高版本、MySQL 8。
Release 运行不需要 Node.js、npm、Maven，也不需要单独运行 Vite。

第一次部署
----------
1. 安装并启动 MySQL 8，确保 mysql.exe 已加入 PATH。
2. 双击 init-db.bat，按提示输入 MySQL 地址、端口、账号和密码。
   脚本只执行 database\schema.sql 和 database\seed.sql。
3. 复制 config.bat.example 为 config.bat，填写本机数据库账号、密码和随机 JWT_SECRET。
   JWT_SECRET 至少使用 32 个随机字节，不要把 config.bat 提交到 Git。
4. 双击 start.bat。健康检查成功后，浏览器会打开 http://localhost:8080。

以后答辩演示
------------
1. 确保 MySQL 服务已经启动。
2. 双击 start.bat，并保持启动窗口开启。

停止程序
--------
在 start.bat 的窗口中按 Ctrl+C，然后确认终止。

数据库脚本说明
--------------
- schema.sql：为全新环境创建当前数据库结构，不执行 DROP；已有环境重复执行可能因表已存在而停止。
- seed.sql：角色、权限、系统配置、根管理员，以及四支球队的CLUB账号、完整阵容和STANDARD_8主场。
- test-data.sql：正式初始化结果的只读验收查询。
- demo-data.sql：可选答辩演示数据，会清理并重建业务演示记录，绝不会由 init-db.bat 自动导入。
- migrations\：历史数据库增量脚本；全新数据库不要重复执行。

目录说明
--------
- league-ticket.jar：包含 Vue 页面和全部运行依赖的 Spring Boot 可执行 JAR。
- third-party-jars\：供课程检查的运行时第三方依赖副本；正常启动无需手工设置 classpath。
- database\：数据库设计、正式初始化数据、验收查询、可选演示数据和迁移脚本。
- uploads\：头像和队徽等运行时上传目录。

常见错误
--------
- 提示找不到 Java：安装 Java 17+ 并将 java.exe 加入 PATH。
- 提示找不到 mysql：将 MySQL 8 的 bin 目录加入 PATH。
- 数据库连接失败：核对 config.bat 的 DB_URL、DB_USERNAME、DB_PASSWORD，并确认 MySQL 已启动。
- 8080 端口被占用：关闭占用端口的程序后重试。
