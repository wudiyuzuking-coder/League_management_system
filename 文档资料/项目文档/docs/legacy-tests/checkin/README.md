# 旧CHECKER检票集成测试归档

本目录保存项目历史版本的 `CheckinIntegrationTest.java`，源代码完整保留并由Git继续跟踪。

该测试依赖旧角色模型中的 `demo_checker`，验证独立CHECKER检票、检票记录以及检票与退票竞争。当前正式提交角色为 `USER`、`CLUB`、`EVENT_ADMIN`、`ADMIN`，检票不属于本次最终提交和验收范围，因此该文件已从 `backend/src/test/java` 移出，默认Maven测试不会编译或执行它。

本次归档没有使用 `@Disabled`，也没有在Surefire中增加隐藏排除规则；检票Service、Controller、前端遗留页面和历史测试实现均未为四角色版本改写。
