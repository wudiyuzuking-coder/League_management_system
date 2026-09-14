# 足球联赛管理与在线票务系统

本目录包含系统的后端、前端、数据库脚本和本地运行脚本。当前正式角色只有：

- `USER`：浏览赛事、维护预填购票人、购票、支付、查看电子票和自动退票。
- `CLUB`：维护俱乐部、私有标准主场、人员阵容、赛季报名、已确认赛程和俱乐部数据。
- `EVENT_ADMIN`：创建赛季、确认自动赛程、维护比赛票务、独立提交赛果并查看赛季营收。
- `ADMIN`：分别管理普通用户、内部管理人员和俱乐部，完成俱乐部审核及冲突赛果确认。

`CHECKER` 不是当前正式角色，也不属于当前菜单、账号和答辩流程。

## 当前业务规则

### 账号

- 登录按 `phone + roleCode` 精确定位账号；同一手机号允许跨角色复用，同角色手机号唯一。
- JWT 的 `sub` 为 `userId`，业务授权仍以服务端重新加载的账号及角色为准。
- 管理人员工号全局唯一。EVENT_ADMIN 使用 `EA####`，ADMIN 使用 `SA####`。
- 管理账号由 ADMIN 预登记为 `PENDING_ACTIVATION`，本人首次启用后进入 `ENABLED`。
- CLUB 注册后为 `PENDING_CLUB_APPROVAL`。审核只允许创建并绑定新的俱乐部，不支持关联已有俱乐部。
- `DISABLED`、`LOCKED`、`CANCELLED`、待首次启用和待俱乐部审核具有独立语义。注销采用软注销并保留全部历史业务引用；已绑定俱乐部的 CLUB 账号不能直接注销，最后一个可用 ADMIN 不能注销。

### CLUB 与 STANDARD_8

- 每个 CLUB 维护一个私有 `STANDARD_8` 主场，不从共享场馆列表选择。
- 每个标准主场包含 EAST/WEST/SOUTH/NORTH 四个方向，每个方向各有 VIP 和 NORMAL 两个独立物理票区。
- VIP 使用前排 `1..rowsPerZone`；NORMAL 使用后排 `rowsPerZone+1..rowsPerZone*2`。
- 容量由 `(长边每排座位数 + 短边每排座位数) × 2 × 每区排数 × 2` 派生。
- VIP 默认价格必须高于 NORMAL。主场结构及人员阵容不合规时，后端拒绝赛季报名。
- 合规阵容为 11 名首发、恰好 1 名首发门将、最多 7 名替补、1 名现役主教练和最多 2 名现役副教练。
- 报名成功后保存球员、教练和主场快照，当前资料调整不改写历史报名。

### 赛季、售票与订单

- EVENT_ADMIN 创建赛季时填写赛季名称、开始日期和 2～20 的参赛队伍上限。
- 报名窗口、全局售票开始时间和预计结束日期由系统基于 `SystemTime` 与正式排赛规则生成。
- 正式赛程为主客场双循环，每天最多一场，同一 CLUB 相邻比赛至少间隔 6 天，统一 20:00。
- 每场停售时间固定为比赛开始前 1 小时。查询、Preview 和 Order 共用唯一 `TicketSalePolicy`。
- USER 在 STANDARD_8 比赛只选择 VIP 或普通及数量；后端选择一个可承载本单的实际方向票区，一单仍只有一个 `match_zone_id`，不跨方向拼单。
- `maxContinuousCount` 只用于连坐提示。库存足够但无法完全连坐时仍允许购买，并按现有算法尽量分配连坐座位。
- 预填购票人按身份证号识别：每个 USER 最多 4 人，同一身份证号最多由 4 个不同 USER 保存。订单明细保存姓名和身份证号快照。
- USER 退票由系统直接处理：比赛开始至少 7 天前退款 100%，不足 7 天但仍在允许期限内退款 50%；不进入 EVENT_ADMIN 审核。

### 赛果与营收

- 不同 EVENT_ADMIN 的赛果提交相互独立，不得覆盖。
- 两名不同 EVENT_ADMIN 提交相同比分时自动发布；单人提交或冲突提交进入 ADMIN 待确认。
- 只有正式发布的最终比分才参与积分榜和比赛统计。
- 有效票务营收排除退款订单，主场 CLUB、平台、客场 CLUB 分别按 60%、10%、30% 计算。

## 数据库脚本

- `database/schema.sql`：全新环境的当前完整结构快照。
- `database/seed.sql`：角色、权限和系统基础配置。
- `database/test-data.sql`：仅供隔离开发环境使用的可选样例数据。
- `database/migrations/`：已有历史数据库的增量升级记录。

全新环境只执行当前 `schema.sql`，随后执行 `seed.sql`。不要在当前完整 schema 上再次执行历史增量 migration。

历史数据库只能在确认其基线版本、完成备份和只读 preflight 后，按文件名中的阶段顺序逐个执行适用 migration。migration 不是一套可在任意状态下重复运行的幂等脚本。

## 本地运行

后端使用 Java 17、Spring Boot 3.5 和 MySQL 8；前端使用 Vue 3、Vite 7、Pinia 和 Element Plus。

通过环境变量注入数据库和 JWT 配置，不要将密码或 secret 写入版本库：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:mysql://localhost:3306/league_ticket?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "db_user"
$env:DB_PASSWORD = "your-local-password"
$env:JWT_SECRET = "replace-with-a-random-secret-of-at-least-32-bytes"
$env:APP_UPLOAD_DIR = "./uploads"

cd backend
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd frontend
npm ci
npm run dev
```

开发服务器默认将 `/api` 和 `/uploads` 转发到后端。上传仅接受不超过 2MB 的真实 JPEG/PNG 文件，文件名使用 UUID，运行时上传目录不进入 Git。

## 测试与构建

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd clean package

cd ..\frontend
node --test tests/*.test.js
npm run build
```

数据库集成测试必须使用允许测试写入或事务回滚的隔离数据库，并显式设置 `RUN_DB_TESTS=true`。不得把破坏性测试指向正式库或保留答辩数据的开发库。

最近一次 Freeze Audit：后端发现 153 项测试，实际运行 41、跳过 112、失败 0；前端 21 项测试全部运行通过；后端打包和前端生产构建成功。跳过的数据库门控测试不计作实际通过。

## 安全边界

- `.env`、数据库 dump、MySQL 登录文件、构建日志、运行时 uploads、IDE 配置和本地测试输出不得提交。
- `application-dev.yml` 中只保留环境变量引用或非敏感本地默认值。
- 示例密码初始化器默认关闭，只能用于可丢弃的隔离样例数据库。
- `database/test-data.sql` 不得导入包含真实数据的数据库。
