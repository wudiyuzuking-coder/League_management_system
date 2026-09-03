<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminClubs } from '../../api/club'
import {
  approveClubUser, createAdminUser, getAdminUsers, getRoles,
  updateAdminUser, updateAdminUserStatus,
} from '../../api/user'

const rows = ref([])
const roles = ref([])
const clubs = ref([])
const availableClubs = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref(null)
const formRef = ref()
const query = reactive({ username: '', roleCode: '', userStatus: '', page: 1, size: 10 })
const blank = () => ({
  username: '', phone: '', password: '', realName: '', employeeNo: '',
  roleCode: 'USER', clubId: null, userStatus: 'ENABLED',
})
const form = reactive(blank())
const roleName = code => roles.value.find(role => role.roleCode === code)?.roleName || code
const needsClub = computed(() => form.roleCode === 'CLUB')
const isManagement = computed(() => ['EVENT_ADMIN', 'ADMIN'].includes(form.roleCode))
const validateEmployeeNo = (_rule, value, done) => {
  if (!isManagement.value) return done()
  const pattern = form.roleCode === 'EVENT_ADMIN' ? /^EA\d{4}$/ : /^SA\d{4}$/
  if (!value) return done(new Error('管理账号必须填写工号'))
  return pattern.test(value) ? done() : done(new Error(form.roleCode === 'EVENT_ADMIN' ? '工号格式应为EA0001' : '工号格式应为SA0001'))
}
const rules = {
  username: [{ required: true, message: '请输入昵称' }, { min: 2, max: 50, message: '长度为2到50个字符' }],
  phone: [{ required: true, message: '请输入手机号' }, { pattern: /^1\d{10}$/, message: '请输入11位手机号' }],
  password: [{ validator: (_rule, value, done) => editingId.value || value?.length >= 6 ? done() : done(new Error('密码至少6位')) }],
  realName: [{ required: true, message: '请输入姓名' }],
  employeeNo: [{ validator: validateEmployeeNo }],
  roleCode: [{ required: true, message: '请选择角色' }],
  clubId: [{ validator: (_rule, value, done) => !needsClub.value || value ? done() : done(new Error('该角色必须绑定俱乐部')) }],
}

const approvalVisible = ref(false)
const approvalSaving = ref(false)
const approvalUser = ref({})
const approval = reactive({ mode: 'CREATE_NEW', existingClubId: null })
const canApprove = row => row.roleCode === 'CLUB' && row.userStatus === 'DISABLED' && !row.clubId

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
    const payload = {
      username: form.username, realName: form.realName, phone: form.phone,
      roleCode: form.roleCode, employeeNo: isManagement.value ? form.employeeNo : null,
      clubId: needsClub.value ? form.clubId : null, userStatus: form.userStatus,
    }
    if (editingId.value) await updateAdminUser(editingId.value, payload)
    else await createAdminUser({ ...payload, password: form.password })
    visible.value = false
    ElMessage.success('用户资料已保存')
    await load()
  } finally {
    saving.value = false
  }
}
const toggle = async row => {
  const enabling = row.userStatus !== 'ENABLED'
  if (enabling && row.roleCode === 'CLUB' && !row.clubId) {
    ElMessage.warning('请使用CLUB注册审核完成创建或关联')
    return
  }
  if (enabling && ['EVENT_ADMIN', 'ADMIN'].includes(row.roleCode) && !row.employeeNo) {
    ElMessage.warning('请先为管理账号设置合法工号')
    return
  }
  await updateAdminUserStatus(row.userId, enabling ? 'ENABLED' : 'DISABLED')
  ElMessage.success('用户状态已更新')
  await load()
}
const openApproval = async row => {
  approvalUser.value = row
  approval.mode = 'CREATE_NEW'
  approval.existingClubId = null
  const result = (await getAdminClubs({ page: 1, size: 100, withoutLeader: true })).data
  availableClubs.value = result.records
  approvalVisible.value = true
}
const submitApproval = async () => {
  if (approval.mode === 'BIND_EXISTING' && !approval.existingClubId) {
    ElMessage.warning('请选择一个尚无负责人的俱乐部')
    return
  }
  approvalSaving.value = true
  try {
    await approveClubUser(approvalUser.value.userId, {
      mode: approval.mode,
      existingClubId: approval.mode === 'BIND_EXISTING' ? approval.existingClubId : null,
    })
    approvalVisible.value = false
    ElMessage.success('CLUB注册审核已通过')
    await load()
  } finally {
    approvalSaving.value = false
  }
}

onMounted(async () => {
  const [roleResult, clubResult] = await Promise.all([
    getRoles(), getAdminClubs({ page: 1, size: 100, status: 'ACTIVE' }),
  ])
  roles.value = roleResult.data.filter(role => role.roleStatus === 'ENABLED')
  clubs.value = clubResult.data.records
  await load()
})
</script>

<template>
  <el-card>
    <template #header><div class="head"><h2>用户管理</h2><el-button type="primary" @click="open()">新增用户</el-button></div></template>
    <el-form inline>
      <el-form-item label="昵称"><el-input v-model="query.username" clearable /></el-form-item>
      <el-form-item label="角色"><el-select v-model="query.roleCode" clearable style="width:130px"><el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.userStatus" clearable style="width:120px"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select></el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
    </el-form>
    <el-table :data="rows" v-loading="loading" empty-text="暂无用户">
      <el-table-column prop="username" label="昵称" />
      <el-table-column prop="phone" label="手机号" min-width="125" />
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column label="申请俱乐部" min-width="150"><template #default="{ row }">{{ row.clubApplyName || '—' }}</template></el-table-column>
      <el-table-column label="工号" width="110"><template #default="{ row }">{{ row.employeeNo ?? '—' }}</template></el-table-column>
      <el-table-column label="角色"><template #default="{ row }">{{ roleName(row.roleCode) }}</template></el-table-column>
      <el-table-column label="绑定俱乐部"><template #default="{ row }">{{ row.clubId ?? '—' }}</template></el-table-column>
      <el-table-column label="状态"><template #default="{ row }"><StatusTag :value="row.userStatus" /></template></el-table-column>
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button v-if="canApprove(row)" link type="success" @click="openApproval(row)">审核</el-button>
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link :type="row.userStatus === 'ENABLED' ? 'danger' : 'success'" @click="toggle(row)">{{ row.userStatus === 'ENABLED' ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
  </el-card>

  <el-dialog v-model="approvalVisible" title="CLUB注册审核" width="560px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="负责人">{{ approvalUser.realName }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ approvalUser.phone }}</el-descriptions-item>
      <el-descriptions-item label="申请俱乐部">{{ approvalUser.clubApplyName }}</el-descriptions-item>
    </el-descriptions>
    <el-radio-group v-model="approval.mode" class="approval-mode">
      <el-radio value="CREATE_NEW">创建新俱乐部</el-radio>
      <el-radio value="BIND_EXISTING">关联已有俱乐部</el-radio>
    </el-radio-group>
    <el-alert v-if="approval.mode === 'CREATE_NEW'" :closable="false" type="info"
              :title="`即将创建：${approvalUser.clubApplyName}；负责人：${approvalUser.realName}`" />
    <el-select v-else v-model="approval.existingClubId" filterable placeholder="请选择尚无负责人的俱乐部" style="width:100%">
      <el-option v-for="club in availableClubs" :key="club.clubId" :label="club.clubName" :value="club.clubId" />
    </el-select>
    <template #footer>
      <el-button @click="approvalVisible = false">取消</el-button>
      <el-button type="primary" :loading="approvalSaving" @click="submitApproval">确认通过</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="visible" :title="editingId ? '编辑用户' : '新增用户'" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="昵称" prop="username"><el-input v-model="form.username" /></el-form-item>
      <el-form-item v-if="!editingId" label="初始密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="真实姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item v-if="isManagement" label="工号" prop="employeeNo"><el-input v-model="form.employeeNo" :placeholder="form.roleCode === 'EVENT_ADMIN' ? 'EA0001' : 'SA0001'" /></el-form-item>
      <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="角色" prop="roleCode"><el-select v-model="form.roleCode"><el-option v-for="role in roles" :key="role.roleCode" :label="`${role.roleName}（${role.roleCode}）`" :value="role.roleCode" /></el-select></el-form-item>
      <el-form-item v-if="needsClub" label="绑定俱乐部" prop="clubId"><el-select v-model="form.clubId" filterable><el-option v-for="club in clubs" :key="club.clubId" :label="club.clubName" :value="club.clubId" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="form.userStatus"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; }
.head h2 { margin: 0; }
.el-pagination { margin-top: 16px; justify-content: flex-end; }
.approval-mode { display: flex; margin: 20px 0; }
</style>
