# 足球联赛购票系统测试报告

## 当前验收范围

- 验收日期：2026-09-02（阶段19最终冻结验收）
- 正式角色：`USER`、`CLUB`、`EVENT_ADMIN`、`ADMIN`
- Java编译目标：17（本机运行时 Java 21.0.8）
- Spring Boot：3.5.7
- MySQL：8.0.44 Community Server
- 隔离数据库：`127.0.0.1:3317`（本轮新建MySQL 8数据目录，未复用此前验收库）
- 前端：Vue 3、Vite 7.3.6

项目历史版本曾实现独立CHECKER检票模块。当前四角色提交版本未将该模块纳入正式验收范围，检票Service、Controller和前端遗留页面继续保留用于版本追溯；本报告不把检票写成当前正式业务流程。

## 历史检票测试归档

旧版 `CheckinIntegrationTest.java` 共5个测试，依赖当前初始化数据中已不存在的 `demo_checker`。测试源码已完整移动到 `docs/legacy-tests/checkin/CheckinIntegrationTest.java`，由Git继续跟踪，但不再位于默认Maven测试源码目录。

本次没有使用 `@Disabled`，没有修改Surefire添加隐藏exclude，也没有把旧检票测试迁移给EVENT_ADMIN或ADMIN。默认测试目录因此只包含当前正式需求测试。

## 数据库初始化

空隔离实例依次执行 `database/schema.sql → database/seed.sql → database/test-data.sql` 成功，共创建30张业务表（原25张、3张阶段16B报名表、2张阶段16C赛程批次表）。正式角色为4个，正式演示账号为4个。再次执行 `seed.sql` 和 `test-data.sql` 后数量保持不变，演示脚本幂等检查通过。

正式演示账号以手机号登录：

- USER：`13800000001`，昵称`demo_user`
- CLUB：`13800000003`，昵称`demo_club`
- EVENT_ADMIN：`13800000005`，昵称`demo_event_admin`，工号`EA0001`
- ADMIN：`13800000002`，昵称`demo_admin`，工号`SA0001`

阶段17A在 `sys_user` 新增可空且全局唯一的 `employee_no VARCHAR(16)`。USER、CLUB保持NULL；EVENT_ADMIN使用 `EA####`，ADMIN使用 `SA####`。迁移脚本只自动回填上述两个明确演示账号，其他历史管理账号需人工补齐后才能启用。

阶段17C在`sys_user`新增`avatar_url VARCHAR(255) NULL`，只保存头像相对访问路径，历史账号保持NULL并使用默认头像。空库仍为30张表；阶段17B库执行等价升级SQL后表数不变，历史账号非空头像数为0。

## 自动化测试结果

使用 `RUN_DB_TESTS=true`、隔离数据库和测试用JWT密钥执行：

```text
Tests run: 107
Failures: 0
Errors: 0
Skipped: 0
```

| 测试套件 | 数量 | 结果 |
|---|---:|---|
| 连坐算法单元测试 | 8 | 通过 |
| 认证、四角色与CLUB绑定 | 6 | 通过 |
| 管理人员工号、创建编辑、启用与CLUB回归 | 6 | 通过 |
| 手机号登录、重复昵称、JWT主体与资料修改 | 5 | 通过 |
| 四角色头像、公开读取、替换删除与异常文件防护 | 6 | 通过 |
| USER俱乐部公开详情、时间计算、权限与票区安全映射 | 7 | 通过 |
| 俱乐部管理 | 4 | 通过 |
| CLUB赛季报名、容量并发、事务与权限 | 7 | 通过 |
| 自动双循环排赛、确认、触发与权限 | 7 | 通过 |
| 健康检查与404映射 | 2 | 通过 |
| 联赛赛季轮次 | 3 | 通过 |
| 应用上下文 | 1 | 通过 |
| 比赛管理 | 3 | 通过 |
| 比分日期限制、动态赛果提醒与权限 | 6 | 通过 |
| 订单与锁座 | 6 | 通过 |
| 支付与电子票 | 6 | 通过 |
| 退票 | 5 | 通过 |
| 连坐数据库并发 | 3 | 通过 |
| 场馆与静态座位 | 3 | 通过 |
| 统计分析 | 4 | 通过 |
| 统一系统时间、权限、日志与业务窗口 | 6 | 通过 |
| 比赛票区与库存 | 3 | 通过 |

阶段16B新增7项报名测试，阶段16C新增7项排赛测试，阶段16D新增6项比分与提醒测试，阶段17A新增6项管理工号测试。阶段17B新增5项手机号身份专项测试，覆盖四角色手机号登录、错误手机号/密码、DISABLED账号、username-only拒绝、重复手机号409、同昵称不同手机号与不同userId、JWT subject为userId、改为已有昵称、手机号修改和历史空手机号启用拦截。阶段17C新增6项头像与统一资料专项测试，覆盖JPEG/PNG、静态读取、替换、删除、空文件与伪造/不匹配/超限文件、四角色字段分离、JWT身份和路径安全。阶段18A新增7项专项测试，覆盖公开聚合详情、敏感字段排除、ACTIVE过滤与号码排序、统一系统时间年龄/剩余天数、公开比赛状态、权限与不存在/停用俱乐部、票区安全映射和DRAFT隐藏。阶段17C的原100项正式测试全部继续通过，没有禁用、跳过或降低原断言。

### 阶段18A USER俱乐部详情与购票信息链

- USER可从比赛的主队、客队分别进入正确的俱乐部详情；非USER角色访问USER专用详情接口返回403。
- 详情只返回ACTIVE球员和教练，球员按号码排序；年龄与下一场剩余天数均使用统一系统时间计算，缺少生日时显示为空且不暴露出生日期。
- 战绩读取当前/最近正式赛季的`club_season_record`；最近比赛只包含FINISHED，未来比赛只包含PUBLISHED，DRAFT不公开，未结束比赛比分保持空值。
- 比赛票区以安全DTO返回物理座位、ACTIVE座位、排数、座号范围、票价、余票、最大连坐和当前可购状态，不暴露创建人、锁座订单或内部库存状态。

### 比分日期限制与赛果提醒

- 统一系统日期早于比赛日期时比分接口返回409；比分字段和积分榜均保持不变。
- 比赛当天按日期放行，不要求到具体开球时刻；之后日期同样允许，前提是原比赛状态允许。
- `DRAFT`、`PUBLISHED`、`CANCELLED` 继续由原状态机拒绝比分录入。
- 提醒接口只返回日期已到的 `PUBLISHED`、`IN_PROGRESS`，动态计算 `TODAY/OVERDUE` 和 `daysOverdue`；不新增表、字段或Scheduler。
- `FINISHED` 不进入提醒；合法比分录入并完成原状态流程后，提醒自动消失且积分榜正确重算。
- 统一系统时间调整到比赛当天时提醒出现，回拨到前一天时未完成比赛的提醒消失。

### 自动排赛与确认

- 4队：6轮12场；每队6场、3主3客；任意两队恰好一主一客；每轮每队恰好一场。
- 5队：10轮20场；每队8场、4主4客；每轮2场且1队轮空；BYE没有写入数据库。
- 满额报名提交后AFTER_COMMIT自动生成；未满员但到截止时间后Scheduler扫描生成；未截止、单队和日期容量不足均拒绝。
- 两个并发generate请求最终仅1个批次、正确轮次数和比赛数；系统时间回拨后报名仍被拒绝。
- GENERATED对CLUB隐藏；EVENT_ADMIN确认后CLUB只看到自己的赛程，比赛仍保持DRAFT。

真实HTTP补充验收：

- 场景A（满额）：4个CLUB依次报名均返回200；最后一个名额提交后生成 `FULL/GENERATED` 批次，6轮12场；EVENT_ADMIN确认后为 `CONFIRMED`，CLUB只看到自己的6场比赛。
- 场景B（截止）：2个CLUB报名、`maxClubs=4`，统一系统时间移动到deadline后，真实Scheduler在10秒内生成 `DEADLINE/GENERATED` 批次，2轮2场。
- 场景C（重复）：连续generate返回同一batchId，数据库仍为1个批次和12场比赛。
- 场景D（回拨）：生成后系统时间回到报名窗口，可报名列表不再显示该赛季，继续报名返回409。
- 场景E（权限）：USER、CLUB、ADMIN确认接口均403；EVENT_ADMIN为200。数据库复核所有自动比赛仍为DRAFT，场馆与主队报名快照完全一致。

## 真实HTTP验收

### 阶段18A USER俱乐部详情与购票链

- A：USER手机号登录后查询主队俱乐部详情返回200，得到11名ACTIVE球员、1名ACTIVE教练、当前赛季积分和未来比赛；公开响应未出现`phone`、`employeeNo`、`userId`、报名快照、锁座字段或出生日期。
- B：同一场比赛的客队链接进入正确的另一俱乐部，返回clubId与页面名称一致。
- C：统一系统时间距下一场4天时`daysUntilNextMatch=4`；移动到比赛日为0，且不会出现负数。
- D：比赛详情返回场馆“滨江足球场”、地址“滨江区演示路1号”和容量48；东看台返回票价120、物理座位24、ACTIVE座位24、3排、座号1至8以及最大连坐8。
- E：2张连坐Preview、创建订单和支付均成功；数据库最终为`order=PAID`、2个`item=PAID`、2个`inventory=SOLD`、2张`ticket=UNUSED`。
- F：DRAFT比赛不出现在公开详情或票区查询；不存在/停用俱乐部返回404，CLUB、EVENT_ADMIN、ADMIN不能访问USER专用详情。

浏览器实机验收：比赛列表中的主队、客队名称均可独立点击；主队详情展示基础资料、11名球员、教练、积分和未来比赛，客队链接进入正确俱乐部；比赛详情展示场馆地址、容量、票价、余票、最大连坐、座位结构和可购状态；点击“检查连坐”实际返回“当前可满足1张连坐：1排，3座”。

### 阶段17C用户资料与头像

- A：USER初始`avatarUrl=NULL`；上传真实PNG返回200并得到`/uploads/avatars/{uuid}.png`，直接GET图片返回200。
- B：再次上传后URL变化，数据库只指向新头像；旧文件按事务提交结果清理。
- C：移除头像返回200，资料接口恢复`avatarUrl=NULL`，页面显示默认图标。
- D：文本文件、伪造JPG和扩展名/真实格式不一致文件均返回400。
- E：超过2MB文件返回413。
- F：USER、CLUB、EVENT_ADMIN、ADMIN均可读取资料和上传头像；CLUB返回绑定`clubId`，管理角色分别返回`EA0001`、`SA0001`，字段没有混用。
- G：USER修改昵称返回200，原JWT继续访问且`userId`与JWT subject均为1；浏览器顶部昵称立即更新并在刷新后保持。
- H：头像功能上线后，USER两张连坐Preview、下单、支付均成功；数据库为order=PAID、2个item=PAID、2个inventory=SOLD、2张ticket=UNUSED。

数据库双路径验收：空库执行`schema.sql → seed.sql → test-data.sql`并重复数据脚本后仍为30表、4角色、4演示账号，`avatar_url`存在且可空；阶段17B库增加该字段后仍为30表，历史账号均保持NULL。

浏览器实机验收：USER上传头像后资料页和顶部栏同时出现同一新头像；修改昵称后顶部立即更新，刷新后仍保持；移除头像后两处恢复默认图标。EVENT_ADMIN资料页分别显示真实姓名、手机号和`EA0001`，ADMIN分别显示真实姓名、手机号和`SA0001`。

### 阶段17B手机号身份

- A（同昵称）：两个`username=柚子`、不同手机号的USER均注册200、登录200，`/me`返回不同userId。
- B（手机号唯一）：重复注册已有手机号返回409，消息为“手机号已存在”。
- C（昵称修改）：使用原Token修改昵称返回200，继续调用`/me`为200并返回新昵称，userId不变。
- D（停止旧登录）：只提交`username + password`返回400。
- E（四角色）：USER、CLUB、EVENT_ADMIN、ADMIN均通过固定手机号登录200，角色码正确。
- F（CLUB）：profile、players、coaches、enrollments、schedules均为200。
- G（USER）：2张连坐Preview 200、下单200、支付200；数据库为order=PAID、2个item=PAID、2个inventory=SOLD、2张ticket=UNUSED。
- H（EVENT_ADMIN）：seasons、enrollments、schedules、result-reminders、statistics/overview均为200；ADMIN用户和俱乐部管理也均为200。

数据库双路径验收：空库执行schema、seed、test-data并重复执行数据脚本后仍为30表、4角色、4演示账号；username普通索引且同昵称可插入2条，phone唯一索引拒绝重复。阶段17A库执行`phase17b_phone_login.sql`前用户名/手机号/工号均为唯一索引，执行后username为普通索引、phone与employee_no唯一保持，启用账号空手机号为0，表数仍为30。

浏览器实机验收：登录页显示“使用手机号登录”和“请输入手机号”；四角色依次通过手机号进入`/user/seasons`、`/club/profile`、`/admin/matches`、`/admin/users`。页面顶部显示昵称而非手机号；USER在账号资料页修改昵称后刷新，原会话仍有效且顶部显示新昵称；ADMIN列表按userId、手机号、昵称、真实姓名、工号分列展示。

### 阶段17A管理人员工号

- 场景A：EVENT_ADMIN以真实姓名“张三”和工号`EA0101`公开申请返回200，数据分别落入姓名与工号字段，初始为DISABLED；启用前登录403，ADMIN启用200，启用后登录200；赛季、比赛、报名、赛程、赛果提醒和统计关键接口均为200。
- 场景B：EVENT_ADMIN提交`SA0101`返回400，响应为“赛事管理员工号必须为EA加4位数字”。
- 场景C：ADMIN创建姓名“李四”、工号`SA0101`的系统管理员返回200；另一账号重复使用该工号返回409，响应为“管理人员工号已存在”；ADMIN用户管理和俱乐部管理接口均为200。
- 场景D：直接构造历史EVENT_ADMIN且`employee_no=NULL`，启用返回409且保持DISABLED；后台补为`EA0102`后编辑200、启用200。
- 场景E：CLUB公开注册200且初始DISABLED，未绑定启用409，绑定`clubId` 200，启用200，登录200。
- 数据库复核：`EA0101`、`EA0102`、`SA0101`各仅1条；姓名与工号分列保存；CLUB的`employee_no`为NULL。

### 阶段16D比分日期限制与动态提醒

- 场景A（未来日期）：统一系统日期早于比赛日期时录入比分返回409，响应消息为“比赛日期尚未到达，暂不能录入比分”；数据库比分仍为NULL，积分榜未受污染。
- 场景B（当天日期）：系统时间移动到比赛当天后录入比分返回200，完成比赛返回200；比赛为FINISHED，积分榜按比分正确重算。
- 场景C（动态提醒）：比赛当天的未完成比赛出现在提醒接口，类型为TODAY、`daysOverdue=0`；查询结果和菜单数量一致。
- 场景D（完成消失）：IN_PROGRESS比赛录入比分并结束后，提醒总数立即减少，已完成比赛不再出现在列表。
- 场景E（权限）：USER、CLUB、ADMIN访问提醒接口均为403，EVENT_ADMIN为200。

前端实机验收确认：EVENT_ADMIN可见“赛果待维护（数量）”菜单；列表可显示TODAY/OVERDUE、逾期天数、状态和维护提示；操作入口复用既有比赛维护页；录入比分并结束后菜单数量由7降为6且该比赛从列表消失；恢复真实时间后菜单和列表动态重算为0。验收中发现并修正了菜单将Vue `ref`对象渲染成`[object Object]`的问题，修正后显示实际数字。

### CLUB赛季报名

- 场景A：CLUB查看可报名赛季、确认默认主场、选择11名本队球员和1名本队教练，提交200；详情为 `SUBMITTED`。
- 场景B：同CLUB重复报名同赛季返回409。
- 场景C：报名日期闭区间重叠赛季返回409。
- 场景D：伪造其他俱乐部球员返回403，未留下报名数据。
- 场景E：EVENT_ADMIN报名列表和详情均返回200，页面及接口没有批准/拒绝操作。
- 场景F：`max_clubs` 已满后赛季从可报名列表消失，继续POST返回409。
- 场景G：两个不同CLUB并发争夺最后一个名额，结果为200/409，数据库最终报名数为1。
- 场景H：系统时间在报名开始前、窗口内、截止后得到不可报名/可报名/不可报名，资格判断使用 `SystemTimeService`。

### CLUB账号审核

`CLUB注册200 → DISABLED且clubId为空 → 未绑定直接启用409 → ADMIN绑定clubId 200 → 启用200 → CLUB登录200 → /api/club/profile 200`。

### USER购票支付

USER浏览比赛和票区，执行2张连坐Preview、创建订单并模拟支付成功。最终状态为：

```text
order = PAID
item = PAID
inventory = SOLD
ticket = UNUSED
```

流程到电子票 `UNUSED` 为止，不执行检票。

### 整单退票

USER创建并支付另一张2票订单，提交整单退票；EVENT_ADMIN审核通过。最终状态为：

```text
order = REFUNDED
item = REFUNDED
ticket = REFUNDED
inventory = AVAILABLE
```

### 运营统计

EVENT_ADMIN访问 overview、matches、clubs、popular-matches、sales-trend、refunds 均成功。本轮不验证checkins统计接口。

## 阶段19迁移链验收

从Git提交`0b64a59`中的可靠阶段16前`schema.sql`构造旧库，并依次实际执行：

1. `phase16b_club_season_enrollment.sql`
2. `phase16c_auto_schedule.sql`
3. `phase17a_employee_no.sql`
4. `phase17b_phone_login.sql`
5. `phase17c_user_avatar.sql`

五个脚本均执行成功，未删除已有业务数据。迁移后为30表、290列、121个索引和174个约束；与当前`schema.sql`逐项比较，列差异0、索引差异0、约束差异0。迁移文件是按版本只执行一次的升级脚本，不承诺对同一库重复执行；phase17B会输出空手机号和重复手机号审计结果，发现历史异常时必须人工处理后再继续，不能假设脚本自动推导缺失手机号。

## 阶段19数据一致性

十五项当前范围一致性查询均返回0：

- PENDING_PAYMENT订单没有LOCKED库存
- PAID订单库存非SOLD
- PAID订单明细非PAID
- PAID订单票数与电子票数量不一致
- REFUNDED订单库存未恢复AVAILABLE
- REFUNDED订单电子票非REFUNDED
- LOCKED库存缺少lock_order_id
- 报名主表存在但0球员
- 报名主表存在但0教练
- 报名数量超过season.max_clubs
- 同season、同club重复报名
- schedule batch关联比赛数与记录不符
- 同一season存在多个schedule batch
- schedule_match引用不存在批次或比赛
- FINISHED比赛缺少主队或客队积分记录

不检查USED或其他检票相关状态。

## 阶段19真实运行与浏览器验收

- 使用`java -jar backend/target/league-ticket-backend-0.0.1-SNAPSHOT.jar`连接3317隔离库启动成功，`/api/health`为200，手机号登录和核心接口正常。
- 四角色真实HTTP权限矩阵逐格调用：允许项为200或进入预期业务校验的400/409，越权项为403；匿名访问系统时间、管理、订单、CLUB资料、USER专用俱乐部详情和头像上传均为401，公开头像读取为200。
- USER完成注册、手机号登录、昵称、头像、公开俱乐部详情、2座连坐Preview、下单、支付、电子票和退款；EVENT_ADMIN批准后数据库为order/item/ticket REFUNDED、inventory AVAILABLE。
- 新CLUB注册后为DISABLED且clubId为空，直接启用409；ADMIN绑定后启用，CLUB登录及profile/players/coaches均200。
- 4个CLUB报名后生成FULL批次，6轮12场；EVENT_ADMIN确认后CLUB只看到自己的6场。时间回拨不能重复报名或生成第二批次。
- 比赛前一天录分409；比赛当天按正常状态流转录分、完赛均成功，积分榜更新且赛果提醒消失。
- EVENT_ADMIN的overview、matches、clubs、popular-matches、sales-trend、refunds均200；不验证checkins统计。
- 两个手机号不同、昵称同为“柚子”的USER拥有不同userId、资料和订单；修改A昵称后原JWT仍有效。
- USER、CLUB、EVENT_ADMIN、ADMIN浏览器菜单及关键页面逐项打开，无404、死链接、角色菜单泄漏或新增控制台错误。首次使用旧浏览器来源时出现两条失效会话错误；切换干净来源重新登录后不再新增。

## 已知延期/非当前范围

- 独立CHECKER、检票正式业务和检票前端入口；历史代码及测试仅保留追溯。
- 固定东/西/南/北×VIP/普通八区模板、长边/宽边每排座位数。
- 主场VIP/普通默认价格；当前按`match_ticket_zone.ticket_price`进行比赛票区定价。
- 教练年龄；`coach_info`缺少出生日期/出生年。
- 管理人员先由ADMIN预登记姓名/工号再本人注册的旧需求仍与当前双入口账号流程冲突，状态为`CONFLICT_REMAINS`。
- 直接发送缺少multipart boundary的畸形头像请求当前返回500；真实PNG/JPEG的正常上传、替换、删除和安全校验均通过。这是低优先级API健壮性项，不阻塞正式演示。

## 构建结论

- `clean test`：BUILD SUCCESS，107/0/0/0。
- `clean package`：未使用 `-DskipTests`，BUILD SUCCESS，107/0/0/0，并生成可执行JAR。
- `npm run build`：SUCCESS。
- 非阻塞提示：Element Plus公共包仍超过Vite默认500 kB提示，本轮不调整其引入方式。
- 本轮敏感信息扫描未发现真实数据库密码、真实JWT密钥、私钥或token进入Git；`.env.example`仅含占位值，uploads、target、dist和隔离MySQL数据目录均未跟踪。
- UTF-8严格扫描`frontend/src`、README和docs共93个文件：无效UTF-8为0，替换字符为0。
