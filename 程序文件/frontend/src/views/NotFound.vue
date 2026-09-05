<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="not-found-page">
    <el-result icon="warning" title="404" sub-title="你访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="router.push('/login')">返回登录页</el-button>
      </template>
    </el-result>
  </main>
</template>

<style scoped>
.not-found-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
}
</style>
