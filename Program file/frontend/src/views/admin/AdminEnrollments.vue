<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminEnrollment, getAdminEnrollments } from '../../api/club'
import { formatDateTime } from '../../utils/format'

const rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), visible = ref(false), detail = ref({}), detailLoading = ref(false)
const query = reactive({ seasonId: null, clubId: null, enrollmentStatus: 'SUBMITTED', page: 1, size: 20 })
const positionLabel = { GOALKEEPER: '门将', DEFENDER: '后卫', MIDFIELDER: '中场', FORWARD: '前锋' }
const lineupLabel = { STARTER: '首发', SUBSTITUTE: '替补' }
const coachTitleLabel = { HEAD_COACH: '主教练', ASSISTANT_COACH: '助理教练', FITNESS_COACH: '体能教练', GOALKEEPER_COACH: '守门员教练' }
const load = async () => { loading.value = true; error.value = ''; try { const result = (await getAdminEnrollments(query)).data; rows.value = result.records; total.value = result.total } catch (e) { error.value = e?.message || '加载报名记录失败，请稍后重试。' } finally { loading.value = false } }
const reset = () => { Object.assign(query, { seasonId: null, clubId: null, page: 1 }); load() }
const show = async id => { detailLoading.value = true; try { detail.value = (await getAdminEnrollment(id)).data; visible.value = true } catch (e) { if (!e?.__notified) ElMessage.error(e?.message || '加载报名详情失败') } finally { detailLoading.value = false } }
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{label:'赛事运营'},{label:'赛季报名'}]" title="赛季报名" subtitle="查看俱乐部报名、主场和提交阵容快照。"><template #actions><el-button @click="load">刷新</el-button></template></PageHeader>
    <FilterBar :model="query"><el-form-item label="赛季编号"><el-input-number v-model="query.seasonId" :min="1" aria-label="赛季编号" /></el-form-item><el-form-item label="俱乐部编号"><el-input-number v-model="query.clubId" :min="1" aria-label="俱乐部编号" /></el-form-item><template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="query.page=1;load()">查询</el-button></template></FilterBar>
    <TableWrapper label="赛季报名列表">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无报名记录" empty-description="调整筛选条件后重新查询。" @retry="load">
        <el-table :data="rows"><el-table-column prop="seasonName" label="赛季" min-width="170" show-overflow-tooltip /><el-table-column prop="clubName" label="俱乐部" min-width="150" show-overflow-tooltip /><el-table-column prop="stadiumName" label="主场" min-width="150" show-overflow-tooltip /><el-table-column label="报名时间" min-width="180"><template #default="{row}">{{ formatDateTime(row.submittedAt) }}</template></el-table-column><el-table-column prop="playerCount" label="球员" width="80" align="right" /><el-table-column prop="coachCount" label="教练" width="80" align="right" /><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.enrollmentStatus" /></template></el-table-column><el-table-column label="操作" width="144" fixed="right"><template #default="{row}"><el-button link type="primary" :loading="detailLoading" @click="show(row.enrollmentId)">查看详情</el-button></template></el-table-column></el-table>
      </DataState>
      <template #footer><el-pagination v-if="total > query.size" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" /></template>
    </TableWrapper>

    <el-dialog v-model="visible" class="app-dialog enrollment-dialog" title="报名详情" width="min(900px, 92vw)"><el-descriptions :column="2" border><el-descriptions-item label="赛季">{{ detail.seasonName }}</el-descriptions-item><el-descriptions-item label="俱乐部">{{ detail.clubName }}</el-descriptions-item><el-descriptions-item label="主场">{{ detail.stadiumName }}</el-descriptions-item><el-descriptions-item label="状态"><StatusTag :value="detail.enrollmentStatus" /></el-descriptions-item></el-descriptions><TableWrapper class="detail-table" title="球员阵容" label="报名球员阵容" compact><el-table :data="detail.players || []"><el-table-column prop="playerName" label="姓名" /><el-table-column prop="shirtNo" label="号码" align="right" /><el-table-column label="位置"><template #default="{row}">{{ positionLabel[row.position] || row.position }}</template></el-table-column><el-table-column label="阵容"><template #default="{row}">{{ lineupLabel[row.lineupRole] || row.lineupRole }}</template></el-table-column><el-table-column prop="age" label="年龄" align="right" /></el-table></TableWrapper><TableWrapper class="detail-table" title="教练团队" label="报名教练团队" compact><el-table :data="detail.coaches || []"><el-table-column prop="coachName" label="姓名" /><el-table-column label="职务"><template #default="{row}">{{ coachTitleLabel[row.title] || row.title }}</template></el-table-column></el-table></TableWrapper><template #footer><el-button @click="visible=false">关闭</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.detail-table{margin-top:var(--space-5)}.enrollment-dialog :deep(.el-dialog__body){max-height:70vh;overflow-y:auto;overscroll-behavior:contain}</style>
