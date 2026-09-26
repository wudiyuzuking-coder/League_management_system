<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminRounds, getAdminSeason, getAdminStandings, initStandings, updateSeasonRecord } from '../../api/league'

const route = useRoute(), seasonId = Number(route.params.id)
const season = ref({}), rounds = ref([]), standings = ref([]), loading = ref(false), error = ref('')
const recordVisible = ref(false), recordId = ref(null), recordRef = ref()
const recordSaving = ref(false)
const cancelled = computed(() => season.value.seasonStatus === 'CANCELLED')
const blankRecord = () => ({ wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0 }), record = reactive(blankRecord())
const nonnegative = { type: 'number', min: 0, message: '不能小于0' }, recordRules = { wins: [nonnegative], draws: [nonnegative], losses: [nonnegative], goalsFor: [nonnegative], goalsAgainst: [nonnegative] }
const matchCount = computed(() => season.value.matchCount ?? rounds.value.reduce((sum, item) => sum + Number(item.matchCount || 0), 0))
const metrics = computed(() => [
  { label: '报名球队', value: `${season.value.submittedTeamCount ?? 0} / ${season.value.maxClubs ?? '—'}` },
  season.value.scheduleBatchStatus ? { label: '排赛状态', status: season.value.scheduleBatchStatus } : { label: '排赛状态', value: '未生成' },
  { label: '比赛数量', value: matchCount.value },
  season.value.seasonStatus ? { label: '当前阶段', status: season.value.seasonStatus } : { label: '当前阶段', value: '—' },
])
const load = async () => { loading.value = true; error.value = ''; try { const [s, r, t] = await Promise.all([getAdminSeason(seasonId), getAdminRounds(seasonId), getAdminStandings(seasonId)]); season.value = s.data; rounds.value = r.data; standings.value = t.data } catch (e) { error.value = e?.message || '加载赛季详情失败，请稍后重试。' } finally { loading.value = false } }
const initialize = async () => { const result = await initStandings(seasonId); ElMessage.success(result.data ? `新增${result.data}条记录` : '记录已完整，无需重复初始化'); await load() }
const openRecord = rowData => { recordId.value = rowData.recordId; Object.assign(record, blankRecord(), rowData); recordVisible.value = true }
const saveRecord = async () => { await recordRef.value.validate(); recordSaving.value = true; try { await updateSeasonRecord(recordId.value, record); recordVisible.value = false; ElMessage.success('测试战绩已更新'); await load() } finally { recordSaving.value = false } }
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader back back-fallback="/admin/seasons" :breadcrumb="[{ label: '赛季管理', to: '/admin/seasons' }, { label: season.seasonName || '赛季详情' }]"
      :title="season.seasonName || '赛季详情'" subtitle="查看报名、排赛、比赛阶段与积分数据。"><template #status>
        <StatusTag v-if="season.seasonStatus" :value="season.seasonStatus" />
      </template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!season.seasonId" empty-title="未找到赛季"
      empty-description="该赛季可能已不存在或暂时无法访问。" @retry="load">
      <MetricStrip class="detail-metrics" :items="metrics" label="赛季状态概览" />
      <el-alert v-if="cancelled" class="cancellation-alert" type="error" :closable="false">
        <template #title>赛季已取消</template>
        <div class="cancellation-facts"><span><b>取消原因：</b>{{ season.cancelReason || '未提供取消原因' }}</span><span v-if="season.cancelledAt"><b>取消时间：</b>{{ $formatDateTime(season.cancelledAt) }}</span></div>
      </el-alert>
      <CardShell compact>
        <el-tabs>
          <el-tab-pane label="轮次与赛程">
            <el-alert :title="cancelled ? '该赛季已取消，未安排赛程。' : '报名结束后，系统会自动生成并发布轮次与比赛。'" :type="cancelled ? 'error' : 'info'" :closable="false" />
            <TableWrapper class="tab-table" label="赛季轮次列表" compact><el-table :data="rounds"
                empty-text="暂无轮次"><el-table-column prop="roundNo" label="编号" width="90" align="right" /><el-table-column
                  prop="roundName" label="名称" min-width="160" /><el-table-column prop="startDate" label="开始日期"
                  min-width="130" /><el-table-column prop="endDate" label="结束日期" min-width="130" /><el-table-column
                  label="状态" width="110"><template #default="{ row }">
                    <StatusTag :value="row.roundStatus" />
                  </template></el-table-column></el-table>
            </TableWrapper>
          </el-tab-pane>
          <el-tab-pane label="积分与数据">
            <el-alert :title="cancelled ? '该赛季已取消，不生成或维护积分数据。' : '当前为比赛模块完成前的管理员手工测试战绩维护。'" :type="cancelled ? 'error' : 'warning'" :closable="false" />
            <ActionToolbar v-if="!cancelled" class="standings-toolbar" title="积分榜管理" description="初始化参赛俱乐部并维护测试战绩。"><template
                #actions><el-button type="primary" @click="initialize">初始化参赛俱乐部</el-button></template></ActionToolbar>
            <TableWrapper class="tab-table" label="赛季积分榜" compact><el-table :data="standings"
                empty-text="暂无积分记录"><el-table-column prop="rank" label="排名" width="80" align="right" /><el-table-column
                  prop="clubName" label="俱乐部" min-width="170" show-overflow-tooltip /><el-table-column
                  prop="matchesPlayed" label="场" align="right" /><el-table-column prop="wins" label="胜"
                  align="right" /><el-table-column prop="draws" label="平" align="right" /><el-table-column prop="losses"
                  label="负" align="right" /><el-table-column prop="goalDifference" label="净胜"
                  align="right" /><el-table-column prop="points" label="积分" align="right" /><el-table-column label="操作"
                  v-if="!cancelled" width="144" fixed="right"><template #default="{ row }"><el-button link type="primary"
                      @click="openRecord(row)">维护战绩</el-button></template></el-table-column></el-table>
            </TableWrapper>
          </el-tab-pane>
        </el-tabs>
      </CardShell>
    </DataState>

    <el-dialog v-model="recordVisible" class="app-dialog" title="维护测试战绩" width="500px"><el-form ref="recordRef"
        :model="record" :rules="recordRules" label-width="80px"><el-form-item label="胜" prop="wins"><el-input-number
            v-model="record.wins" :min="0" /></el-form-item><el-form-item label="平" prop="draws"><el-input-number
            v-model="record.draws" :min="0" /></el-form-item><el-form-item label="负" prop="losses"><el-input-number
            v-model="record.losses" :min="0" /></el-form-item><el-form-item label="进球" prop="goalsFor"><el-input-number
            v-model="record.goalsFor" :min="0" /></el-form-item><el-form-item label="失球"
          prop="goalsAgainst"><el-input-number v-model="record.goalsAgainst"
            :min="0" /></el-form-item></el-form><template #footer><el-button :disabled="recordSaving"
          @click="recordVisible = false">取消</el-button><el-button type="primary" :loading="recordSaving"
          @click="saveRecord">保存战绩</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.detail-metrics {
  margin-bottom: var(--space-6)
}

.cancellation-alert {
  margin-bottom: var(--space-4)
}

.cancellation-facts {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-6)
}

.tab-table {
  margin-top: var(--space-4)
}

.standings-toolbar {
  margin-top: var(--space-4)
}

:deep(.el-tabs__header) {
  margin: 0 var(--space-4) var(--space-4)
}

:deep(.el-tabs__content) {
  padding: 0 var(--space-4) var(--space-4)
}

:deep(.el-date-editor) {
  width: 100%
}
</style>
