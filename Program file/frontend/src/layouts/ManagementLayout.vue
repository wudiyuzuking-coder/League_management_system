<script setup>
import { useAppStore } from '../stores/app'
import { useAuthStore } from '../stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { computed,onMounted,ref,watch } from 'vue'
import SystemTimeControl from '../components/SystemTimeControl.vue'
import {useSystemTimeStore} from '../stores/systemTime'
import {getResultReminders} from '../api/match'
import { UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { MENU_PATHS, ROLE_LABELS, ROLE_MENUS, ROLE_MENU_GROUPS } from '../config/navigation'
import { cancelAccount } from '../api/auth'
import { cancelCurrentAccount } from '../utils/accountCancellation'
import { confirmAction } from '../utils/confirmAction'

const appStore = useAppStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const systemTimeStore=useSystemTimeStore(),resultReminderCount=ref(0),accountCancelling=ref(false)
const loadReminderCount=async()=>{if(authStore.user?.roleCode!=='EVENT_ADMIN')return;try{resultReminderCount.value=(await getResultReminders({page:1,size:1})).data.total}catch{resultReminderCount.value=0}}
const menuActive = computed(() => {
  const path = route.path
  return MENU_PATHS.find(root => path === root || path.startsWith(`${root}/`)) || path
})
const logout = () => {
  authStore.logout()
  router.replace('/login')
}
const cancelSelf = async () => {
  try {
    await cancelCurrentAccount({
      confirm: () => confirmAction({title:'注销账号',message:'确定注销当前账号吗？',impact:'注销后将无法继续登录，历史订单和业务数据不会删除。',confirmButtonText:'确认注销',danger:true}),
      request: async () => { accountCancelling.value=true; try { return await cancelAccount() } finally { accountCancelling.value=false } },
      logout: () => authStore.logout(),
      redirect: path => router.replace(path),
      notify: message => ElMessage.success(message),
    })
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (!error?.__notified) ElMessage.error(error?.message || '注销失败')
  }
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
  if (command === 'cancel') cancelSelf()
}
const menuLabel=item=>item[0]==='/admin/matches/result-reminders'?`${item[1]}（${resultReminderCount.value}）`:item[1]
const menuGroups=computed(()=>{
  const items=ROLE_MENUS[authStore.user?.roleCode]||[]
  return (ROLE_MENU_GROUPS[authStore.user?.roleCode]||[]).map(group=>({
    ...group,
    items:group.paths.map(path=>items.find(([itemPath])=>itemPath===path)).filter(Boolean),
  }))
})
const currentPageTitle=computed(()=>{
  const items=ROLE_MENUS[authStore.user?.roleCode]||[]
  return items.find(([path])=>route.path===path||route.path.startsWith(`${path}/`))?.[1]||'工作台'
})
onMounted(loadReminderCount)
watch(()=>systemTimeStore.revision,loadReminderCount)
</script>

<template>
  <a class="skip-link" href="#main-content">跳至主要内容</a>
  <el-container class="management-layout">
    <el-aside width="220px" class="management-aside">
      <div class="app-brand">{{ appStore.appName }}</div>
      <p>{{ ROLE_LABELS[authStore.user?.roleCode] || authStore.user?.roleCode }}入口</p>
      <nav aria-label="一级导航">
        <section v-for="group in menuGroups" :key="group.label" class="menu-group">
          <h2>{{group.label}}</h2>
          <el-menu router :default-active="menuActive" class="management-menu">
            <el-menu-item v-for="item in group.items" :key="item[0]" :index="item[0]">
              <span class="menu-dot" aria-hidden="true" />{{ menuLabel(item) }}
            </el-menu-item>
          </el-menu>
        </section>
      </nav>
    </el-aside>
    <el-container class="management-content">
      <el-header class="management-header">
        <div class="header-context"><span>{{ROLE_LABELS[authStore.user?.roleCode] || '工作台'}}</span><strong>{{currentPageTitle}}</strong></div>
        <div class="management-user">
          <SystemTimeControl />
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
                <el-dropdown-item command="cancel" :disabled="accountCancelling">注销账号</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main id="main-content" class="management-main" tabindex="-1">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.management-layout {
  min-height: 100vh;
}

.skip-link { position: fixed; z-index: 3000; top: 10px; left: 10px; padding: 8px 12px; border-radius: var(--radius-sm); color: #fff; background: var(--primary); transform: translateY(-160%); }
.skip-link:focus { transform: translateY(0); }

.management-aside {
  padding: 24px 20px;
  color: #ffffff;
  background: #123b29;
}

.management-aside .app-brand {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: var(--font-weight-bold);
}

.management-aside p {
  margin: 0;
  color: #bbf7d0;
}

.management-menu {
  border-right: 0;
  background: transparent;
}

.menu-group { margin-top: var(--space-lg); }
.menu-group h2 { margin: 0 0 8px 12px; color: #91b6a0; font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.menu-dot { width: 5px; height: 5px; margin-right: 10px; border-radius: 50%; background: currentColor; opacity: .55; }

.management-menu :deep(.el-menu-item) { height: 44px; margin: 3px 0; color: #d1fae5; border-radius: var(--radius-sm); }
.management-menu :deep(.el-menu-item:hover),
.management-menu :deep(.el-menu-item.is-active) { color: #123524; background: #dcfce7; }
.management-menu :deep(.el-menu-item:focus-visible) { outline: 3px solid rgb(187 247 208 / 45%); outline-offset: 2px; }

.management-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.header-context { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.header-context span { color: var(--text-muted); font-size: 12px; }
.header-context strong { overflow: hidden; color: var(--text-primary); font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }

.management-content {
  min-width: 0;
}

.management-main {
  overflow-x: auto;
  padding: var(--space-lg);
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
