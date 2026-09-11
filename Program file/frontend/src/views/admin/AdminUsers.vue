<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsers, getRoles, updateAdminUserStatus } from '../../api/user'
import { accountStatusLabel, statusType } from '../../constants/status'

const rows = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const selectedUser = ref({})
const query = reactive({ username: '', roleCode: 'USER', userStatus: '', page: 1, size: 10 })
const roleName = code => roles.value.find(role => role.roleCode === code)?.roleName || code

const load = async () => {
  loading.value = true
  try {
    const data = (await getAdminUsers(query)).data
    rows.value = data.records
    total.value = data.total
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
  const nextStatus = row.userStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await updateAdminUserStatus(row.userId, nextStatus)
  ElMessage.success(`账号已${nextStatus === 'ENABLED' ? '启用' : '停用'}`)
  await load()
}

onMounted(async () => {
  const roleResult = await getRoles()
  roles.value = roleResult.data.filter(role => role.roleStatus === 'ENABLED' && role.roleCode === 'USER')
  await load()
})
</script>

<template>
  <el-card>
    <template #header><div class="head"><h2>用户管理</h2></div></template>
    <el-form class="filters" inline>
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
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="rows" v-loading="loading" empty-text="暂无普通用户">
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="phone" label="手机号" min-width="125" />
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column label="角色"><template #default="{ row }">{{ roleName(row.roleCode) }}</template></el-table-column>
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="statusType(row.userStatus)">{{ accountStatusLabel(row.userStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="view(row)">查看</el-button>
          <el-button link :type="row.userStatus === 'ENABLED' ? 'danger' : 'success'" @click="toggle(row)">{{ row.userStatus === 'ENABLED' ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
  </el-card>

  <el-dialog v-model="detailVisible" title="用户详情" width="520px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户名">{{ selectedUser.username || '—' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ selectedUser.phone || '—' }}</el-descriptions-item>
      <el-descriptions-item label="真实姓名">{{ selectedUser.realName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ roleName(selectedUser.roleCode) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ accountStatusLabel(selectedUser.userStatus) }}</el-descriptions-item>
    </el-descriptions>
    <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
  </el-dialog>
</template>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; }
.head h2 { margin: 0; }
.filters { display: flex; align-items: center; flex-wrap: wrap; }
.filters :deep(.el-form-item) { margin-bottom: 18px; }
.el-pagination { margin-top: 16px; justify-content: flex-end; }
</style>
