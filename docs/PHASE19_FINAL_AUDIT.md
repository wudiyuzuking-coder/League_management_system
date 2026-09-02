# 阶段19最终需求审计

审计日期：2026-09-02

审计基线：`030971f2751288370d84207ac97edb35ca8ec3b5`及当前尚未提交的阶段17A至18A正式实现

允许状态：`DONE`、`DEFERRED`、`CONFLICT_RESOLVED`、`CONFLICT_REMAINS`

本表逐条复核仓库根目录`版本待修改事项.md`。`DEFERRED`表示明确不属于当前冻结范围，不等同于缺陷。

| 原需求 | 当前状态 | 代码/文档证据 | 最终处理决定 |
|---|---|---|---|
| Windows下MySQL未加入PATH的处理 | DONE | README给出MySQL 8.0默认安装路径、版本检查和全路径导入示例 | 保留已验证命令 |
| `mysql -e source`兼容方案 | DONE | README同时提供cmd输入重定向导入 | 保留两种方式 |
| 提供`.env.example` | DONE | 根目录`.env.example`覆盖dev数据库、JWT、演示密码和上传目录 | 仅保留示例值 |
| `test-data.sql`导入前风险说明 | DONE | 文件头说明只用于空库/隔离验收且脚本保持幂等 | 保留 |
| 前端中文UTF-8 | DONE | 阶段19严格解码扫描93个文件，0个无效UTF-8、0个替换字符 | 不做格式化 |
| Node版本及PATH检查 | DONE | README要求20.19+或22.12+并给出`node -v`、`npm -v` | 保留 |
| 比赛页进入主客队详情 | DONE | `UserMatches.vue`、`UserMatchDetail.vue`、`UserClubDetail.vue` | 浏览器复核通过 |
| 俱乐部详情含资料、球员、教练、战绩与赛程 | DONE | `UserClubDetailServiceImpl`及7项阶段18A测试 | HTTP与浏览器通过 |
| 注册后修改昵称和头像 | DONE | `/api/profile`与`/api/profile/avatar`，统一资料页 | 四角色profile、USER完整头像E2E通过 |
| 手机号登录且全局唯一 | DONE | `LoginRequest.phone`、`uq_sys_user_phone`、阶段17B测试 | 保留 |
| username昵称化并允许重复 | DONE | `idx_sys_user_username`为普通索引，JWT主体为userId | 同昵称隔离E2E通过 |
| 账号资料字段分离 | DONE | phone、display_name/realName、employee_no、club_id分别存储和展示 | 保留；CLUB申请名暂存display_name为已知技术债 |
| 统一系统当前时间 | DONE | `SystemTimeService`与`sys_config`偏移模型 | 全角色可调，匿名拒绝 |
| 调整时间全局生效 | DONE | 统一时间接口和各业务Service集成 | HTTP与比分/报名/售票回归通过 |
| 时间调整操作日志 | DONE | `operation_log`记录调整人及前后时间 | 保留 |
| 所有核心时间判断接入统一时间 | DONE | 报名、排赛、比赛、售票、订单、支付、退款及公开剩余天数测试 | 107项回归通过 |
| USER赛季/赛程/俱乐部/场馆/票区/价格展示链 | DONE | 阶段18A公开DTO和前端详情链 | HTTP与浏览器通过 |
| 俱乐部最近比赛及剩余天数 | DONE | 公开俱乐部聚合详情使用SystemTime | 测试与HTTP通过 |
| 比赛详情场馆、区域、价格、余票、最大连坐 | DONE | 安全票区DTO和`TicketZoneList.vue` | 不暴露锁座明细 |
| CLUB查看可报名赛季 | DONE | `/api/club/enrollments/available-seasons` | 报名窗口E2E通过 |
| 赛季日期冲突校验 | DONE | 日期闭区间冲突规则 | 专项测试通过 |
| 固定东/西/南/北×VIP/普通八区 | DEFERRED | 当前为通用stadium/zone/seat模型 | 不破坏成熟通用模型 |
| VIP/普通排数及长边/宽边每排座位数 | DEFERRED | 当前无这些结构化字段 | 冻结阶段不新增字段 |
| 主场VIP/普通默认定价 | DEFERRED | 价格存于`match_ticket_zone.ticket_price` | 当前采用按比赛票区定价 |
| 报名球员姓名、位置、号码、出生年、首发/替补 | DONE | 报名快照表与既有球员模型；位置枚举保持四类 | 不要求首发恰好11人 |
| 按统一时间计算球员年龄 | DONE | 公开详情根据出生日期和SystemTime计算且不泄露生日 | 测试通过 |
| 报名教练及教练年龄 | DEFERRED | 报名教练已实现；`coach_info`无出生字段，年龄未实现 | 保留教练报名，不新增出生字段 |
| 至少11球员和1教练 | DONE | 报名Service校验及事务回滚测试 | 保留 |
| CLUB查看报名与最近比赛倒计时 | DONE | 报名列表/详情、已确认赛程与公开详情 | 浏览器通过 |
| EVENT_ADMIN申请后等待启用 | DONE | 公开申请状态为DISABLED，启用前不能登录 | 保留 |
| 赛季容量1至20 | DONE | season字段、Service校验及容量并发测试 | 保留 |
| 比赛开始至少晚于报名开始1个月 | DONE | 以`start_date 00:00:00`校验 | 保留 |
| 报名截止至少提前7天 | DONE | registrationDeadline约束 | 保留 |
| 满额或截止自动排赛 | DONE | AFTER_COMMIT满额触发、Scheduler截止触发 | E2E通过 |
| 主客场双循环且相邻轮至少6天 | DONE | Circle Method及日期容量校验 | 4队6轮12场复核通过 |
| EVENT_ADMIN确认赛程 | DONE | batch `GENERATED→CONFIRMED` | E2E通过 |
| 比赛日前禁止录比分 | DONE | SystemTime日期级限制 | 前一天409、当天200 |
| 赛果维护提醒 | DONE | 动态查询，不增加提醒表 | 完赛后消失 |
| 票务开售、价格、时间和库存 | DONE | 既有比赛票区、销售窗口和库存管理 | 本阶段仅回归，不改状态机 |
| 初始系统管理员账号 | DONE | `demo_admin`/SA0001演示账号 | 初始化器写BCrypt密码 |
| ADMIN预登记管理人员准入资料 | CONFLICT_REMAINS | 无独立预登记表；需求文档仍保留该限定 | 不在冻结阶段改变账号流程 |
| 公开申请必须匹配预登记姓名/工号 | CONFLICT_REMAINS | 当前公开申请做格式/唯一/角色匹配并置DISABLED，但不匹配预登记记录 | 明确记录差异 |
| ADMIN后台创建管理账号 | DONE | ADMIN用户管理创建EVENT_ADMIN/ADMIN | 与公开申请形成双入口 |
| EA####/SA####格式与全局唯一 | DONE | Service统一校验和`uq_sys_user_employee_no` | 重复返回409 |
| 数据库/JWT/8080启动故障说明 | DONE | README“常见启动问题” | 保留 |

## 检票历史模块

- `sys_role`正式seed只有USER、CLUB、EVENT_ADMIN、ADMIN；没有CHECKER，`test-data.sql`不创建`demo_checker`。
- 当前四角色菜单没有检票入口；README中的阶段13检票内容位于明确标记的历史演进章节。
- `CheckinIntegrationTest`完整归档于`docs/legacy-tests/checkin/`，默认Maven测试不编译、不执行，也未使用`@Disabled`或Surefire隐藏排除。
- 历史Controller、Service、数据库表和权限数据为版本追溯保留，不属于正式验收；本阶段不删除也不修改检票业务。

## 阶段19验收摘要

- 全新MySQL 8：`schema.sql → seed.sql → test-data.sql`成功，30表、4角色、4演示账号；二次执行数据脚本数量不变。
- 从可靠的阶段16前基线`0b64a59`实际执行phase16b、16c、17a、17b、17c迁移链；最终列、索引、约束签名与当前`schema.sql`完全一致。
- 后端：107项测试，Failures 0、Errors 0、Skipped 0；`clean test`和未跳过测试的`clean package`均成功。
- 运行：可执行JAR真实启动，健康检查、登录和核心接口正常；前端`npm run build`成功，仅保留既有chunk体积提示。
- 真实HTTP：四角色权限、匿名权限、USER购票/退款、CLUB审核/报名、自动排赛/确认、SystemTime回拨、比分/提醒、统计、头像及同昵称身份隔离全部通过。
- 数据一致性：15项检查全部0异常；四角色浏览器主要页面无404、死链接、菜单泄漏或新增控制台错误。

阶段19没有新增业务功能。建议停止新增业务功能并冻结当前版本。
