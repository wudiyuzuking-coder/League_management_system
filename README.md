# 足球联赛管理与在线票务系统

基于 Vue 3、Spring Boot 3 和 MySQL 8 的前后端分离联赛管理系统，包含赛季生命周期、俱乐部报名、自动排赛、比赛票务、订单、模拟支付、电子票、退票、赛果确认和运营统计。

正式角色：

- `USER`：普通用户
- `CLUB`：俱乐部负责人
- `EVENT_ADMIN`：赛事管理员
- `ADMIN`：系统管理员

## 技术栈

- 前端：Vue 3.5、Vite 7、Pinia 3、Vue Router 4、Element Plus 2、Axios 1
- 后端：Java 17、Spring Boot 3.5.7、Spring Security、MyBatis、JWT、Maven Wrapper
- 数据库：MySQL 8、InnoDB、`utf8mb4`

开发模式端口：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`
- MySQL：默认 `localhost:3306`

## 项目目录

```text
League_management_system/
├─ Program file/
│  ├─ backend/          Spring Boot 后端
│  ├─ frontend/         Vue 前端
│  ├─ database/         数据库结构、基础数据、演示数据和迁移记录
│  ├─ scripts/          Windows PowerShell 开发辅助脚本
│  ├─ release-template/ Release 运行脚本模板
│  └─ .env.example      环境变量示例，不会被程序自动读取
├─ Documents and materials/
├─ uploads/
└─ README.md
```

## 环境要求

- JDK 17 或更高版本
- Node.js `^20.19.0` 或 `>=22.12.0`
- npm
- MySQL 8
- MySQL 命令行客户端 `mysql.exe`，或能够完整执行 SQL 文件的数据库工具

项目自带 Maven Wrapper，不要求单独安装 Maven。首次执行 Maven Wrapper 时需要联网下载 Maven 和项目依赖。

## 本地开发：首次启动

以下命令均在 Windows PowerShell 中执行。项目目录包含空格，请保留路径引号。

### 1. 启动 MySQL

先确认 MySQL 8 正在运行：

```powershell
mysqladmin -h localhost -P 3306 -u root -p ping
```

看到 `mysqld is alive` 后再继续。

如果使用项目提供的 MySQL 启动脚本，需要先设置 `MYSQL_EXE`、`MYSQL_DATA_DIR`、`DB_USERNAME` 和 `DB_PASSWORD`：

```powershell
$env:MYSQL_EXE = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$env:MYSQL_DATA_DIR = "替换为本机 MySQL 数据目录"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "替换为本机 MySQL 密码"

& ".\Program file\scripts\start-mysql.ps1"
```

已经由 Windows 服务启动 MySQL 时，不需要设置 `MYSQL_DATA_DIR`。

### 2. 初始化数据库

进入程序目录：

```powershell
cd ".\Program file"
```

按顺序执行结构和基础数据：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/schema.sql"
mysql --default-character-set=utf8mb4 -u root -p -e "source database/seed.sql"
```

执行正式初始化验收（只读，不写入数据库）：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/test-data.sql"
```

如需答辩演示账号以及赛季、比赛、订单等完整演示数据，再执行：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/demo-data.sql"
```

> **警告：** `demo-data.sql` 会清理并重建业务演示记录，只能用于可丢弃的本地开发数据库。不要在生产库、共享库或包含重要数据的数据库执行。

数据库脚本说明：

- `schema.sql`：创建当前完整数据库结构。
- `seed.sql`：写入角色、权限、系统参数、根管理员，以及四支球队的CLUB账号、完整阵容和STANDARD_8主场。
- `test-data.sql`：只读检查正式初始化后的账号和球队阵容数量。
- `demo-data.sql`：可选答辩演示数据；演示账号初始使用不可登录占位密码。
- `migrations/`：已有旧数据库的增量迁移记录；全新数据库不要重复执行。

### 3. 初始化演示账号密码并启动后端

`demo-data.sql` 不保存明文密码，也不直接保存固定 BCrypt 哈希。导入后，演示账号的 `password_hash` 是：

```text
DEMO_PASSWORD_NOT_FOR_LOGIN
```

因此，**导入 `demo-data.sql` 后必须再启动一次演示密码初始化器**，否则使用 `123456` 登录一定会提示“密码错误”。

#### 推荐方式：使用开发启动脚本

回到仓库根目录，设置本机数据库连接：

```powershell
cd ..

$env:DB_URL = "jdbc:mysql://localhost:3306/league_ticket?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "替换为本机 MySQL 密码"
$env:JWT_SECRET = "替换为至少32字节的本地随机字符串"
$env:APP_UPLOAD_DIR = ".\uploads"

& ".\Program file\scripts\run-backend.ps1"
```

该脚本会在本地 `dev` Profile 下设置：

```text
DEMO_PASSWORD_INIT_ENABLED=true
DEMO_PASSWORD=123456
```

首次成功初始化时，后端日志应出现：

```text
Initialized BCrypt passwords for 7 demo account(s)
```

#### 手动启动后端

如果不使用 `run-backend.ps1`，必须自行设置演示密码变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:mysql://localhost:3306/league_ticket?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "替换为本机 MySQL 密码"
$env:JWT_SECRET = "替换为至少32字节的本地随机字符串"
$env:JWT_EXPIRATION_MINUTES = "120"
$env:APP_UPLOAD_DIR = ".\uploads"
$env:DEMO_PASSWORD_INIT_ENABLED = "true"
$env:DEMO_PASSWORD = "123456"

cd ".\Program file\backend"
.\mvnw.cmd spring-boot:run
```

初始化器只会更新值为 `DEMO_PASSWORD_NOT_FOR_LOGIN` 的账号，不会覆盖已经设置过密码的其他账号。完成首次初始化后，建议后续启动时关闭初始化开关：

```powershell
$env:DEMO_PASSWORD_INIT_ENABLED = "false"
```

完整业务接口只在 `dev` Profile 下注册。默认 `local` Profile 不连接 MySQL，主要用于应用上下文和健康检查测试。

### 4. 启动前端

新开一个 PowerShell 终端，在仓库根目录执行：

```powershell
cd ".\Program file\frontend"
npm ci
npm run dev
```

浏览器打开 `http://localhost:5173`。Vite 默认将 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。

## 演示账号

正式执行 `schema.sql + seed.sql` 后存在以下5个可登录账号，初始密码均为 `123456`：

| 身份 | 手机号 | 4 位工号 | 绑定对象 |
|---|---|---:|---|
| 根管理员 | `13800000000` | `0001` | — |
| CLUB负责人 | `13800000001` | 不需要 | 曼城足球俱乐部 |
| CLUB负责人 | `13800000002` | 不需要 | 拜仁慕尼黑足球俱乐部 |
| CLUB负责人 | `13800000003` | 不需要 | 巴塞罗那足球俱乐部 |
| CLUB负责人 | `13800000004` | 不需要 | 迈阿密国际足球俱乐部 |

正式初始化账号的数据库密码字段均为 BCrypt 哈希，不保存明文。首次登录后应立即修改初始密码。

以下账号仅在额外执行 `demo-data.sql` 并完成演示密码初始化后存在：

成功完成演示密码初始化后，下列账号密码均为 `123456`：

| 身份 | 手机号 | 4 位工号 | 说明 |
|---|---|---:|---|
| 普通用户 | `13800000001` | 不需要 | 演示普通用户 |
| 系统管理员 | `13800000002` | `0001` | 完整工号为 `SA0001` |
| 俱乐部负责人 | `13800000003` | 不需要 | 杭州潮汐足球俱乐部 |
| 赛事管理员 | `13800000005` | `0001` | 完整工号为 `EA0001` |
| 俱乐部负责人 | `13800000006` | 不需要 | 苏州园林足球俱乐部 |
| 俱乐部负责人 | `13800000007` | 不需要 | 杭州星火足球俱乐部 |
| 俱乐部负责人 | `13800000008` | 不需要 | 苏州远航足球俱乐部 |

管理账号登录页只填写工号中的 4 位数字，例如 `EA0001` 填写 `0001`。

## 登录提示密码错误

依次检查：

1. 确认后端使用 `dev` Profile 启动。
2. 确认 `demo-data.sql` 已在当前后端连接的同一个数据库中执行。
3. 确认启动时设置了 `DEMO_PASSWORD_INIT_ENABLED=true`。
4. 检查启动日志是否出现 `Initialized BCrypt passwords for 7 demo account(s)`。
5. 管理账号还需选择正确身份并填写正确的 4 位工号。

如果日志显示初始化了 `0` 个账号，说明这些账号的密码字段已经不是占位值。对于可丢弃的本地演示数据库，最简单可靠的恢复方式是：

1. 停止后端。
2. 重新执行 `database/demo-data.sql`。
3. 设置 `DEMO_PASSWORD_INIT_ENABLED=true` 和期望的 `DEMO_PASSWORD`。
4. 重新启动后端，确认日志显示初始化了 7 个账号。

重新执行 `demo-data.sql` 会清理演示业务数据，请先确认数据库可以被重置。

## 赛季生命周期

```text
DRAFT（草稿）
  ↓ 开启赛季
REGISTERING（报名中）
  ↓ 结束报名并生成赛程
PREPARING（准备中）
  ↓ 确认赛程
ACTIVE（进行中）
  ↓ 结束赛季
FINISHED（已结束）
```

## 测试与构建

### 后端测试

```powershell
cd ".\Program file\backend"
$env:JWT_SECRET = "本地测试用的至少32字节字符串"
.\mvnw.cmd clean test
```

数据库集成测试通过 `RUN_DB_TESTS` 控制，默认会跳过。运行完整数据库集成测试时，必须使用可被测试修改的独立 MySQL 数据库：

独立测试库需先执行 `schema.sql`、`seed.sql` 和 `demo-data.sql`；后者提供集成测试使用的演示账号、赛季和比赛基线。

```powershell
$env:RUN_DB_TESTS = "true"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "独立测试数据库连接"
$env:DB_USERNAME = "测试数据库账号"
$env:DB_PASSWORD = "测试数据库密码"
$env:JWT_SECRET = "本地测试用的至少32字节字符串"
.\mvnw.cmd clean test
```

不要对生产库、共享库或保存重要数据的数据库运行数据库集成测试。

### 前端测试与构建

```powershell
cd ".\Program file\frontend"
npm ci
npm test
npm run build
```

生产构建输出到 `Program file/frontend/dist/`。

## Release / 课程设计运行模式

构建 Release：

```powershell
powershell -ExecutionPolicy Bypass -File ".\Program file\scripts\build-release.ps1"
```

输出目录：

```text
Program file/release/LeagueTicket/
```

Release 中的 Spring Boot JAR 同时提供 Vue 页面、`/api/**` 和 `/uploads/**`，运行时不需要 Node.js、npm、Maven 或单独启动 Vite。

首次部署：

1. 安装并启动 MySQL 8 和 Java 17+。
2. 运行 `init-db.bat` 初始化 `schema.sql` 和 `seed.sql`。
3. 复制 `config.bat.example` 为 `config.bat`，填写本机数据库连接和至少 32 字节的 `JWT_SECRET`。
4. 运行 `start.bat`。

`init-db.bat` 不会自动导入 `demo-data.sql`。如果手工导入了演示数据，还必须在 `config.bat` 中临时加入：

```bat
set "DEMO_PASSWORD_INIT_ENABLED=true"
set "DEMO_PASSWORD=123456"
```

启动一次并确认初始化日志后，将 `DEMO_PASSWORD_INIT_ENABLED` 改回 `false`，再用于日常启动。

## 安全说明

- 不要把真实数据库密码、JWT 密钥或本机 `config.bat` 提交到版本库。
- 不要在生产环境启用演示密码初始化器。
- 不要在包含真实数据的数据库执行 `demo-data.sql` 或数据库集成测试。
- 项目支付功能为课程项目内的模拟支付，不接入真实支付网关或资金结算。
