<template>
  <main class="state-page">
    <el-result icon="warning" title="403" sub-title="当前账号无权访问该角色区域">
      <template #extra><el-button type="primary" @click="$router.push('/')">返回角色首页</el-button></template>
    </el-result>
  </main>
</template>
