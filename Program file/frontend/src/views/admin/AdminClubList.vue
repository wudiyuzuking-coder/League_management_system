<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAdminClub, getAdminClubs, updateAdminClub, updateAdminClubStatus } from '../../api/club'
import { approveClubUser, getAdminUsers } from '../../api/user'
import { accountStatusLabel, clubStatusLabel, statusType } from '../../constants/status'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const visible = ref(false)
const editingId = ref(null)
const formRef = ref()
const pendingUsers = ref([])
const approvalVisible = ref(false)
const approvalSaving = ref(false)
const approvalUser = ref({})
const availableClubs = ref([])
const approval = reactive({ mode: 'CREATE_NEW', existingClubId: null })
const query = reactive({ name: '', status: '', page: 1, size: 10 })
const empty = () => ({
  clubName: '', shortName: '', logoUrl: '', homeCity: '', homeAddress: '',
  homeStadiumId: null, description: '',
})
const form = reactive(empty())
const rules = {
  clubName: [{ required: true, message: '请输入俱乐部名称' }],
  homeCity: [{ required: true, message: '请输入主场城市' }],
}

const load = async () => {
  loading.value = true
  try {
    const result = (await getAdminClubs(query)).data
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}
const search = () => {
  query.page = 1
  load()
}
const loadPendingUsers = async () => {
  const data = (await getAdminUsers({ roleCode: 'CLUB', userStatus: 'DISABLED', page: 1, size: 100 })).data
  pendingUsers.value = data.records.filter(user => !user.clubId)
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
    await Promise.all([load(), loadPendingUsers()])
  } finally { approvalSaving.value = false }
}
const open = row => {
  editingId.value = row?.clubId || null
  Object.assign(form, empty(), row || {})
  visible.value = true
}
const save = async () => {
  await formRef.value.validate()
  const payload = { ...form, homeStadiumId: form.homeStadiumId || null }
  if (editingId.value) await updateAdminClub(editingId.value, payload)
  else await createAdminClub(payload)
  visible.value = false
  ElMessage.success('保存成功')
  await load()
}
const toggle = async row => {
  await updateAdminClubStatus(row.clubId, row.clubStatus === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
  ElMessage.success('状态已更新')
  await load()
}
onMounted(() => Promise.all([load(), loadPendingUsers()]))
</script>

<template>
  <el-card>
    <template #header>
      <div class="head"><h2>俱乐部管理</h2><el-button type="primary" @click="open()">新增俱乐部</el-button></div>
    </template>
    <el-form inline>
      <el-form-item label="名称"><el-input v-model="query.name" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:140px">
          <el-option label="已启用" value="ACTIVE" /><el-option label="已停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="clubName" label="名称" min-width="160" />
      <el-table-column label="城市"><template #default="{ row }">{{ row.homeCity || '—' }}</template></el-table-column>
      <el-table-column label="负责人"><template #default="{ row }">{{ row.leaderName || '未绑定' }}</template></el-table-column>
      <el-table-column label="负责人手机号" min-width="130"><template #default="{ row }">{{ row.leaderPhone || '—' }}</template></el-table-column>
      <el-table-column label="账号状态"><template #default="{ row }"><el-tag v-if="row.leaderStatus" :type="statusType(row.leaderStatus)">{{ accountStatusLabel(row.leaderStatus) }}</el-tag><span v-else>—</span></template></el-table-column>
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="statusType(row.clubStatus)">{{ clubStatusLabel(row.clubStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/clubs/${row.clubId}`)">详情</el-button>
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link :type="row.clubStatus === 'ACTIVE' ? 'danger' : 'success'" @click="toggle(row)">
            {{ row.clubStatus === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />

    <el-divider content-position="left">待审核俱乐部注册</el-divider>
    <el-table :data="pendingUsers" empty-text="暂无待审核俱乐部注册">
      <el-table-column prop="clubApplyName" label="申请俱乐部" min-width="160" />
      <el-table-column prop="realName" label="负责人" />
      <el-table-column prop="phone" label="负责人手机号" min-width="130" />
      <el-table-column label="账号状态"><template #default="{ row }"><el-tag :type="statusType(row.userStatus)">{{ accountStatusLabel(row.userStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="success" @click="openApproval(row)">审核</el-button></template></el-table-column>
    </el-table>
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
    <el-alert v-if="approval.mode === 'CREATE_NEW'" :closable="false" type="info" :title="`即将创建：${approvalUser.clubApplyName}；负责人：${approvalUser.realName}`" />
    <el-select v-else v-model="approval.existingClubId" filterable placeholder="请选择尚无负责人的俱乐部" style="width:100%">
      <el-option v-for="club in availableClubs" :key="club.clubId" :label="club.clubName" :value="club.clubId" />
    </el-select>
    <template #footer><el-button @click="approvalVisible = false">取消</el-button><el-button type="primary" :loading="approvalSaving" @click="submitApproval">确认通过</el-button></template>
  </el-dialog>

  <el-dialog v-model="visible" :title="editingId ? '编辑俱乐部' : '新增俱乐部'" width="600px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="名称" prop="clubName"><el-input v-model="form.clubName" /></el-form-item>
      <el-form-item label="简称"><el-input v-model="form.shortName" /></el-form-item>
      <el-form-item label="主场城市" prop="homeCity"><el-input v-model="form.homeCity" /></el-form-item>
      <el-form-item label="主场地址"><el-input v-model="form.homeAddress" /></el-form-item>
      <el-form-item label="场馆ID"><el-input-number v-model="form.homeStadiumId" :min="1" /></el-form-item>
      <el-form-item label="队徽URL"><el-input v-model="form.logoUrl" /></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.description" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; }
.head h2 { margin: 0; }
.el-pagination { margin-top: 16px; justify-content: flex-end; }
.approval-mode { display: flex; margin: 20px 0; }
</style>
