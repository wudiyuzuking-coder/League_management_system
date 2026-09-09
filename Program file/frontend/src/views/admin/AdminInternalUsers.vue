<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createAdminUser, getAdminUsers, getRoles, updateAdminUser, updateAdminUserStatus } from '../../api/user'
import { accountStatusLabel, statusType } from '../../constants/status'

const rows = ref([]), roles = ref([]), total = ref(0), loading = ref(false)
const saving = ref(false), visible = ref(false), editingId = ref(null), formRef = ref()
const query = reactive({ username: '', roleCode: 'EVENT_ADMIN', userStatus: '', page: 1, size: 10 })
const blank = () => ({ username: '', phone: '', password: '', realName: '', employeeNo: '', roleCode: 'EVENT_ADMIN', clubId: null, userStatus: 'ENABLED' })
const form = reactive(blank())
const roleName = code => roles.value.find(role => role.roleCode === code)?.roleName || code
const employeePlaceholder = computed(() => form.roleCode === 'EVENT_ADMIN' ? 'EA0001' : 'SA0001')
const rules = {
  username: [{ required: true, message: '请输入昵称' }, { min: 2, max: 50, message: '长度为2到50个字符' }],
  phone: [{ required: true, message: '请输入手机号' }, { pattern: /^1\d{10}$/, message: '请输入11位手机号' }],
  password: [{ validator: (_rule, value, done) => editingId.value || value?.length >= 6 ? done() : done(new Error('密码至少6位')) }],
  realName: [{ required: true, message: '请输入姓名' }],
  employeeNo: [{ validator: (_rule, value, done) => {
    const valid = (form.roleCode === 'EVENT_ADMIN' ? /^EA\d{4}$/ : /^SA\d{4}$/).test(value || '')
    return valid ? done() : done(new Error(`工号格式应为${employeePlaceholder.value}`))
  } }],
  roleCode: [{ required: true, message: '请选择角色' }],
}

const load = async () => {
  loading.value = true
  try {
    const data = (await getAdminUsers(query)).data
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}
const search = () => { query.page = 1; load() }
const open = row => {
  editingId.value = row?.userId || null
  Object.assign(form, blank(), row || {})
  form.password = ''
  visible.value = true
}
const save = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { username: form.username, realName: form.realName, phone: form.phone, roleCode: form.roleCode, employeeNo: form.employeeNo, clubId: null, userStatus: form.userStatus }
    if (editingId.value) await updateAdminUser(editingId.value, payload)
    else await createAdminUser({ ...payload, password: form.password })
    visible.value = false
    query.roleCode = form.roleCode
    ElMessage.success('内部人员资料已保存')
    await load()
  } finally { saving.value = false }
}
const toggle = async row => {
  const enabling = row.userStatus !== 'ENABLED'
  if (enabling && !row.employeeNo) return ElMessage.warning('请先为管理账号设置合法工号')
  await updateAdminUserStatus(row.userId, enabling ? 'ENABLED' : 'DISABLED')
  ElMessage.success(`账号已${enabling ? '启用' : '停用'}`)
  await load()
}
onMounted(async () => {
  const result = await getRoles()
  roles.value = result.data.filter(role => role.roleStatus === 'ENABLED' && ['EVENT_ADMIN', 'ADMIN'].includes(role.roleCode))
  await load()
})
</script>

<template>
  <el-card>
    <template #header><div class="head"><h2>内部人员管理</h2><el-button type="primary" @click="open()">新增内部人员</el-button></div></template>
    <el-alert class="phase-tip" type="info" :closable="false" title="当前沿用现有管理员账号创建与维护流程，首次启用流程将在后续阶段实施。" />
    <el-form class="filters" inline>
      <el-form-item label="昵称/用户名"><el-input v-model="query.username" clearable /></el-form-item>
      <el-form-item label="角色"><el-select v-model="query.roleCode" style="width:150px"><el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.userStatus" clearable style="width:130px"><el-option label="已启用" value="ENABLED" /><el-option label="已停用" value="DISABLED" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="rows" v-loading="loading" empty-text="暂无内部人员">
      <el-table-column prop="username" label="昵称/用户名" min-width="140" />
      <el-table-column prop="phone" label="手机号" min-width="125" />
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column label="工号" width="110"><template #default="{ row }">{{ row.employeeNo || '—' }}</template></el-table-column>
      <el-table-column label="角色"><template #default="{ row }">{{ roleName(row.roleCode) }}</template></el-table-column>
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="statusType(row.userStatus)">{{ accountStatusLabel(row.userStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="160"><template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link :type="row.userStatus === 'ENABLED' ? 'danger' : 'success'" @click="toggle(row)">{{ row.userStatus === 'ENABLED' ? '停用' : '启用' }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
  </el-card>

  <el-dialog v-model="visible" :title="editingId ? '编辑内部人员' : '新增内部人员'" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="昵称" prop="username"><el-input v-model="form.username" /></el-form-item>
      <el-form-item v-if="!editingId" label="初始密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="真实姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item label="工号" prop="employeeNo"><el-input v-model="form.employeeNo" :placeholder="employeePlaceholder" /></el-form-item>
      <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="角色" prop="roleCode"><el-select v-model="form.roleCode"><el-option v-for="role in roles" :key="role.roleCode" :label="`${role.roleName}（${role.roleCode}）`" :value="role.roleCode" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="form.userStatus"><el-option label="已启用" value="ENABLED" /><el-option label="已停用" value="DISABLED" /></el-select></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.head { display:flex; align-items:center; justify-content:space-between; }
.head h2 { margin:0; }
.phase-tip { margin-bottom:18px; }
.filters { display:flex; align-items:center; flex-wrap:wrap; }
.filters :deep(.el-form-item) { margin-bottom:18px; }
.el-pagination { margin-top:16px; justify-content:flex-end; }
</style>
