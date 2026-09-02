<script setup>
import { useAppStore } from '../stores/app'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import SystemTimeControl from '../components/SystemTimeControl.vue'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()
const menuActive = computed(() => ['/user/profile','/user/seasons','/user/matches','/user/orders','/user/tickets','/user/refunds'].find(root => router.currentRoute.value.path === root || router.currentRoute.value.path.startsWith(`${root}/`)) || router.currentRoute.value.path)
const logout = () => {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="user-layout">
    <el-header class="user-header">
      <span class="brand">{{ appStore.appName }}</span>
      <div class="header-actions">
        <SystemTimeControl />
        <el-avatar :size="32" :src="authStore.user?.avatarUrl || undefined" :icon="UserFilled" />
        <span>{{ authStore.user?.username }}</span>
        <el-tag effect="plain">普通用户</el-tag>
        <el-button link type="danger" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main>
      <el-menu router mode="horizontal" :default-active="menuActive" class="user-menu">
        <el-menu-item index="/user/profile">账号资料</el-menu-item>
        <el-menu-item index="/user/seasons">联赛赛季</el-menu-item>
        <el-menu-item index="/user/matches">比赛列表</el-menu-item>
        <el-menu-item index="/user/orders">我的订单</el-menu-item>
        <el-menu-item index="/user/tickets">我的电子票</el-menu-item>
        <el-menu-item index="/user/refunds">我的退票</el-menu-item>
      </el-menu>
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.user-layout {
  min-height: 100vh;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.brand {
  color: #166534;
  font-size: 20px;
  font-weight: 700;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-menu { margin-bottom: 20px; }
</style>
