<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <el-card class="placeholder-card" shadow="never">
    <el-result icon="info" :title="title" :sub-title="description" />
  </el-card>
</template>

<style scoped>
.placeholder-card {
  max-width: 760px;
  margin: 48px auto;
}
</style>
