<script setup>
import { useAppStore } from '../stores/app'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import { computed } from 'vue'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()
const menuActive = computed(() => {
  const path = router.currentRoute.value.path
  if (path.startsWith('/admin/statistics')) return '/admin/statistics'
  const roots = ['/admin/users','/admin/clubs','/admin/seasons','/admin/matches','/admin/stadiums','/admin/refunds','/admin/checkins','/club/profile','/club/players','/club/coaches','/club/stats','/club/matches','/club/statistics']
  return roots.find(root => path === root || path.startsWith(`${root}/`)) || path
})
const roleLabels = {
  CLUB: '俱乐部负责人',
  EVENT_ADMIN: '赛事管理员',
  ADMIN: '系统管理员',
}
const menus = {
  CLUB: [
    ['/club/profile', '俱乐部资料'], ['/club/players', '球员管理'],
    ['/club/coaches', '教练管理'], ['/club/stats', '赛季数据'], ['/club/matches', '本队比赛'], ['/club/statistics', '主场统计'],
  ],
  EVENT_ADMIN: [['/admin/seasons', '赛季与积分榜'], ['/admin/matches', '比赛管理'], ['/admin/stadiums', '场馆与座位'], ['/admin/refunds', '退票审核'], ['/admin/statistics', '统计分析']],
  ADMIN: [['/admin/users', '用户管理'], ['/admin/clubs', '俱乐部注册审核']],
}
const logout = () => {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="management-layout">
    <el-aside width="220px" class="management-aside">
      <h1>{{ appStore.appName }}</h1>
      <p>{{ roleLabels[authStore.user?.roleCode] || authStore.user?.roleCode }}入口</p>
      <el-menu router :default-active="menuActive" class="management-menu">
        <el-menu-item v-for="item in menus[authStore.user?.roleCode] || []" :key="item[0]" :index="item[0]">
          {{ item[1] }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="management-header">
        <span>{{ appStore.currentStage }}</span>
        <div class="management-user">
          <span>{{ authStore.user?.realName }}</span>
          <el-tag type="success" effect="plain">{{ roleLabels[authStore.user?.roleCode] || authStore.user?.roleCode }}</el-tag>
          <el-button link type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.management-layout {
  min-height: 100vh;
}

.management-aside {
  padding: 24px 20px;
  color: #ffffff;
  background: #123524;
}

.management-aside h1 {
  margin: 0 0 12px;
  font-size: 20px;
}

.management-aside p {
  margin: 0;
  color: #bbf7d0;
}

.management-menu {
  margin-top: 24px;
  border-right: 0;
  background: transparent;
}

.management-menu :deep(.el-menu-item) { color: #d1fae5; }
.management-menu :deep(.el-menu-item:hover),
.management-menu :deep(.el-menu-item.is-active) { color: #123524; background: #dcfce7; }

.management-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.management-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
