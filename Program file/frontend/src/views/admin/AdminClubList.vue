<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminClubs, updateAdminClubLeaderStatus, updateAdminClubStatus } from '../../api/club'
import { approveClubUser, getClubApplications } from '../../api/user'
import { maskPhone } from '../../utils/privacy'

const rows = ref([]), pending = ref([]), total = ref(0), loading = ref(false), error = ref(''), actionKey = ref('')
const confirmVisible = ref(false), confirmation = ref({})
const query = reactive({ name: '', status: '', page: 1, size: 10 })
const clubStatusOptions = Object.freeze([{ value: 'ACTIVE', label: '正常' }, { value: 'DISABLED', label: '已停用' }])
const load = async () => { loading.value = true; error.value = ''; try { const [clubs, applications] = await Promise.all([getAdminClubs(query), getClubApplications({ page: 1, size: 100 })]); rows.value = clubs.data.records; total.value = clubs.data.total; pending.value = applications.data.records } catch (e) { error.value = e?.message || '加载俱乐部失败，请稍后重试。' } finally { loading.value = false } }
const reset = () => { Object.assign(query, { name: '', status: '', page: 1 }); load() }
const requestConfirmation = options => { confirmation.value = options; confirmVisible.value = true }
const runConfirmation = async () => { if (actionKey.value || !confirmation.value.execute) return; actionKey.value = confirmation.value.key; try { await confirmation.value.execute(); confirmVisible.value = false; await load() } catch (e) { if (!e?.__notified) ElMessage.error(e?.message || '操作失败') } finally { actionKey.value = '' } }
const toggleClub = row => { const enabling = row.clubStatus !== 'ACTIVE'; requestConfirmation({ key: `club-${row.clubId}`, title: enabling ? '启用俱乐部' : '停用俱乐部', message: `将${enabling ? '启用' : '停用'}俱乐部“${row.clubName}”。`, impact: enabling ? '启用后俱乐部可恢复参与后续业务；历史数据保持不变。' : '停用后俱乐部无法参与新的业务；负责人账号及历史数据不会删除。', confirmText: enabling ? '确认启用' : '确认停用', danger: !enabling, execute: async () => { await updateAdminClubStatus(row.clubId, enabling ? 'ACTIVE' : 'DISABLED'); ElMessage.success('俱乐部状态已更新') } }) }
const toggleLeader = row => { if (!row.leaderStatus) return; const enabling = row.leaderStatus !== 'ENABLED'; requestConfirmation({ key: `leader-${row.clubId}`, title: enabling ? '启用负责人账号' : '停用负责人账号', message: `将${enabling ? '启用' : '停用'}“${row.leaderName}”的负责人账号。`, impact: enabling ? '启用后负责人可以重新登录；俱乐部数据保持不变。' : '停用后负责人将无法登录；俱乐部及历史业务数据不会删除。', confirmText: enabling ? '确认启用' : '确认停用', danger: !enabling, execute: async () => { await updateAdminClubLeaderStatus(row.clubId, enabling ? 'ENABLED' : 'DISABLED'); ElMessage.success('负责人账号状态已更新') } }) }
const approve = row => requestConfirmation({ key: `approve-${row.userId}`, title: '通过俱乐部注册', message: `通过俱乐部“${row.clubApplyName}”的注册申请。`, impact: '通过后将创建新俱乐部并绑定当前申请人为负责人；申请资料和审核结果会保留。', confirmText: '确认通过', danger: false, execute: async () => { await approveClubUser(row.userId); ElMessage.success('审核通过并完成负责人绑定') } })
const handleMore = (command, row) => command === 'club' ? toggleClub(row) : toggleLeader(row)
onMounted(load)
</script>

<template>
  <div class="governance-page">
    <PageHeader :breadcrumb="[{label:'系统管理'},{label:'俱乐部管理'}]" title="俱乐部管理" subtitle="管理俱乐部、负责人账号与待审核的注册申请。" />
    <FilterBar :model="query"><el-form-item label="名称"><el-input v-model="query.name" name="club-name-filter" autocomplete="off" clearable placeholder="输入俱乐部名称…" /></el-form-item><el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部状态"><el-option v-for="option in clubStatusOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item><template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="query.page=1;load()">查询</el-button></template></FilterBar>
    <DataState :loading="loading" :error="error" :empty="!rows.length && !pending.length" empty-title="暂无俱乐部" empty-description="当前没有俱乐部或待审核申请。" @retry="load">
      <TableWrapper title="俱乐部" description="查看俱乐部与负责人账号状态。" label="俱乐部列表">
        <el-table :data="rows" empty-text="暂无俱乐部"><el-table-column prop="clubName" label="名称" min-width="160" show-overflow-tooltip /><el-table-column label="负责人" min-width="120"><template #default="{row}">{{ row.leaderName || '未绑定负责人' }}</template></el-table-column><el-table-column label="负责人手机号" min-width="140"><template #default="{row}">{{ maskPhone(row.leaderPhone) }}</template></el-table-column><el-table-column label="负责人状态" width="120"><template #default="{row}"><StatusTag v-if="row.leaderStatus" :value="row.leaderStatus" /><span v-else>—</span></template></el-table-column><el-table-column label="俱乐部状态" width="120"><template #default="{row}"><StatusTag :value="row.clubStatus === 'ACTIVE' ? 'ENABLED' : row.clubStatus" /></template></el-table-column><el-table-column label="操作" width="180" fixed="right"><template #default="{row}"><RouterLink :to="`/admin/clubs/${row.clubId}`" class="table-link">查看详情</RouterLink><el-dropdown trigger="click" @command="command => handleMore(command, row)"><el-button link aria-label="打开俱乐部操作菜单">更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="club">{{ row.clubStatus === 'ACTIVE' ? '停用俱乐部' : '启用俱乐部' }}</el-dropdown-item><el-dropdown-item v-if="row.leaderStatus" command="leader" divided>{{ row.leaderStatus === 'ENABLED' ? '停用负责人' : '启用负责人' }}</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column></el-table>
        <template #footer><el-pagination v-if="total > query.size" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" /></template>
      </TableWrapper>
      <TableWrapper class="pending-table" title="待审核俱乐部注册" description="审核通过后将创建俱乐部并绑定负责人。" label="待审核俱乐部注册"><template #toolbar><StatusTag v-if="pending.length" value="PENDING" /></template><el-table :data="pending" empty-text="暂无待审核俱乐部注册"><el-table-column prop="clubApplyName" label="申请俱乐部" min-width="180" show-overflow-tooltip /><el-table-column prop="realName" label="负责人" min-width="120" /><el-table-column label="手机号" min-width="140"><template #default="{row}">{{ maskPhone(row.phone) }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.userStatus" /></template></el-table-column><el-table-column label="操作" width="144" fixed="right"><template #default="{row}"><el-button link type="primary" @click="approve(row)">审核申请</el-button></template></el-table-column></el-table></TableWrapper>
    </DataState>
    <ConfirmDialog v-model="confirmVisible" :title="confirmation.title || '确认操作'" :message="confirmation.message || ''" :impact="confirmation.impact || ''" :confirm-text="confirmation.confirmText || '确认'" :danger="confirmation.danger" :loading="Boolean(actionKey)" @confirm="runConfirmation" />
  </div>
</template>

<style scoped>.pending-table{margin-top:var(--space-6)}.table-link{margin-right:var(--space-2);color:var(--color-pitch);font-size:var(--font-size-sm);font-weight:var(--font-weight-semibold)}.table-link:hover{text-decoration:underline;text-underline-offset:3px}</style>
