# 足球联赛购票系统测试报告

## 当前验收范围

- 验收日期：2026-08-31
- 正式角色：`USER`、`CLUB`、`EVENT_ADMIN`、`ADMIN`
- Java编译目标：17（本机运行时 Java 21.0.8）
- Spring Boot：3.5.7
- MySQL：8.0.44 Community Server
- 隔离数据库：`127.0.0.1:3316/league_ticket`
- 前端：Vue 3、Vite 7.3.6

项目历史版本曾实现独立CHECKER检票模块。当前四角色提交版本未将该模块纳入正式验收范围，检票Service、Controller和前端遗留页面继续保留用于版本追溯；本报告不把检票写成当前正式业务流程。

## 历史检票测试归档

旧版 `CheckinIntegrationTest.java` 共5个测试，依赖当前初始化数据中已不存在的 `demo_checker`。测试源码已完整移动到 `docs/legacy-tests/checkin/CheckinIntegrationTest.java`，由Git继续跟踪，但不再位于默认Maven测试源码目录。

本次没有使用 `@Disabled`，没有修改Surefire添加隐藏exclude，也没有把旧检票测试迁移给EVENT_ADMIN或ADMIN。默认测试目录因此只包含当前正式需求测试。

## 数据库初始化

空隔离实例依次执行 `database/schema.sql → database/seed.sql → database/test-data.sql` 成功，共创建30张业务表（原25张、3张阶段16B报名表、2张阶段16C赛程批次表）。正式角色为4个，正式演示账号为4个。再次执行 `seed.sql` 和 `test-data.sql` 后数量保持不变，演示脚本幂等检查通过。

正式演示账号：

- `demo_user`
- `demo_club`
- `demo_event_admin`
- `demo_admin`

## 自动化测试结果

使用 `RUN_DB_TESTS=true`、隔离数据库和测试用JWT密钥执行：

```text
Tests run: 83
Failures: 0
Errors: 0
Skipped: 0
```

| 测试套件 | 数量 | 结果 |
|---|---:|---|
| 连坐算法单元测试 | 8 | 通过 |
| 认证、四角色与CLUB绑定 | 6 | 通过 |
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

阶段16B新增7项报名测试，覆盖时间窗口、截止边界、赛季闭区间冲突、重复报名、主场和人员归属、人数与教练下限、事务无残留、EVENT_ADMIN只读权限、同CLUB并发重复和不同CLUB争夺最后一个名额。阶段16C新增7项测试，覆盖4队/5队Circle Method、BYE不落库、主客反转、6天间隔、主场快照、满额AFTER_COMMIT、截止Scheduler、资格拒绝、日期容量、确认可见性、积分榜幂等、时间回拨、并发生成和四角色权限。阶段16D新增6项测试，覆盖未来日期409且无写入、当天零点后允许、之后日期允许、时间回拨、DRAFT/PUBLISHED/CANCELLED状态保持、TODAY/OVERDUE与排序过滤、FINISHED自动消失、积分榜重算和四角色权限。原77项正式测试均保留并通过。

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

## 数据一致性

七项当前范围一致性查询均返回0：

- PENDING_PAYMENT订单没有LOCKED库存
- PAID订单库存非SOLD
- PAID订单明细非PAID
- PAID订单票数与电子票数量不一致
- REFUNDED订单库存未恢复AVAILABLE
- REFUNDED订单电子票非REFUNDED
- LOCKED库存缺少lock_order_id

不检查USED或其他检票相关状态。

## 构建结论

- `clean test`：BUILD SUCCESS，83/0/0/0。
- `clean package`：未使用 `-DskipTests`，BUILD SUCCESS，83/0/0/0，并生成可执行JAR。
- `npm run build`：SUCCESS。
- 非阻塞提示：Element Plus公共包仍超过Vite默认500 kB提示，本轮不调整其引入方式。
