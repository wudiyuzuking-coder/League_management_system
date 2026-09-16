<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmSchedule, getSchedule, getSchedules } from '../../api/league'

const rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), visible = ref(false), detail = ref({}), confirming = ref(false), confirmVisible = ref(false)
const query = reactive({ seasonId: null, batchStatus: '', page: 1, size: 20 })
const triggerLabel = value => ({ MANUAL: '提前截止报名', AUTO: '报名到期自动生成' }[value] || value || '—')
const load = async () => { loading.value = true; error.value = ''; try { const result = (await getSchedules(query)).data; rows.value = result.records; total.value = result.total } catch (e) { error.value = e?.message || '加载赛程失败，请稍后重试。' } finally { loading.value = false } }
const reset = () => { Object.assign(query, { seasonId: null, batchStatus: '', page: 1 }); load() }
const show = async row => { try { detail.value = (await getSchedule(row.seasonId)).data; visible.value = true } catch (e) { if (!e?.__notified) ElMessage.error(e?.message || '加载赛程详情失败') } }
const confirm = async () => { if (confirming.value) return; confirming.value = true; try { detail.value = (await confirmSchedule(detail.value.seasonId)).data; ElMessage.success('赛程已确认'); confirmVisible.value = false; await load() } catch (e) { if (!e?.__notified) ElMessage.error(e?.message || '赛程确认失败') } finally { confirming.value = false } }
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{label:'赛事运营'},{label:'赛程管理'}]" title="赛程管理" subtitle="查询自动生成的赛程批次并完成正式确认。"><template #actions><el-button @click="load">刷新</el-button></template></PageHeader>
    <FilterBar :model="query"><el-form-item label="赛季编号"><el-input-number v-model="query.seasonId" :min="1" aria-label="赛季编号" /></el-form-item><el-form-item label="排赛状态"><el-select v-model="query.batchStatus" clearable placeholder="全部状态"><el-option label="待确认" value="GENERATED" /><el-option label="已确认" value="CONFIRMED" /></el-select></el-form-item><template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="query.page=1;load()">查询</el-button></template></FilterBar>
    <TableWrapper label="赛程批次列表">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无赛程批次" empty-description="满足排赛条件后，自动生成的赛程会显示在这里。" @retry="load">
        <el-table :data="rows"><el-table-column prop="seasonName" label="赛季" min-width="180" show-overflow-tooltip /><el-table-column prop="clubCount" label="球队数" width="90" align="right" /><el-table-column prop="roundCount" label="轮次" width="80" align="right" /><el-table-column prop="matchCount" label="比赛数" width="90" align="right" /><el-table-column label="生成方式" min-width="150"><template #default="{row}">{{ triggerLabel(row.triggerType) }}</template></el-table-column><el-table-column label="生成时间" min-width="180"><template #default="{row}">{{ $formatDateTime(row.generatedAt) }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.batchStatus" /></template></el-table-column><el-table-column label="操作" width="144" fixed="right"><template #default="{row}"><el-button link type="primary" @click="show(row)">查看赛程</el-button></template></el-table-column></el-table>
      </DataState>
      <template #footer><el-pagination v-if="total > query.size" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" /></template>
    </TableWrapper>

    <el-dialog v-model="visible" class="app-dialog schedule-dialog" title="赛程详情" width="min(960px, 92vw)">
      <MetricStrip :items="[{label:'参赛球队',value:detail.clubCount},{label:'比赛轮次',value:detail.roundCount},{label:'比赛数量',value:detail.matchCount},{label:'排赛状态',status:detail.batchStatus}]" label="赛程摘要" />
      <section v-for="round in detail.rounds || []" :key="round.roundNo" class="round-section"><h3>第 {{ round.roundNo }} 轮</h3><TableWrapper :label="`第 ${round.roundNo} 轮比赛`" compact><el-table :data="round.matches"><el-table-column label="比赛时间" min-width="180"><template #default="{row}">{{ $formatDateTime(row.matchDateTime) }}</template></el-table-column><el-table-column prop="homeClubName" label="主队" min-width="130" show-overflow-tooltip /><el-table-column prop="awayClubName" label="客队" min-width="130" show-overflow-tooltip /><el-table-column prop="stadiumName" label="主场" min-width="140" show-overflow-tooltip /><el-table-column label="比赛状态" width="110"><template #default="{row}"><StatusTag :value="row.matchStatus" /></template></el-table-column></el-table></TableWrapper></section>
      <template #footer><el-button @click="visible=false">关闭</el-button><el-button v-if="detail.batchStatus === 'GENERATED'" type="primary" @click="confirmVisible=true">确认赛程</el-button></template>
    </el-dialog>
    <ConfirmDialog v-model="confirmVisible" title="确认正式赛程" :message="`确认“${detail.seasonName || ''}”的当前赛程。`" impact="确认后赛程将进入正式业务流程；球队、轮次、比赛时间和场馆数据会保留。" confirm-text="确认赛程" :loading="confirming" @confirm="confirm" />
  </div>
</template>

<style scoped>.round-section{margin-top:var(--space-6)}.round-section h3{margin:0 0 var(--space-3);font-size:var(--font-size-lg);font-weight:var(--font-weight-semibold)}.schedule-dialog :deep(.el-dialog__body){max-height:70vh;overflow-y:auto;overscroll-behavior:contain}</style>
