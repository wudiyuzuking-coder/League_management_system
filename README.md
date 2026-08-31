# 足球联赛购票系统

本项目是“软件课程设计 I”的足球联赛购票系统，采用前后端分离架构。当前正式提交范围覆盖联赛赛程、俱乐部、场馆与座位、比赛票务、连坐分配、订单、模拟支付、电子票、退票和统计分析。

当前仓库已完成**阶段16D：比分录入日期限制与赛果维护提醒**，可按本文从空数据库初始化并演示四类角色完整流程。

## 项目功能与系统角色

- `USER`：浏览比赛、连坐购票、支付、订单、电子票和退票申请。
- `CLUB`：维护账号绑定俱乐部的资料、球员、教练和球员赛季数据，提交赛季报名，查看自己的已确认赛程、本队比赛和主场统计。
- `EVENT_ADMIN`：负责赛季、轮次、自动赛程确认、比赛与赛果维护、场馆、座位、比赛票务、库存、退票审核和运营统计，并只读查看俱乐部报名。
- `ADMIN`：负责用户管理、俱乐部管理，以及CLUB账号审核、`clubId`绑定和启停。

系统不采用前端菜单隐藏作为权限保障。JWT认证、角色权限和CLUB的俱乐部数据范围均由后端校验。

## 核心演示流程

流程A（购票）：`USER登录 → 浏览比赛 → 查看票区 → 连坐Preview → 创建订单 → 支付 → 查看电子票UNUSED`。

流程B（退票）：`USER购票并支付 → 申请退票 → EVENT_ADMIN审核通过 → 订单/明细/电子票REFUNDED → 比赛座位恢复AVAILABLE`。

流程C（运营）：`EVENT_ADMIN → 比赛管理 → 场馆座位 → 比赛票务与库存 → 退票审核 → 运营统计`。

流程D（账号管理）：`CLUB注册 → DISABLED → ADMIN绑定clubId → ADMIN启用 → CLUB登录并管理自己的俱乐部`。

项目历史版本曾实现独立CHECKER检票模块；当前四角色提交版本未将该模块纳入正式验收范围，相关代码和历史测试保留用于版本追溯。当前演示流程不执行检票，也不把电子票推进为 `USED`。

## 演示系统时间

系统提供统一的演示时间，采用“服务器真实时间 + 全局偏移秒数”模型。默认偏移为0，因此系统时间跟随服务器真实时间；调整到目标时间后，系统时间仍会继续正常流动。任意已登录的正式角色 `USER`、`CLUB`、`EVENT_ADMIN`、`ADMIN` 都可在页面顶部查看、调整并恢复系统时间，未登录请求会被拒绝。

调整以后会立即影响全系统的售票窗口、下单与锁座到期时间、支付、订单超时释放、退票截止以及现有比赛业务时间记录。订单超时调度器仍由服务器真实时钟按固定频率触发，但任务内部使用统一系统时间判断哪些订单已经过期。

## CLUB赛季报名（阶段16B）

赛季比赛阶段继续使用日期级闭区间 `start_date ~ end_date`。EVENT_ADMIN创建或修改新赛季时必须设置报名开始时间、报名截止时间和1至20的俱乐部上限；比赛阶段开始边界为 `start_date 00:00:00`，它必须至少晚于报名开始一个月，报名截止必须至少提前七天。

CLUB只能报名状态为 `DRAFT`、当前统一系统时间位于 `[registration_start_time, registration_deadline)` 且仍有名额的赛季。旧赛季报名字段为NULL时不会进入可报名列表。赛季区间按日期闭区间判断冲突，不能通过比赛时间反推。

报名使用JWT绑定的 `clubId` 和该俱乐部唯一的 `home_stadium_id`。主场必须启用并具有至少一个ACTIVE区域和ACTIVE座位；阵容至少11名ACTIVE球员、至少1名ACTIVE教练，人员必须属于当前俱乐部。每位球员记录 `STARTER` 或 `SUBSTITUTE`，不限制首发恰好11人。报名成功状态只有 `SUBMITTED`，不包含人工批准或拒绝。

主要接口：

- CLUB：`GET /api/club/enrollments/available-seasons`、`POST /api/club/enrollments`、`GET /api/club/enrollments`、`GET /api/club/enrollments/{id}`。
- EVENT_ADMIN：`GET /api/admin/enrollments`、`GET /api/admin/enrollments/{id}`，均为只读。
- 已有数据库升级：执行 `database/migrations/phase16b_club_season_enrollment.sql`，再由人工为需要开放报名的历史赛季回填明确的报名时间和容量。

## 自动双循环排赛与确认（阶段16C）

当DRAFT赛季报名达到 `max_clubs` 时，报名事务提交后自动尝试生成赛程；报名未满但统一系统时间到达 `registration_deadline` 后，每分钟一次的Scheduler扫描也会尝试生成。EVENT_ADMIN还可调用人工补偿接口，但不能绕过满额或截止资格。生成失败不会回滚已经合法提交的报名。

排赛按 `club_id ASC` 使用确定性的Circle Method。N支球队生成 `N × (N - 1)` 场比赛；偶数队为 `2 × (N - 1)` 轮，奇数队内部加入不落库的BYE并生成 `2N` 轮。第二循环严格反转主客场，每轮同一球队至多一场。各轮均匀分布在赛季日期闭区间内，相邻轮至少间隔6天，开球时间读取 `AUTO_SCHEDULE_DEFAULT_KICKOFF_TIME`（默认19:30）。场馆使用报名表保存的主场快照。

自动生成的轮次和比赛保持 `DRAFT`，不创建票区、库存，也不自动发布。`season_schedule_batch` 独立保存 `GENERATED → CONFIRMED` 状态，`season_schedule_match` 追溯批次内比赛；每赛季唯一批次保证Scheduler、满额事件和人工请求重复触发时不会生成第二套。确认只代表EVENT_ADMIN接受赛程，确认时幂等初始化报名球队的零积分记录，比赛仍可沿用原DRAFT编辑/发布流程。

主要接口：

- EVENT_ADMIN：`POST /api/admin/seasons/{seasonId}/schedule/generate`、`GET /api/admin/seasons/{seasonId}/schedule`、`GET /api/admin/schedules`、`POST /api/admin/seasons/{seasonId}/schedule/confirm`。
- CLUB：`GET /api/club/schedules`，只返回与JWT绑定clubId有关的CONFIRMED批次比赛。
- 已有数据库升级：执行 `database/migrations/phase16c_auto_schedule.sql`；无需重建已有数据库。

## 比分日期限制与赛果维护提醒（阶段16D）

比分仍沿用原状态流程：只有 `IN_PROGRESS` 或 `FINISHED` 比赛可维护比分；`IN_PROGRESS` 先保存比分，再由EVENT_ADMIN显式将比赛推进为 `FINISHED`，此时重算积分榜；`FINISHED` 更正比分后立即重算。阶段16D只增加日期级校验：`SystemTimeService.now().toLocalDate() >= matchTime.toLocalDate()`。因此比赛当天即使尚未到具体开球时刻也可录入，比赛日期之前则返回409且不写比分、不改变积分榜。`DRAFT`、`PUBLISHED`、`CANCELLED` 仍按原状态机拒绝比分录入。

EVENT_ADMIN通过 `GET /api/admin/matches/result-reminders` 查看待维护赛果，可按 `seasonId`、`reminderType`（`TODAY`/`OVERDUE`）分页筛选。提醒由现有 `match_info` 数据和Java传入的统一系统日期动态计算：仅包含比赛日期已到的 `PUBLISHED`、`IN_PROGRESS`；`DRAFT`、`FINISHED`、`CANCELLED` 不包含。`daysOverdue` 是系统日期与比赛日期之差，逾期比赛优先并按比赛时间升序排列。比赛完成后自动从查询结果消失，系统时间调整或回拨后前端重新请求即可得到新结果。

本阶段没有新增提醒表、字段、状态、Scheduler、消息中心或推送机制。前端EVENT_ADMIN菜单显示“赛果待维护”数量并进入独立列表，列表的维护按钮复用原比赛状态和比分维护页面。

后端接口：

- `GET /api/system-time`：读取系统时间、服务器真实时间和偏移秒数。
- `PUT /api/system-time`：提交 `targetTime`，按目标时间重新计算全局偏移。
- `POST /api/system-time/reset`：将偏移恢复为0。

每次调整或重置都会复用 `operation_log` 记录操作者、调整前后系统时间、offset变化和操作发生的真实时间。

> 系统时间调整功能仅用于课程设计演示环境，不适用于生产环境。真实生产系统不应向普通用户开放全局时间修改权限。

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring MVC
- MyBatis
- MySQL
- Maven
- Jakarta Validation
- Lombok
- Spring Security
- JJWT

### 前端

- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router
- Pinia

## 目录结构

```text
League_management_system/
├─ backend/       Spring Boot 后端
├─ frontend/      Vue 3 前端
├─ database/      MySQL 8 建表、基础数据和演示数据脚本
├─ docs/          设计辅助、测试报告与AI使用记录
├─ README.md      项目运行说明
├─ .gitignore
└─ *.doc/*.docx/*.pptx/*.md  课程要求与需求设计资料
```

后端遵循 Controller、Service、Mapper 多层结构。Controller 只负责 HTTP 请求处理和参数校验，不直接访问 Mapper；业务规则统一放入 Service。

## 环境要求

- JDK 17 或更高版本，项目编译目标为 Java 17
- Maven 3.6.3 或更高版本，项目提供 Maven Wrapper 时优先使用 Wrapper
- Node.js 20.19+、22.12+ 或更高兼容版本
- npm 10 或更高版本
- MySQL 8.0.16 或更高版本（需要数据库实际执行 `CHECK` 约束）

在 Windows PowerShell 中可先确认 Node.js 与 npm 已正确安装并加入 `PATH`：

```powershell
node -v
npm -v
```

如果命令无法识别，请重新打开终端；仍无效时检查 Node.js 安装目录（通常为 `C:\Program Files\nodejs`）是否已加入用户或系统 `PATH`。当前前端使用 Vite 7，Node.js 必须满足上面的版本要求。

`local` Profile仅用于无需数据库的健康检查；认证与俱乐部管理接口必须使用 `dev` Profile连接已经初始化的 `league_ticket` 数据库。

## 初始化数据库

数据库名称为 `league_ticket`，字符集为 `utf8mb4`，排序规则为 `utf8mb4_0900_ai_ci`。请使用具有创建数据库和表权限的 MySQL 账号，严格按以下顺序执行：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/schema.sql"
mysql --default-character-set=utf8mb4 -u root -p -e "source database/seed.sql"
mysql --default-character-set=utf8mb4 -u root -p -e "source database/test-data.sql"
```

若当前 MySQL 客户端不能通过 `-e "source ..."` 导入，可在 Windows `cmd.exe` 中使用输入重定向：

```bat
mysql --default-character-set=utf8mb4 -u root -p < database\schema.sql
mysql --default-character-set=utf8mb4 -u root -p < database\seed.sql
mysql --default-character-set=utf8mb4 -u root -p < database\test-data.sql
```

PowerShell 不直接支持上述 `<` 语法，可使用管道：

```powershell
Get-Content -Raw database/schema.sql | mysql --default-character-set=utf8mb4 -u root -p
Get-Content -Raw database/seed.sql | mysql --default-character-set=utf8mb4 -u root -p
Get-Content -Raw database/test-data.sql | mysql --default-character-set=utf8mb4 -u root -p
```

如果 `where.exe mysql` 找不到客户端，可将 MySQL 的 `bin` 目录加入 `PATH`，或用实际安装路径完整调用。例如 MySQL 8.0 的常见路径为：

```powershell
where.exe mysql
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --version
```

在 `cmd.exe` 中结合完整路径与重定向时，命令形式为：

```bat
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p < database\schema.sql
```

以上重定向与PowerShell管道形式已在本项目隔离的 MySQL 8.0.44 环境验证。

- `schema.sql`：创建数据库、30张正式业务表、外键、唯一约束、`CHECK` 约束和必要索引。
- `seed.sql`：写入四个固定角色、基础权限、角色权限关系和系统参数；设计为可重复执行。
- `test-data.sql`：写入课程设计演示所需的最小赛季、轮次、俱乐部、场馆、静态座位、比赛、比赛库存和测试账号。它只应用于空库、隔离开发库或专用测试库；已有数据环境导入前必须备份并核对主键、唯一键和演示数据覆盖风险。初始密码字段是不可登录标记，必须使用下述显式初始化方式转换为BCrypt。

如使用 MySQL Workbench，可依次打开并完整执行以上三个文件。`schema.sql` 不会删除已有数据库或表，建议首次执行使用空数据库环境。

从阶段6开始，`match_info` 增加首次发布时间字段。已经使用旧版脚本初始化过的开发数据库需在启动阶段6后端前执行一次：

```sql
ALTER TABLE match_info
    ADD COLUMN published_at DATETIME NULL COMMENT '首次发布时间' AFTER match_status;
```

对已有已发布演示比赛，可按实际情况回填首次发布时间；全新数据库直接执行当前 `schema.sql` 和 `test-data.sql` 即可，无需额外迁移。

阶段8调整了比赛票区字段。已有数据库必须按以下顺序迁移，不能假定比赛票区为空：

```sql
-- 1. 删除不再使用的持久化总票数字段及其CHECK。
ALTER TABLE match_ticket_zone
    DROP CHECK ck_match_zone_quantity;
ALTER TABLE match_ticket_zone
    DROP COLUMN total_quantity;

-- 2. 先以可空方式增加创建人，避免已有数据使ALTER直接失败。
ALTER TABLE match_ticket_zone
    ADD COLUMN created_by BIGINT UNSIGNED NULL COMMENT '创建管理员'
    AFTER stadium_zone_id;

-- 3. 选择一个合法且启用的ADMIN账号进行历史数据回填。
SET @migration_admin_id := (
    SELECT u.user_id
    FROM sys_user u
    JOIN sys_role r ON r.role_id = u.role_id
    WHERE r.role_code = 'ADMIN' AND u.user_status = 'ENABLED'
    ORDER BY u.user_id
    LIMIT 1
);
UPDATE match_ticket_zone
SET created_by = @migration_admin_id
WHERE created_by IS NULL;

-- 4. 必须先确认结果为0；若不是0，应先创建或启用一个ADMIN账号再回填。
SELECT COUNT(*) AS ticket_zone_without_creator
FROM match_ticket_zone
WHERE created_by IS NULL;

-- 5. 回填完整后再收紧非空约束并增加外键。
ALTER TABLE match_ticket_zone
    MODIFY COLUMN created_by BIGINT UNSIGNED NOT NULL COMMENT '创建管理员',
    ADD CONSTRAINT fk_match_zone_created_by
        FOREIGN KEY (created_by) REFERENCES sys_user (user_id);
```

若第4步查询结果不为0，不得执行第5步。迁移后创建人只在新建比赛票区时由后端写入，后续编辑不会修改。

阶段10为订单取消原因和订单明细状态增加正式字段。已有数据库按以下顺序安全迁移：

```sql
-- 1. 先检查历史订单与明细。当前项目在阶段10前通常均为0。
SELECT COUNT(*) AS existing_order_count FROM ticket_order;
SELECT COUNT(*) AS existing_item_count FROM order_item;

-- 2. 取消原因默认NULL，不会改变已有订单业务状态。
ALTER TABLE ticket_order
    ADD COLUMN cancel_reason VARCHAR(32) NULL COMMENT '取消原因'
    AFTER cancelled_at;

-- 3. 新增明细状态。MySQL会将已有记录填充为默认LOCKED。
ALTER TABLE order_item
    ADD COLUMN item_status VARCHAR(16) NOT NULL DEFAULT 'LOCKED' COMMENT '订单明细状态'
    AFTER seat_no_snapshot;

-- 4. 若第1步发现历史明细，必须按其订单状态复核并修正，不能直接假定全部LOCKED。
SELECT oi.item_id, oi.order_id, o.order_status, oi.item_status
FROM order_item oi
JOIN ticket_order o ON o.order_id = oi.order_id
WHERE (o.order_status = 'CANCELLED' AND oi.item_status <> 'CANCELLED')
   OR (o.order_status = 'PAID' AND oi.item_status <> 'PAID')
   OR (o.order_status = 'REFUNDED' AND oi.item_status <> 'REFUNDED');

-- 示例修正必须在人工确认历史业务含义后执行：
-- UPDATE order_item oi JOIN ticket_order o ON o.order_id=oi.order_id
-- SET oi.item_status='CANCELLED' WHERE o.order_status='CANCELLED';

-- 5. 确认历史状态一致后增加完整状态CHECK。
ALTER TABLE order_item
    ADD CONSTRAINT ck_order_item_status
    CHECK (item_status IN ('LOCKED', 'PAID', 'CANCELLED', 'REFUNDED'));
```

全新数据库直接执行当前 `schema.sql`。阶段10仅产生 `LOCKED`、`CANCELLED`，不会提前产生 `PAID` 或 `REFUNDED`。

## 启动后端

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS/Linux：

```bash
cd backend
./mvnw spring-boot:run
```

如果本机已经安装 Maven，也可以运行：

```bash
mvn spring-boot:run
```

启动后访问：

```text
http://localhost:8080/api/health
```

需要连接开发数据库时启用 `dev` Profile。`application-dev.yml` 使用以下环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，可选连接池参数为 `DB_MAX_POOL_SIZE` 和 `DB_MIN_IDLE`。

根目录 [`.env.example`](.env.example) 汇总了开发环境变量示例。Spring Boot 不会自动读取根目录 `.env`；可以按下例在终端设置，或由IDE启动配置/本地环境加载。真实密码和JWT密钥只放在本机 `.env` 或环境变量中，不要提交到Git。

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:mysql://localhost:3306/league_ticket?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
$env:JWT_SECRET="replace-with-at-least-32-random-bytes"
$env:JWT_EXPIRATION_MINUTES="120"
.\mvnw.cmd spring-boot:run
```

首次初始化演示账号密码时，再临时设置：

```powershell
$env:DEMO_PASSWORD_INIT_ENABLED="true"
$env:DEMO_PASSWORD="123456"
```

初始化器只会替换仍为 `DEMO_PASSWORD_NOT_FOR_LOGIN` 的账号密码，写入数据库的是随机加盐的BCrypt哈希。成功初始化一次后，应关闭 `DEMO_PASSWORD_INIT_ENABLED`。

> 演示账号密码仅用于课程设计测试，不代表生产安全策略。

## JWT与演示账号

登录成功返回仅有一个Access Token，不实现Refresh Token。Token默认有效期2小时，至少携带 `userId`、`username` 和 `roleCode`，前端通过 `Authorization: Bearer <token>` 发送。

完成上述密码初始化后，可使用统一演示密码 `123456`：

| 角色 | 用户名 | 前端入口 |
|---|---|---|
| USER | `demo_user` | `/user` |
| CLUB | `demo_club` | `/club` |
| EVENT_ADMIN | `demo_event_admin` | `/admin` |
| ADMIN | `demo_admin` | `/admin` |

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

Vite 开发服务器会将 `/api` 请求代理到 `http://localhost:8080`。

## 常见启动问题

- 数据库连接失败：确认 MySQL 已启动，`league_ticket` 已初始化，并核对 `DB_URL`、端口、账号、密码及 `allowPublicKeyRetrieval=true` 等连接参数。使用 `dev` Profile 时不能依赖 `local` Profile 的无数据库健康检查配置。
- `JWT_SECRET` 未设置或长度不足：为开发环境设置至少32字节的随机密钥，重新启动后端；不要把真实密钥写入仓库。
- 端口8080被占用：在 PowerShell 运行 `Get-NetTCPConnection -LocalPort 8080`，或在 `cmd.exe` 运行 `netstat -ano | findstr :8080`，根据PID定位占用进程。请人工确认进程后停止冲突服务，或在本地启动配置中显式改用其他端口；不要直接自动终止未知系统进程。若改端口，还需同步前端 Vite 代理目标。
- `node` 或 `npm` 无法识别：执行 `node -v`、`npm -v`，确认安装版本与 `PATH` 后重新打开终端。

## 构建与测试

后端：

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

数据库集成测试默认不自动连接本机数据库。需要执行完整数据库测试时，先准备专用测试库并设置 `RUN_DB_TESTS=true`、`DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `JWT_SECRET`；不要对保存真实业务数据的数据库运行集成测试。

完整验收应在隔离的 MySQL 8 实例中依次执行 `schema.sql → seed.sql → test-data.sql`，设置 `RUN_DB_TESTS=true` 后运行后端测试。测试总结见 [docs/test-report/TEST_REPORT.md](docs/test-report/TEST_REPORT.md)。

前端：

```bash
cd frontend
npm run build
```

## 交付文档

- [完整测试报告](docs/test-report/TEST_REPORT.md)
- [大模型辅助开发使用说明](docs/ai-usage/AI_USAGE.md)
- [需求文档索引](docs/requirements/README.md)
- [设计文档索引](docs/design/README.md)

当前四角色版本使用隔离 MySQL 8.0.44 验收：阶段16A测试基线为63项（原57项正式测试全部保留，新增6项系统时间测试），`Failures=0`、`Errors=0`、`Skipped=0`；`clean test`、不跳过测试的 `clean package` 和前端生产构建均成功。真实JAR与Vue环境已完成购票支付、整单退票、CLUB账号绑定启用、运营统计和统一演示时间流程。

## 实现与设计说明

以下阶段说明是项目演进记录。阶段13曾实现独立CHECKER检票模块，其中出现的CHECKER、检票接口和历史权限描述仅用于版本追溯，不代表当前四角色提交版本的正式角色或验收流程。对应旧集成测试已归档至 `docs/legacy-tests/checkin/`。

阶段1已包含：

- Spring Boot 和 Vue 基础工程
- 统一 API 返回结构和全局异常处理
- Jakarta Validation 异常处理
- CORS 配置
- 后端健康检查
- Element Plus、Router、Pinia 和 Axios 基础配置
- 用户端与管理端基础布局
- 登录占位页和 404 页面

阶段2已增加：

- 阶段2形成25张业务表；阶段16B新增3张独立报名表，阶段16C新增2张赛程批次关系表，当前共30张正式业务表及完整数据库约束和索引
- 系统角色、基础权限、角色授权和系统配置
- 可用于课程设计演示的最小测试数据

阶段3已增加：

- 普通用户注册、四类账号登录、当前用户信息
- 个人资料和密码修改
- 管理员用户分页、详情、创建、修改和状态管理
- 角色及权限只读接口
- Spring Security无状态JWT认证、数据库权限加载、401/403响应
- 前端登录、注册、Token管理、路由守卫和四类角色入口

阶段4已增加：

- 已登录用户可分页查询俱乐部和查看俱乐部详情
- CLUB仅能按当前JWT账号绑定的 `clubId` 查看、修改自身资料，管理本俱乐部球员、教练和球员赛季数据
- ADMIN可新增、修改、启停任意俱乐部，并管理目标俱乐部球员、教练和球员赛季数据
- 球衣号唯一、球员归属、赛季统计唯一和非负数等规则在Service层校验
- CLUB前端包含俱乐部资料、球员、教练和赛季数据四个页面
- ADMIN前端包含俱乐部列表、分页筛选、创建编辑启停和俱乐部详情管理页面

### 阶段4接口

- 公共已登录读取：`GET /api/clubs`、`GET /api/clubs/{id}`
- CLUB自身资料：`GET/PUT /api/club/profile`
- CLUB球队管理：`/api/club/players`、`/api/club/coaches`、`/api/club/player-season-stats`
- ADMIN俱乐部管理：`/api/admin/clubs`、`/api/admin/clubs/{id}`、`/api/admin/clubs/{id}/status`
- ADMIN球队管理：`/api/admin/clubs/{clubId}/players`、`/api/admin/clubs/{clubId}/coaches`、`/api/admin/clubs/{clubId}/player-season-stats`

CLUB测试账号为 `demo_club`，ADMIN测试账号为 `demo_admin`。完成一次演示密码初始化后，两者均使用配置的 `DEMO_PASSWORD`（示例为 `123456`）。CLUB账号必须在 `sys_user.club_id` 绑定有效俱乐部，否则自身管理接口会明确拒绝访问。

阶段5已增加：

- 赛季列表、详情、ADMIN新增修改及 `DRAFT → ACTIVE → FINISHED` 单向状态流转
- 轮次列表、详情、ADMIN维护及 `DRAFT → PUBLISHED → FINISHED` 单向状态流转
- 轮次编号唯一、日期范围和赛季日期范围的Service层校验
- 四种已登录角色均可读取赛季、轮次和积分榜，只有ADMIN可以写入
- 积分榜按积分、净胜球、进球数和俱乐部ID稳定排序，排名与净胜球实时计算
- USER入口：`/user/seasons`、`/user/seasons/:id/rounds`、`/user/seasons/:id/standings`
- ADMIN入口：`/admin/seasons`、`/admin/seasons/:id`
- CLUB球员赛季数据页面已改为从赛季接口加载下拉选项

### 阶段5接口

- 已登录读取：`GET /api/seasons`、`GET /api/seasons/{id}`、`GET /api/seasons/{seasonId}/rounds`、`GET /api/rounds/{id}`、`GET /api/seasons/{seasonId}/standings`
- ADMIN赛季：`GET/POST /api/admin/seasons`、`PUT /api/admin/seasons/{id}`、`PUT /api/admin/seasons/{id}/status`
- ADMIN轮次：`GET/POST /api/admin/seasons/{seasonId}/rounds`、`PUT /api/admin/rounds/{id}`、`PUT /api/admin/rounds/{id}/status`
- ADMIN积分榜：`POST /api/admin/seasons/{seasonId}/standings/init`、`PUT /api/admin/season-records/{recordId}`

当前尚无赛季参赛俱乐部关联表。初始化及重算积分榜时采用课程设计简化规则：将当前所有 `ACTIVE` 俱乐部作为参赛队并补齐缺失记录；已经参与已结束比赛的俱乐部也会确保存在记录。管理员手工战绩维护仅作为测试接口，正式战绩由已结束比赛统一重算。

阶段6已增加：

- 已登录角色可分页筛选比赛并查看比赛详情
- ADMIN可创建、编辑、发布、开始、结束或取消比赛，并维护比分
- 比赛状态机：`DRAFT → PUBLISHED → IN_PROGRESS → FINISHED`，并允许非终态进入 `CANCELLED`
- 首次发布写入 `published_at`，重复发布或后续流转不覆盖
- 主队必须配置主场，比赛场馆必须等于主队主场
- 比赛时间必须位于所属赛季和轮次日期范围，同一轮相同主客队对阵由Service拒绝重复
- 仅 `IN_PROGRESS`、`FINISHED` 比赛允许维护比分，结束前必须已有完整比分
- 首次结束比赛以及更正已结束比赛比分后，统一根据该赛季所有完整的 `FINISHED` 比赛重算积分榜
- USER入口：`/user/matches`、`/user/matches/:id`
- CLUB入口：`/club/matches`，只读展示当前账号绑定俱乐部参与的比赛
- ADMIN入口：`/admin/matches`、`/admin/matches/:id`

### 阶段6接口

- 已登录读取：`GET /api/matches`、`GET /api/matches/{id}`
- 场馆只读：`GET /api/stadiums`、`GET /api/stadiums/{id}`
- ADMIN比赛：`GET/POST /api/admin/matches`、`GET/PUT /api/admin/matches/{id}`
- ADMIN状态：`PUT /api/admin/matches/{id}/status`
- ADMIN比分：`PUT /api/admin/matches/{id}/score`

阶段5的 `PUT /api/admin/season-records/{recordId}` 继续保留为课程设计测试维护接口。正常业务以已结束比赛为战绩唯一数据源；任何比赛触发的赛季重算都会覆盖手工维护值。

阶段7已增加：

- 场馆资料新增、修改、查询和启停，已有关联业务数据不物理删除
- 场馆静态票区新增、修改和启停；同场馆票区编码、名称分别保持唯一
- 物理座位新增、修改和启停；支持不同排宽的按排批量生成
- 批量生成会先校验整批数据与已有座位，任一排号或座位标签冲突时整批拒绝、零写入
- 布局按排排序号、座位排序号稳定展示，排标签和座位标签可独立维护
- 容量汇总同时显示场馆申报容量、已建座位数、启用座位数和停用座位数
- CLUB俱乐部资料页从启用场馆列表选择主场；ADMIN比赛页面也只允许选择启用场馆
- 新建或编辑比赛时若目标场馆已停用，后端Service会拒绝保存；停用场馆不会级联修改已有比赛
- ADMIN入口：`/admin/stadiums`、`/admin/stadiums/:id`

### 阶段7静态模型与接口

静态模型分为三层：`stadium_info` 表示场馆资料，`stadium_zone` 表示场馆长期静态票区，`stadium_seat` 表示实际物理座位。静态座位状态仅有 `ACTIVE` 和 `DISABLED`，绝不使用 `LOCKED`、`SOLD` 等比赛销售状态；这些状态将在后续由单场比赛座位库存承担。

- 已登录读取：`GET /api/stadiums`、`GET /api/stadiums/{id}`、`GET /api/stadiums/{id}/zones`
- 座位读取：`GET /api/stadium-zones/{id}/seats`、`GET /api/stadium-zones/{id}/layout`
- ADMIN场馆：`GET/POST /api/admin/stadiums`、`GET/PUT /api/admin/stadiums/{id}`、`PUT /api/admin/stadiums/{id}/status`
- ADMIN票区：`GET/POST /api/admin/stadiums/{id}/zones`、`PUT /api/admin/stadium-zones/{id}`、`PUT /api/admin/stadium-zones/{id}/status`
- ADMIN座位：`GET/POST /api/admin/stadium-zones/{id}/seats`、`POST /api/admin/stadium-zones/{id}/seats/batch`、`PUT /api/admin/stadium-seats/{id}`、`PUT /api/admin/stadium-seats/{id}/status`
- 容量汇总：`GET /api/admin/stadiums/{id}/capacity-summary`

批量生成请求以 `rows` 数组描述各排，每排分别提供排排序号、排标签、起始座位序号和座位数，因此可稳定表达各排座位数不同的场馆。申报容量是场馆资料值，物理座位数量按 `stadium_seat` 实时统计，两者允许暂时不一致并由管理页同时展示。

阶段8已增加：

- ADMIN为 `DRAFT/PUBLISHED` 比赛配置比赛票区、票价和销售时间，创建人由当前登录管理员自动绑定
- 比赛票区销售状态机：`DRAFT → ON_SALE/CLOSED`、`ON_SALE → PAUSED/CLOSED`、`PAUSED → ON_SALE/CLOSED`，`CLOSED`为终态
- 库存必须由ADMIN显式生成；重复生成直接拒绝，本阶段不提供库存重置
- 生成库存只复制对应静态票区当时处于 `ACTIVE` 的物理座位，形成该场比赛独立快照
- ADMIN可将比赛库存座位在 `AVAILABLE/DISABLED` 间切换，本阶段不主动产生 `LOCKED/SOLD`
- 总库存、余票、停用数量和最大连续数均从 `match_seat_inventory` 实时计算，不写回比赛票区
- USER比赛详情展示票价、销售时间、余票和最大连坐数，但不提供下单
- CLUB可从本队比赛进入只读票务统计；CHECKER可读取基础公开票务信息；所有票务写操作仅限ADMIN
- ADMIN入口：`/admin/matches/:id/tickets`；USER入口：`/user/matches/:id`；CLUB入口：`/club/matches/:id/tickets`

### 阶段8静态结构与比赛快照

`stadium_zone` 是场馆长期存在的静态看台区域，`match_ticket_zone` 是某一场比赛对该区域设置的价格、销售时间和销售状态。`stadium_seat` 是长期物理座位，`match_seat_inventory` 是物理座位在某场比赛中的销售库存快照。

比赛票区创建后，管理员需要单独执行库存生成。生成过程只读取当前 `ACTIVE` 物理座位并写入 `AVAILABLE` 库存；静态座位以后被停用不会自动污染已经生成的比赛快照。比赛A的库存状态变化不会修改 `stadium_seat`，也不会影响比赛B。

### 阶段8接口

- 已登录读取：`GET /api/matches/{matchId}/ticket-zones`、`GET /api/match-ticket-zones/{id}`、`GET /api/match-ticket-zones/{id}/availability`
- ADMIN比赛票区：`GET/POST /api/admin/matches/{matchId}/ticket-zones`、`GET/PUT /api/admin/match-ticket-zones/{id}`、`PUT /api/admin/match-ticket-zones/{id}/status`
- ADMIN库存：`POST /api/admin/match-ticket-zones/{id}/inventory/generate`、`GET /api/admin/match-ticket-zones/{id}/inventory`、`PUT /api/admin/match-seat-inventory/{id}/status`

`totalSeatCount`统计票区全部比赛库存；`availableSeatCount`仅统计 `AVAILABLE`；`disabledSeatCount`仅统计 `DISABLED`。`maxContinuousCount`按排扫描 `AVAILABLE` 座位，以座位排序号相差1作为连续，取所有排中最长连续段。用户侧的 `saleAvailable` 还同时要求票区为 `ON_SALE`、当前时间位于销售区间、比赛为 `PUBLISHED` 且余票大于0。

阶段9已增加：

- `SeatAllocateService`读取 `MAX_TICKETS_PER_ORDER`，支持1～4张连坐预览
- 只允许同一排、座位排序号逐个加1且库存状态为 `AVAILABLE` 的座位组成候选
- 对每个连续段使用滑动窗口生成全部指定长度候选，不遗漏中间窗口
- USER可在比赛票区选择购票张数并检查当前连坐能力；结果仅为只读预览
- ADMIN可查看所有候选的中线距离、剩余碎片数和最大剩余连续空间
- 阶段8的 `maxContinuousCount` 与阶段9共用同一套连续段识别逻辑
- 提供内部事务性条件更新能力，用于验证并发竞争时同一库存最多被一个请求成功修改

### 阶段9连坐算法

连续座位严格定义为同一个 `rowNo` 中 `seatNo` 逐个加1。不同排不能合并，缺失座号、`DISABLED`、`LOCKED`、`SOLD`都会截断连续段。每个长度不少于购票张数的连续段会生成全部滑动窗口，例如 `1 2 3 4 5` 请求3张时生成 `1-3`、`2-4`、`3-5`三个候选。

候选排序规则固定为：

```text
rowNo ASC
→ centerDistance ASC
→ remainingFragmentCount ASC
→ maxRemainingContinuousLength DESC
→ startSeatNo ASC
```

排号是最高优先级，不会为了更靠近中线跨越到后排。排中线按该排完整物理座位范围计算：`(minSeatNo + maxSeatNo) / 2.0`，不会用当前AVAILABLE座位错误地缩小中线范围。候选中线是首尾座号平均值，两者绝对差即 `centerDistance`。

候选占用后，原连续段左侧和右侧仍有座位时分别形成一个剩余片段，片段数量即 `remainingFragmentCount`；片段数相同时优先保留更大的 `maxRemainingContinuousLength`，最后以较小起始座号稳定兜底。

### 阶段9接口与并发边界

- 已登录只读预览：`POST /api/match-ticket-zones/{id}/seat-allocation/preview`
- ADMIN算法调试：`POST /api/admin/match-ticket-zones/{id}/seat-allocation/debug`

preview只计算候选，不修改库存，也不保证之后下单时这些座位仍然可用；实际座位必须在下单事务中重新计算并锁定。无完整连坐候选时返回409，并在错误信息中给出当前 `maxContinuousCount`，系统不会跨排、拆单或返回不连续座位。

阶段9使用 `inventory_status='AVAILABLE' AND version=?` 的条件更新和事务回滚，通过 `AVAILABLE → DISABLED`验证并发原子语义：两个线程竞争同一组座位时最多一个成功，失败事务不会留下部分更新。该状态变化能力不暴露为购票接口，也没有伪造订单。

阶段10已把相同的“事务内重新计算候选 + 条件更新”机制接入真实 `ticket_order`，正式执行 `AVAILABLE → LOCKED`并填写合法的 `lock_order_id`、`locked_at`和`lock_expire_time`。

### 阶段10订单创建与正式锁座

USER接口：

- `POST /api/orders`：提交比赛票区和1～4张数量，创建待支付订单
- `GET /api/orders`：分页查询自己的订单，支持 `orderStatus` 筛选
- `GET /api/orders/{id}`：查询自己的订单和具体座位明细
- `POST /api/orders/{id}/cancel`：取消自己的待支付订单并释放座位

正式创建流程：

```text
用户提交购票
↓
校验比赛、票区、销售状态和销售时间
↓
创建PENDING_PAYMENT订单并取得真实orderId
↓
事务内重新运行连坐算法
↓
AVAILABLE → LOCKED并写入lock_order_id及过期时间
↓
逐座创建LOCKED订单明细和价格、票区、排座快照
↓
等待支付
↓
用户取消/支付超时 → 订单和明细CANCELLED → 座位恢复AVAILABLE
```

Preview始终只读，前端不能保存Preview返回的库存ID用于正式下单。创建订单时会重新查询并锁定票区库存，因此实际座位可能与先前Preview不同，但一定重新满足同排连续规则。

订单号使用 `LT + 毫秒时间戳 + 8位随机值`，数据库唯一约束作为最终保障；不把自增主键作为业务订单号。单价读取比赛票区当前价格并保存到每条 `order_item`，订单总额由后端按 `单价 × 张数` 计算，后续调价不会改变历史订单。

支付截止时间读取 `ORDER_PAYMENT_TIMEOUT_MINUTES`，默认15分钟。订单 `expire_time` 与库存 `lock_expire_time` 使用同一时间值。`OrderTimeoutTask` 每分钟扫描到期的 `PENDING_PAYMENT` 订单，但每个订单由独立事务处理；用户取消写入 `USER_CANCELLED`，超时关闭写入 `PAYMENT_TIMEOUT`。

并发锁座先按固定顺序锁定该票区库存行，再使用阶段9算法在最新状态上重新选择，并以 `inventory_status='AVAILABLE' AND version=?` 条件更新。一个事务内必须同时完成订单、全部库存锁和全部明细；任一步失败整体回滚。取消与超时都先锁订单并复核状态，只释放 `lock_order_id` 属于该订单的 `LOCKED` 库存。

阶段11已完成模拟支付与电子票。USER可在待支付订单详情中选择“模拟成功”或“模拟失败”：失败只记录一次 `FAILED` 支付尝试，订单与锁座保持不变并允许重试；成功则在单一数据库事务内完成订单 `PAID`、明细 `PAID`、库存 `SOLD`（同时清空锁定字段）以及逐明细生成电子票。订单行锁保证重复或并发支付只产生一条成功记录和一组电子票。

### 阶段11接口

- `POST /api/orders/{orderId}/pay`：当前USER模拟支付自己的订单，请求字段为 `payMethod=SIMULATED`、`simulateResult=SUCCESS/FAILED`
- `GET /api/tickets`：分页查询当前USER的电子票，可按 `ticketStatus` 筛选
- `GET /api/tickets/{id}`：查询当前USER的一张电子票
- `GET /api/orders/{id}`：已扩展返回成功支付摘要和本订单电子票

支付前会重新校验订单归属、有效期、订单与明细状态、库存锁归属和数量一致性。到期订单在支付请求中按 `PAYMENT_TIMEOUT` 关闭并释放座位后返回冲突；取消或超时还会关闭仍为 `CREATED` 的支付记录。支付完成后座位不可再调整。接口中的 `enterTime` 映射数据库既有 `e_ticket.used_at`，本阶段不实现检票写入。

库存统计现在从 `match_seat_inventory` 实时返回 `AVAILABLE/LOCKED/SOLD/DISABLED` 四类数量。ADMIN座位布局以 `O/L/S/X` 展示四种状态，`LOCKED` 和 `SOLD` 不可通过静态管理操作切换。

阶段12已完成整单退票申请与审核。系统只允许已支付订单在比赛开始前 `REFUND_STOP_BEFORE_HOURS`（默认24小时）之外申请整单全额退款，退款金额直接使用历史订单 `total_amount`，不接受前端指定金额或部分退票。

### 阶段12退票流程

```text
PAID订单
↓ 用户提交整单退票申请
REFUND_PENDING（座位仍SOLD，电子票仍UNUSED）
↓ ADMIN审核

通过：订单REFUNDED → 明细REFUNDED → 电子票REFUNDED → 座位AVAILABLE
驳回：订单恢复PAID，明细、电子票、库存和支付记录保持不变
```

退票申请状态为 `PENDING/APPROVED/REJECTED`。审核通过会在同一事务中锁定申请、订单和电子票，确认所有票仍为 `UNUSED` 后统一更新申请、订单、明细、电子票和库存；任一步失败整体回滚。审核驳回只更新申请和订单。相同终态的重复审核幂等返回，`APPROVED`与`REJECTED`之间禁止反向转换。

USER接口：`POST /api/orders/{orderId}/refund`、`GET /api/refunds`、`GET /api/refunds/{id}`。ADMIN接口：`GET /api/admin/refunds`、`GET /api/admin/refunds/{id}`、`POST /api/admin/refunds/{id}/approve`、`POST /api/admin/refunds/{id}/reject`。CLUB和CHECKER不具备退票审核权限。

电子票退票后保留票码和历史记录并显示 `REFUNDED`，不允许入场；批准释放的比赛库存恢复为 `AVAILABLE` 且锁字段保持为空，可再次进入余票、最大连坐和选座算法。本阶段仍不包含检票、部分退票、比赛取消自动退款和真实资金退款。

### 阶段13检票结果约束迁移

全新数据库直接执行当前 `database/schema.sql`。已有数据库先检查和迁移历史值，再替换CHECK约束：

```sql
UPDATE checkin_record SET check_result = 'TICKET_USED' WHERE check_result = 'ALREADY_USED';
UPDATE checkin_record SET check_result = 'TICKET_VOID' WHERE check_result = 'VOID_TICKET';

SELECT checkin_id, match_id, ticket_id, scanned_ticket_code, check_result, remark
FROM checkin_record
WHERE check_result = 'INVALID';
```

`INVALID`必须结合 `remark`、`ticket_id`、关联订单和电子票状态逐条分类。只在查询结果为0，或全部记录已经可靠改成 `CODE_NOT_FOUND`、`ORDER_INVALID`、`TICKET_REFUNDED`等准确结果后，继续执行：

```sql
ALTER TABLE checkin_record DROP CHECK ck_checkin_result;
ALTER TABLE checkin_record
ADD CONSTRAINT ck_checkin_result CHECK (
    check_result IN (
        'SUCCESS', 'CODE_NOT_FOUND', 'WRONG_MATCH', 'ORDER_INVALID',
        'TICKET_USED', 'TICKET_REFUNDED', 'TICKET_VOID'
    )
);
```

不得将无法判断的 `INVALID` 静默统一映射为某个新状态。

### 阶段13检票入场

CHECKER只能处理 `home_club_id`等于当前账号 `clubId` 的主场比赛；未绑定俱乐部返回403。ADMIN可复用同一检票接口处理任意比赛。USER和CLUB不能执行检票。当前仅允许 `PUBLISHED`、`IN_PROGRESS` 比赛检票，`FINISHED`和`CANCELLED`拒绝新入场。

```text
电子票UNUSED
↓ 检票员核验票码、比赛、订单和票据状态
有效：UNUSED → USED，写used_at，记录SUCCESS
无效：电子票状态不变，记录明确失败原因
```

检票结果固定为 `SUCCESS/CODE_NOT_FOUND/WRONG_MATCH/ORDER_INVALID/TICKET_USED/TICKET_REFUNDED/TICKET_VOID`。每次业务核验尝试都写入 `checkin_record`；票码不存在时 `ticket_id`为空。权限失败和不允许检票的比赛状态在业务尝试前拒绝，不伪造检票记录。

成功检票先锁定订单，再锁定电子票，并用 `WHERE ticket_status='UNUSED'` 条件更新票据。并发扫描同一票最多一次 `SUCCESS`，其余结果为 `TICKET_USED`。检票和退票沿用一致的订单、票据锁顺序，最终电子票只能是 `USED`或`REFUNDED`之一。检票不修改订单的 `PAID`交易状态，也不修改库存的 `SOLD`销售状态。

接口：

- CHECKER/ADMIN：`GET /api/checker/matches`、`POST /api/checker/matches/{matchId}/checkin`
- CHECKER本人记录：`GET /api/checker/checkins`
- ADMIN全部记录：`GET /api/admin/checkins`、`GET /api/admin/checkins/{id}`

### 阶段14统计分析

阶段14提供只读运营统计，不修改订单、支付、退票、库存、电子票或检票状态，也不新增统计缓存表。ADMIN可查看全联赛数据；CLUB接口始终使用JWT当前用户绑定的`clubId`，只统计该俱乐部作为主队的比赛，忽略请求中自行传入的`clubId`。USER和CHECKER不能访问运营统计。

核心口径统一如下：

- 有效可售座位总数：本场`match_seat_inventory`中状态不为`DISABLED`的库存数。
- 当前有效售票量：`order_item.item_status='PAID'`的明细数；`REFUNDED`不计入。
- 成功入场人数：`e_ticket.ticket_status='USED'`的电子票数，作为上座率最终事实。
- 售票率：当前有效售票量 / 有效可售座位总数 × 100%；分母为0时返回0。
- 上座率：USED电子票数 / 有效可售座位总数 × 100%；分母为0时返回0。
- 毛销售额：`payment_record.pay_status='SUCCESS'`的`pay_amount`合计，包含后来已退款订单的历史成功支付。
- 退票金额：`refund_apply.refund_status='APPROVED'`的`refund_amount`合计。
- 净销售额：毛销售额 - 退票金额。
- 平均售票率、平均上座率：先计算每场比赛百分比，再对所筛选比赛做算术平均；无比赛时返回0。
- overview中的`paidOrders`统计当前仍为`PAID`或`REFUND_PENDING`的订单；`refundedOrders`统计`REFUNDED`订单。
- 退票率：已通过退票订单数 / 存在SUCCESS支付记录的去重订单数；分母为0时返回0。
- 热门比赛：按当前有效售票量降序，净销售额降序作为次级排序，最后按比赛时间与比赛ID稳定排序。

时间口径：比赛总览、比赛列表和俱乐部主场统计的`startTime/endTime`筛选`match_info.match_time`；销售趋势的成功支付按`payment_record.pay_time`归日，已批准退票按现有字段`refund_apply.audit_time`归日；退票申请数量按`refund_apply.created_at`筛选，批准金额仍只累计APPROVED；检票统计按`checkin_record.check_time`筛选。趋势中的`ticketsSold`是当日SUCCESS支付订单的历史支付票数，不等同于当前有效售票量。

ADMIN接口：

- `GET /api/admin/statistics/overview`
- `GET /api/admin/statistics/matches`、`GET /api/admin/statistics/matches/{matchId}`
- `GET /api/admin/statistics/clubs`
- `GET /api/admin/statistics/popular-matches`
- `GET /api/admin/statistics/sales-trend`
- `GET /api/admin/statistics/refunds`
- `GET /api/admin/statistics/checkins`

CLUB接口：`GET /api/club/statistics/overview`、`GET /api/club/statistics/matches`。ADMIN前端入口为`/admin/statistics`和`/admin/statistics/matches`，包含核心指标卡片、趋势表、热门比赛、俱乐部主场排行、检票/退票统计以及单场票区与检票异常详情；CLUB前端入口为`/club/statistics`。

检票失败统计继续使用阶段13固定枚举：`CODE_NOT_FOUND`、`WRONG_MATCH`、`ORDER_INVALID`、`TICKET_USED`、`TICKET_REFUNDED`、`TICKET_VOID`，同时返回SUCCESS、总尝试数和成功率。
