<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { getResultReminders } from '../../api/match'
import { getAdminSeasons } from '../../api/league'
import { useSystemTimeStore } from '../../stores/systemTime'

const systemTimeStore = useSystemTimeStore(), rows = ref([]), total = ref(0), seasons = ref([]), loading = ref(false), error = ref('')
const query = reactive({ page: 1, size: 10, seasonId: null, reminderType: '' })
const load = async () => { loading.value = true; error.value = ''; try { const data = (await getResultReminders(query)).data; rows.value = data.records; total.value = data.total } catch (e) { error.value = e?.message || '加载赛果提醒失败，请稍后重试。' } finally { loading.value = false } }
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, seasonId: null, reminderType: '' }); load() }
watch(() => systemTimeStore.revision, load)
const initialize = async () => { loading.value = true; error.value = ''; try { seasons.value = (await getAdminSeasons()).data; await load() } catch (e) { error.value = e?.message || '加载赛果提醒失败，请稍后重试。'; loading.value = false } }
onMounted(initialize)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{label:'比赛执行'},{label:'赛果待维护'}]" title="赛果待维护" subtitle="按统一系统日期识别今天比赛和逾期未维护的赛果。"><template #status><el-tag v-if="total" type="warning">{{ total }} 场待处理</el-tag></template></PageHeader>
    <FilterBar :model="query"><el-form-item label="赛季"><el-select v-model="query.seasonId" clearable filterable placeholder="全部赛季"><el-option v-for="seasonItem in seasons" :key="seasonItem.seasonId" :label="seasonItem.seasonName" :value="seasonItem.seasonId" /></el-select></el-form-item><el-form-item label="提醒类型"><el-select v-model="query.reminderType" clearable placeholder="全部提醒"><el-option label="今天比赛" value="TODAY" /><el-option label="已逾期" value="OVERDUE" /></el-select></el-form-item><template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></template></FilterBar>
    <TableWrapper label="待维护赛果列表">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="当前没有待维护赛果" empty-description="新的提醒会按照统一系统日期自动出现。" @retry="initialize">
        <el-table :data="rows"><el-table-column label="提醒" width="130"><template #default="{row}"><StatusTag :value="row.reminderType" :label="row.reminderType === 'TODAY' ? '今天比赛' : `逾期 ${row.daysOverdue} 天`" /></template></el-table-column><el-table-column label="比赛时间" min-width="180"><template #default="{row}">{{ $formatDateTime(row.matchTime) }}</template></el-table-column><el-table-column label="对阵" min-width="230"><template #default="{row}"><strong>{{ row.homeClubName }}</strong><span class="versus">对阵</span><strong>{{ row.awayClubName }}</strong></template></el-table-column><el-table-column prop="seasonName" label="赛季" min-width="150" show-overflow-tooltip /><el-table-column prop="roundName" label="轮次" min-width="110" show-overflow-tooltip /><el-table-column label="状态" width="120"><template #default="{row}"><StatusTag :value="row.matchStatus" /></template></el-table-column><el-table-column label="维护提示" min-width="220" show-overflow-tooltip><template #default="{row}">{{ row.matchStatus === 'PUBLISHED' ? '比赛将在预定时间自动进入进行中' : '比赛已开始，请及时录入赛果' }}</template></el-table-column><el-table-column label="操作" width="144" fixed="right"><template #default="{row}"><RouterLink :to="`/admin/matches/${row.matchId}`" class="table-link">{{ row.matchStatus === 'PUBLISHED' ? '查看比赛' : '录入比分' }}</RouterLink></template></el-table-column></el-table>
      </DataState>
      <template #footer><el-pagination v-if="total > query.size" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" /></template>
    </TableWrapper>
  </div>
</template>

<style scoped>.versus{margin:0 var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-xs)}.table-link{color:var(--color-pitch);font-size:var(--font-size-sm);font-weight:var(--font-weight-semibold)}.table-link:hover{text-decoration:underline;text-underline-offset:3px}</style>
