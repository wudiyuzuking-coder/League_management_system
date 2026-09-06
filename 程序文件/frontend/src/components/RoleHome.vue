<script setup>
defineProps({ title: { type: String, required: true }, description: { type: String, required: true } })
</script>

<template>
  <el-card class="role-home" shadow="never">
    <el-tag type="success" effect="dark">阶段3入口已授权</el-tag>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <el-alert title="当前仅完成账号、JWT和权限基础；具体业务将在后续阶段实现。" type="info" :closable="false" show-icon />
  </el-card>
</template>
