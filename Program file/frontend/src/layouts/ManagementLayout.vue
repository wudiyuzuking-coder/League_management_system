<script setup>
import { useAppStore } from '../stores/app'
import { useAuthStore } from '../stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { computed,onMounted,ref,watch } from 'vue'
import SystemTimeControl from '../components/SystemTimeControl.vue'
import {useSystemTimeStore} from '../stores/systemTime'
import {getResultReminders} from '../api/match'
import { UserFilled } from '@element-plus/icons-vue'
import { MENU_PATHS, ROLE_LABELS, ROLE_MENUS } from '../config/navigation'

const appStore = useAppStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const systemTimeStore=useSystemTimeStore(),resultReminderCount=ref(0)
const loadReminderCount=async()=>{if(authStore.user?.roleCode!=='EVENT_ADMIN')return;try{resultReminderCount.value=(await getResultReminders({page:1,size:1})).data.total}catch{resultReminderCount.value=0}}
const menuActive = computed(() => {
  const path = route.path
  return MENU_PATHS.find(root => path === root || path.startsWith(`${root}/`)) || path
})
const logout = () => {
  authStore.logout()
  router.replace('/login')
}
const accountPath = computed(() => {
  if (authStore.user?.roleCode === 'USER') return '/user/profile'
  if (authStore.user?.roleCode === 'CLUB') return '/club/account'
  return '/admin/account'
})
const handleAccountCommand = command => {
  if (command === 'profile') router.push(accountPath.value)
  if (command === 'switch') router.push('/switch-account')
  if (command === 'logout') logout()
}
const menuLabel=item=>item[0]==='/admin/matches/result-reminders'?`${item[1]}（${resultReminderCount.value}）`:item[1]
onMounted(loadReminderCount)
watch(()=>systemTimeStore.revision,loadReminderCount)
</script>

<template>
  <el-container class="management-layout">
    <el-aside width="220px" class="management-aside">
      <h1>{{ appStore.appName }}</h1>
      <p>{{ ROLE_LABELS[authStore.user?.roleCode] || authStore.user?.roleCode }}入口</p>
      <el-menu router :default-active="menuActive" class="management-menu" aria-label="一级导航">
        <el-menu-item v-for="item in ROLE_MENUS[authStore.user?.roleCode] || []" :key="item[0]" :index="item[0]">
          {{ menuLabel(item) }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="management-content">
      <el-header class="management-header">
        <SystemTimeControl />
        <div class="management-user">
          <el-dropdown trigger="click" @command="handleAccountCommand">
            <button class="account-trigger" type="button" aria-label="打开账号菜单">
              <el-avatar :size="32" :src="authStore.user?.avatarUrl || undefined" :icon="UserFilled" />
              <span>{{ authStore.user?.username }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="account-summary">
                  <el-avatar :size="48" :src="authStore.user?.avatarUrl || undefined" :icon="UserFilled" />
                  <div><b>{{authStore.user?.username}}</b><small>{{authStore.user?.phone||'—'}}</small><small>{{ROLE_LABELS[authStore.user?.roleCode]||authStore.user?.roleCode}}</small></div>
                </div>
                <el-dropdown-item command="profile">账号资料</el-dropdown-item>
                <el-dropdown-item command="switch">切换账户</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tag type="success" effect="plain">{{ ROLE_LABELS[authStore.user?.roleCode] || authStore.user?.roleCode }}</el-tag>
        </div>
      </el-header>
      <el-main class="management-main">
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
  justify-content: flex-end;
  gap: 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.management-content {
  min-width: 0;
}

.management-main {
  overflow-x: auto;
}

.management-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px;
  color: #1f2937;
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  background: transparent;
}

.account-trigger:hover,
.account-trigger:focus-visible {
  background: #f3f4f6;
  outline: none;
}
.account-summary{display:flex;gap:12px;align-items:center;padding:12px 16px;min-width:220px;border-bottom:1px solid #ebeef5}.account-summary div{display:flex;flex-direction:column;gap:3px}.account-summary small{color:#6b7280}

@media (max-width: 760px) {
  .management-aside {
    width: 180px !important;
    padding: 20px 12px;
  }

  .management-header {
    height: auto;
    min-height: 60px;
    flex-wrap: wrap;
    padding: 10px 14px;
  }
}
</style>
