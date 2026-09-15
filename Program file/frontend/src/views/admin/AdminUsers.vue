<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsers, getRoles, updateAdminUserStatus } from '../../api/user'
import { accountStatusLabel } from '../../constants/status'
import { confirmAction } from '../../utils/confirmAction'
import { maskPhone } from '../../utils/privacy'

const rows = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const actionUserId = ref(null)
const detailVisible = ref(false)
const selectedUser = ref({})
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
const search = () => {
  query.page = 1
  load()
}
const view = row => {
  selectedUser.value = row
  detailVisible.value = true
}
const toggle = async row => {
  if (row.userStatus === 'CANCELLED') return
  const nextStatus = row.userStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const enabling=nextStatus==='ENABLED'
  await confirmAction({title:enabling?'启用账号':'停用账号',message:`确定${enabling?'启用':'停用'}账号“${row.username}”吗？`,impact:enabling?'启用后，该用户可以重新登录系统。':'停用后，该用户将无法登录；历史订单和业务数据不会删除。',confirmButtonText:enabling?'确认启用':'确认停用',danger:!enabling})
  actionUserId.value=row.userId
  try{await updateAdminUserStatus(row.userId, nextStatus);ElMessage.success(`账号已${enabling ? '启用' : '停用'}`);await load()}finally{actionUserId.value=null}
}

onMounted(async () => {
  const roleResult = await getRoles()
  roles.value = roleResult.data.filter(role => role.roleStatus === 'ENABLED' && role.roleCode === 'USER')
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="用户管理" subtitle="查询普通用户并管理账号可用状态。" />
    <section class="app-surface">
    <FilterBar :model="query">
      <el-form-item label="用户名"><el-input v-model="query.username" clearable /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleCode" disabled style="width: 130px">
          <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.userStatus" clearable style="width: 130px">
          <el-option label="已启用" value="ENABLED" />
          <el-option label="已停用" value="DISABLED" />
          <el-option label="已注销" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <template #actions><el-button type="primary" @click="search">查询</el-button></template>
    </FilterBar>
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无普通用户" @retry="load">
    <el-table :data="rows" empty-text="暂无普通用户">
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column label="手机号" min-width="125"><template #default="{row}">{{maskPhone(row.phone)}}</template></el-table-column>
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column label="角色"><template #default="{ row }">{{ roleName(row.roleCode) }}</template></el-table-column>
      <el-table-column label="状态"><template #default="{ row }"><StatusTag :value="row.userStatus"/></template></el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="view(row)">查看</el-button>
          <el-button v-if="row.userStatus !== 'CANCELLED'" link :loading="actionUserId===row.userId" :type="row.userStatus === 'ENABLED' ? 'danger' : 'success'" @click="toggle(row)">{{ row.userStatus === 'ENABLED' ? '停用' : '启用' }}</el-button>
          <span v-else>不可操作</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </DataState>
    </section>

  <el-dialog v-model="detailVisible" class="app-dialog" title="用户详情" width="520px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户名">{{ selectedUser.username || '—' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ maskPhone(selectedUser.phone) }}</el-descriptions-item>
      <el-descriptions-item label="真实姓名">{{ selectedUser.realName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ roleName(selectedUser.roleCode) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ accountStatusLabel(selectedUser.userStatus) }}</el-descriptions-item>
    </el-descriptions>
    <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
  </el-dialog>
  </div>
</template>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; }
.head h2 { margin: 0; }
.el-pagination { margin-top: 16px; justify-content: flex-end; }
</style>
