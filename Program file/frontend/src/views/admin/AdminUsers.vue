<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsers, getRoles, updateAdminUserStatus } from '../../api/user'
import { maskPhone } from '../../utils/privacy'

const rows = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const actionUserId = ref(null)
const detailVisible = ref(false)
const confirmVisible = ref(false)
const selectedUser = ref({})
const pendingUser = ref(null)
const query = reactive({ username: '', roleCode: 'USER', userStatus: '', page: 1, size: 10 })
const roleName = code => roles.value.find(role => role.roleCode === code)?.roleName || code

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = (await getAdminUsers(query)).data
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    error.value = e?.message || '加载用户失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { username: '', userStatus: '', page: 1 }); load() }
const view = row => { selectedUser.value = row; detailVisible.value = true }
const requestToggle = row => {
  if (row.userStatus === 'CANCELLED') return
  pendingUser.value = row
  confirmVisible.value = true
}
const confirmToggle = async () => {
  const row = pendingUser.value
  if (!row || actionUserId.value) return
  const enabling = row.userStatus !== 'ENABLED'
  actionUserId.value = row.userId
  try {
    await updateAdminUserStatus(row.userId, enabling ? 'ENABLED' : 'DISABLED')
    ElMessage.success(`账号已${enabling ? '启用' : '停用'}`)
    confirmVisible.value = false
    await load()
  } catch (e) {
    if (!e?.__notified) ElMessage.error(e?.message || '账号状态更新失败')
  } finally {
    actionUserId.value = null
  }
}

const initialize = async () => {
  try {
    const roleResult = await getRoles()
    roles.value = roleResult.data.filter(role => role.roleStatus === 'ENABLED' && role.roleCode === 'USER')
    await load()
  } catch (e) {
    error.value = e?.message || '加载用户失败，请稍后重试。'
    loading.value = false
  }
}
onMounted(initialize)
</script>

<template>
  <div class="governance-page">
    <PageHeader :breadcrumb="[{label:'系统管理'},{label:'用户管理'}]" title="用户管理" subtitle="查询普通用户并管理账号可用状态。" />
    <FilterBar :model="query">
      <el-form-item label="用户名"><el-input v-model="query.username" name="user-name-filter" autocomplete="off" clearable placeholder="输入用户名…" /></el-form-item>
      <el-form-item label="角色"><el-select v-model="query.roleCode" disabled aria-label="角色"><el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.userStatus" clearable placeholder="全部状态"><el-option label="已启用" value="ENABLED" /><el-option label="已停用" value="DISABLED" /><el-option label="已注销" value="CANCELLED" /></el-select></el-form-item>
      <template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></template>
    </FilterBar>

    <TableWrapper label="普通用户列表">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无普通用户" empty-description="调整筛选条件后重新查询。" @retry="initialize">
        <el-table :data="rows">
          <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
          <el-table-column label="手机号" min-width="130"><template #default="{row}">{{ maskPhone(row.phone) }}</template></el-table-column>
          <el-table-column prop="realName" label="真实姓名" min-width="120" show-overflow-tooltip />
          <el-table-column label="角色" min-width="110"><template #default="{row}">{{ roleName(row.roleCode) }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.userStatus" /></template></el-table-column>
          <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button link type="primary" @click="view(row)">查看</el-button><el-button v-if="row.userStatus !== 'CANCELLED'" link :type="row.userStatus === 'ENABLED' ? 'danger' : 'primary'" @click="requestToggle(row)">{{ row.userStatus === 'ENABLED' ? '停用' : '启用' }}</el-button><span v-else class="muted-text">不可操作</span></template></el-table-column>
        </el-table>
      </DataState>
      <template #footer><el-pagination v-if="total > query.size" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" /></template>
    </TableWrapper>

    <el-dialog v-model="detailVisible" class="app-dialog" title="用户详情" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ selectedUser.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ maskPhone(selectedUser.phone) }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ selectedUser.realName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleName(selectedUser.roleCode) }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :value="selectedUser.userStatus" /></el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>

    <ConfirmDialog v-model="confirmVisible" :title="pendingUser?.userStatus === 'ENABLED' ? '停用账号' : '启用账号'" :message="`将${pendingUser?.userStatus === 'ENABLED' ? '停用' : '启用'}账号“${pendingUser?.username || ''}”。`" :impact="pendingUser?.userStatus === 'ENABLED' ? '停用后该用户将无法登录；历史订单和业务数据会保留。' : '启用后该用户可以重新登录；历史订单和业务数据保持不变。'" :confirm-text="pendingUser?.userStatus === 'ENABLED' ? '确认停用' : '确认启用'" :danger="pendingUser?.userStatus === 'ENABLED'" :loading="actionUserId !== null" @confirm="confirmToggle" />
  </div>
</template>

<style scoped>.muted-text{color:var(--color-text-muted);font-size:var(--font-size-sm)}</style>
