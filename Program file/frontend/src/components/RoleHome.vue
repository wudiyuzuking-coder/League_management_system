<script setup>
defineProps({ title: { type: String, required: true }, description: { type: String, required: true } })
</script>

<template>
  <el-card class="role-home" shadow="never">
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
  </el-card>
</template>
