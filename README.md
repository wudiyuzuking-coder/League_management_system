# 足球联赛管理与在线票务系统

一个前后端分离的足球联赛管理与在线票务系统。项目覆盖联赛、俱乐部、赛程、场馆座位、比赛票务、订单、模拟支付、电子票、退票和运营统计，并通过 JWT、角色权限和俱乐部数据范围限制不同账号的操作边界。

当前正式角色只有 `USER`、`CLUB`、`EVENT_ADMIN` 和 `ADMIN`。前端为四类角色统一使用左侧一级导航与右侧内容区。

## 核心功能

### USER（普通用户）

- 注册、手机号登录以及账号资料、密码和头像维护。
- 浏览赛季、轮次、积分榜、比赛和俱乐部公开资料。
- 查看比赛票区、票价、销售状态、余票和连续座位预览。
- 创建购票订单、取消待支付订单并完成模拟支付。
- 查看本人订单和电子票。
- 对符合条件的已支付订单申请整单自动退票，并查看退款金额和手续费。

### CLUB（俱乐部负责人）

- 维护账号及绑定俱乐部资料。
- 管理本俱乐部当前球员和教练，并在人员总览查看报名合规状态。
- 查看已确认赛程、票务详情和俱乐部数据。
- 在开放时间和名额范围内提交赛季报名，查看报名记录与阵容快照。
- 只读查看本队比赛的票务统计。

### EVENT_ADMIN（赛事管理员）

- 创建赛季并查看正式发布赛果形成的积分榜。
- 查看俱乐部报名，生成、查看和确认赛程。
- 独立提交比赛比分，并保留待维护赛果提醒。
- 确认自动排赛，初始化 STANDARD_8 比赛票务。
- 查看销售、上座率、俱乐部、比赛和退款等运营统计。

### ADMIN（系统管理员）

- 分域管理普通用户、内部管理人员和俱乐部账号状态。
- 预登记 EVENT_ADMIN / ADMIN 管理账号。
- 审核 CLUB 注册申请，并创建新的俱乐部后绑定唯一负责人。
- 确认单人或冲突的赛果提交。
- 维护本人账号资料。

公开注册仅面向 `USER` 和 `CLUB`。管理账号由 `ADMIN` 在后台创建；`CLUB` 注册后需要完成俱乐部审核与绑定才能使用俱乐部业务入口。

## 技术栈

### 前端

- Vue 3.5
- Vite 7
- Pinia 3
- Vue Router 4
- Element Plus 2
- Axios 1
- JavaScript、HTML、CSS

### 后端

- Java 17 编译目标
- Spring Boot 3.5.7
- Spring MVC、Spring Security、Jakarta Validation
- MyBatis Spring Boot Starter 3.0.4
- JJWT 0.13.0
- Lombok
- Maven Wrapper（Maven 3.9.12）

### 数据库

- MySQL 8
- InnoDB、`utf8mb4`、`utf8mb4_0900_ai_ci`

## 系统架构

```text
浏览器
  │
  ▼
Vue 单页应用（Vite 开发端口 5173）
  │  /api、/uploads 代理
  ▼
Spring Boot REST 服务（默认端口 8080）
  ├─ Spring Security + JWT：认证、角色与权限校验
  ├─ Controller / Service：请求处理与业务事务
  ├─ MyBatis Mapper：数据访问
  ├─ Scheduler：订单超时释放、报名截止排赛检查
  └─ 本地上传目录：用户头像和俱乐部队徽
  │
  ▼
MySQL 8（默认数据库 league_ticket）
```

前端请求基地址为 `/api`，开发服务器默认把 `/api` 和 `/uploads` 转发到 `http://localhost:8080`。后端采用无状态 JWT 认证，Token 通过 `Authorization: Bearer <token>` 发送。

## 项目目录

```text
League_management_system/
├─ Program file/
│  ├─ backend/          Spring Boot 后端及 Maven Wrapper
│  ├─ frontend/         Vue 单页应用
│  ├─ database/         数据库结构、基础数据、样例数据和迁移脚本
│  ├─ scripts/          Windows PowerShell 本地运行辅助脚本
│  └─ .env.example      环境变量示例（不会被 Spring Boot 自动加载）
├─ Documents and materials/
│  └─ ...               需求、设计、报告等项目资料
├─ uploads/             仓库现有上传目录；运行时位置由 APP_UPLOAD_DIR 决定
└─ README.md
```

前端主要目录：

- `frontend/src/router/`：页面路由和角色访问限制。
- `frontend/src/layouts/`：四角色共用的整体布局。
- `frontend/src/config/navigation.js`：四角色正式菜单配置。
- `frontend/src/views/`：按角色划分的页面。
- `frontend/src/api/`：前端 API 请求封装。
- `frontend/src/stores/`：Pinia 状态管理。

后端主要目录：

- `backend/src/main/java/.../controller/`：REST 控制器。
- `backend/src/main/java/.../service/`：业务服务与事务规则。
- `backend/src/main/java/.../mapper/`：MyBatis Mapper。
- `backend/src/main/resources/mapper/`：XML Mapper。
- `backend/src/test/java/`：单元测试和数据库集成测试。

## 数据库设计

`Program file/database/schema.sql` 创建 `league_ticket` 数据库及 35 张表。主要数据域包括：

- 账号与授权：用户、角色、权限、角色权限、系统配置和操作日志。
- 联赛管理：赛季、轮次、俱乐部、球员、教练、球员赛季数据和积分记录。
- 报名与赛程：俱乐部报名、报名阵容快照、赛程批次、赛程关系和比赛。
- 场馆与票务：场馆、区域、物理座位、比赛票区和单场座位库存。
- 交易记录：订单、订单明细、模拟支付记录、电子票和退票申请。

数据库通过外键、唯一约束、`CHECK` 约束和事务维护数据一致性。物理场馆座位与每场比赛的可售库存分开保存；订单锁座、支付、取消和退票会同步更新订单明细及比赛库存。

数据库脚本用途：

- `database/schema.sql`：为全新环境创建当前完整结构；脚本不会删除已有数据库或表。
- `database/seed.sql`：写入四个正式角色、权限关系和系统参数，可重复执行。
- `database/test-data.sql`：可选的本地样例数据，不应导入生产库或包含真实数据的数据库。
- `database/migrations/`：已有数据库的增量迁移记录。执行前必须先核对现有结构和备份，不要在全新数据库上重复应用。

## 环境要求

- JDK 17 或更高版本；项目以 Java 17 为编译目标。
- Node.js `^20.19.0` 或 `>=22.12.0`（Vite 7 的运行要求）。
- npm，以及可访问 npm 依赖的环境。
- MySQL 8 和 MySQL 命令行客户端，或可完整执行 SQL 文件的数据库工具。
- 首次使用 Maven Wrapper 时，需要访问 Maven Central 下载 Maven 发行包和项目依赖。

项目提供 Maven Wrapper，不要求预先安装系统 Maven。

## 快速启动

以下命令以 Windows PowerShell 为例。目录名包含空格，请保留引号。

### 1. 初始化数据库

进入程序目录：

```powershell
cd "Program file"
```

使用具有创建数据库和表权限的本地 MySQL 账号，按顺序执行：

```powershell
mysql --default-character-set=utf8mb4 -u db_user -p -e "source database/schema.sql"
mysql --default-character-set=utf8mb4 -u db_user -p -e "source database/seed.sql"
```

需要本地样例数据时再执行：

```powershell
mysql --default-character-set=utf8mb4 -u db_user -p -e "source database/test-data.sql"
```

将 `db_user` 替换为本地数据库账号。不要把数据库密码写入 README、脚本或版本库。

### 2. 配置并启动后端

Spring Boot 不会自动读取 `Program file/.env.example`。启动前应通过当前终端、IDE 运行配置或本机安全配置注入环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:mysql://localhost:3306/league_ticket?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "db_user"
$env:DB_PASSWORD = "your-local-database-password"
$env:JWT_SECRET = "replace-with-a-random-secret-of-at-least-32-bytes"
$env:JWT_EXPIRATION_MINUTES = "120"
$env:APP_UPLOAD_DIR = "./uploads"

cd backend
.\mvnw.cmd spring-boot:run
```

完整业务接口只在 `dev` Profile 下注册。默认 `local` Profile 会关闭数据源和 MyBatis，主要用于不连接数据库的应用上下文与健康检查。

后端默认地址：

- 服务：`http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`

若导入了 `test-data.sql`，样例账号的密码字段初始不可登录。可在隔离的本地数据库中临时设置 `DEMO_PASSWORD_INIT_ENABLED=true` 和自定义 `DEMO_PASSWORD`，启动一次后端以写入 BCrypt 哈希；完成后应关闭该开关。不要对真实账号或生产数据库启用此初始化器。

### 3. 启动前端

新开一个 PowerShell 终端：

```powershell
cd "Program file\frontend"
npm ci
npm run dev
```

前端默认地址为 `http://localhost:5173`。如后端不在默认地址，可在启动前设置 `VITE_API_TARGET`，该值同时用于 `/api` 和 `/uploads` 代理。

### Windows 辅助脚本

`Program file/scripts/` 提供后端、前端以及本地 MySQL 启停脚本。MySQL 辅助脚本依赖调用者预先设置 `MYSQL_EXE`、`MYSQL_DATA_DIR`、数据库账号和其他必要环境变量；`run-backend.ps1` 还会启用样例密码初始化，因此只应在隔离的本地开发数据库中使用。

## 测试与构建

### 后端测试

```powershell
cd "Program file\backend"
.\mvnw.cmd clean test
```

数据库集成测试默认不连接本地 MySQL，并通过条件注解跳过。运行完整数据库集成测试前，应准备可被测试修改的独立 MySQL 8 数据库，初始化当前 SQL，并设置 `RUN_DB_TESTS=true`、`DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `JWT_SECRET`。不要对生产库或保存重要数据的数据库运行这些测试。

### 后端打包

```powershell
cd "Program file\backend"
.\mvnw.cmd clean package
```

成功后生成可执行 JAR：

```text
Program file/backend/target/league-ticket-backend-0.0.1-SNAPSHOT.jar
```

### 前端测试与构建

前端当前没有配置通用 `npm test` 脚本，Node 测试可直接运行：

```powershell
cd "Program file\frontend"
node --test tests/*.test.js
```

`package.json` 中提供的 npm scripts 为：

```powershell
npm run dev
npm run build
npm run preview
```

生产构建输出到 `Program file/frontend/dist/`。

## 项目状态

- 四类正式角色均已接入统一前端布局、路由守卫和角色菜单。
- 联赛管理、俱乐部报名与赛程、STANDARD_8 场馆、比赛票务、订单锁座、模拟支付、电子票、自动退款、多人赛果和运营统计均有对应前端页面与后端实现。
- 支付方式仅为项目内的模拟支付，不包含第三方支付网关或真实资金结算。
- 头像使用本地文件目录存储，仓库没有提供对象存储集成。
- 仓库没有提供 Docker、容器编排或云部署配置；当前文档只描述已核实的本地运行方式。

最近一次本地验证结果：

- `mvnw clean test`：成功；发现 134 项测试，执行 26 项、跳过 108 项、失败 0。跳过项为未启用 `RUN_DB_TESTS` 的数据库集成测试。
- `mvnw clean package`：成功，并生成 Spring Boot 可执行 JAR；测试执行口径与上项相同。
- `npm run build`：成功；Vite 完成 1796 个模块转换。构建提示主资源块超过 500 kB，但不影响产物生成。

本次验证未连接独立 MySQL 测试库，因此不将被跳过的数据库集成测试记为已执行。
