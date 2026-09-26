<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { closeSeasonRegistrationOnly, createSeason, finishSeason, getAdminSeasons, openSeasonRegistration, updateSeason } from '../../api/league'
import { applySeasonNameConflict, clearSeasonNameConflict } from '../../utils/seasonCreateConflict'

const rows = ref([]), loading = ref(false), error = ref(''), visible = ref(false), editingId = ref(null), formRef = ref(), saving = ref(false), actionSeasonId = ref(null)
const confirmVisible = ref(false), confirmation = ref({})
const createConflict = reactive({ nameError: '' })
const empty = () => ({ seasonName: '', startDate: '', maxClubs: 16 }), form = reactive(empty())
const rules = { seasonName: [{ required: true, message: '请输入赛季名称', trigger: 'blur' }, { validator: (_, value, callback) => value?.trim() ? callback() : callback(new Error('赛季名称不能为空')), trigger: 'blur' }], startDate: [{ required: true, message: '请选择开始日期' }], maxClubs: [{ required: true, type: 'number', min: 2, max: 20, message: '参赛队伍上限为2至20' }] }
const cap = value => { const n = Number(value); form.maxClubs = Number.isFinite(n) ? Math.max(2, Math.min(20, n)) : 2 }
const metrics = computed(() => [
  { label: '赛季总数', value: rows.value.length },
  { label: '已报名球队', value: rows.value.reduce((sum, row) => sum + Number(row.submittedTeamCount || 0), 0) },
  { label: '已确认排赛', value: rows.value.filter(row => row.scheduleBatchStatus === 'CONFIRMED').length },
  { label: '进行中赛季', value: rows.value.filter(row => row.seasonStatus === 'IN_PROGRESS').length },
])
const load = async () => { loading.value = true; error.value = ''; try { rows.value = (await getAdminSeasons()).data } catch (e) { error.value = e?.message || '加载赛季失败，请稍后重试。' } finally { loading.value = false } }
const open = row => { editingId.value = row?.seasonId || null; clearSeasonNameConflict(createConflict); Object.assign(form, empty(), row ? { seasonName: row.seasonName, startDate: row.startDate, maxClubs: row.maxClubs } : {}); visible.value = true }
const save = async () => { cap(form.maxClubs); await formRef.value.validate(); saving.value = true; try { editingId.value ? await updateSeason(editingId.value, form) : await createSeason(form, { skipErrorNotification: true }); visible.value = false; ElMessage.success('赛季及自动时间已生成'); await load() } catch (e) { if (!editingId.value && applySeasonNameConflict(createConflict, e)) return; if (!e?.__notified) ElMessage.error(e?.message || '保存赛季失败') } finally { saving.value = false } }
watch(() => form.seasonName, () => clearSeasonNameConflict(createConflict))
const requestConfirmation = options => { confirmation.value = options; confirmVisible.value = true }
const runConfirmation = async () => { if (actionSeasonId.value || !confirmation.value.execute) return; actionSeasonId.value = confirmation.value.seasonId; try { await confirmation.value.execute(); confirmVisible.value = false; await load() } catch (e) { if (!e?.__notified) ElMessage.error(e?.message || '赛季操作失败') } finally { actionSeasonId.value = null } }
const openRegistration = row => requestConfirmation({ seasonId: row.seasonId, title: '开启赛季报名', message: `开启赛季“${row.seasonName}”的报名。`, impact: '开启后符合条件的俱乐部可在报名窗口内提交报名，赛季核心资料将不再允许修改。', confirmText: '确认开启', danger: false, execute: async () => { await openSeasonRegistration(row.seasonId); ElMessage.success('赛季报名已开启') } })
const closeRegistration = row => requestConfirmation({ seasonId: row.seasonId, title: '结束赛季报名', message: `结束赛季“${row.seasonName}”的报名。`, impact: '结束后俱乐部无法继续报名；系统将自动生成并发布赛程、积分榜和票务库存。', confirmText: '确认结束报名', danger: false, execute: async () => { await closeSeasonRegistrationOnly(row.seasonId); ElMessage.success('报名已结束，系统正在自动生成并发布赛程') } })
const finishSeasonAction = row => requestConfirmation({ seasonId: row.seasonId, title: '结束赛季', message: `结束赛季“${row.seasonName}”。`, impact: '仅当所有比赛完成且不存在待处理赛果时可结束；历史数据会保留。', confirmText: '确认结束', danger: true, execute: async () => { await finishSeason(row.seasonId); ElMessage.success('赛季已结束') } })
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{ label: '赛事运营' }, { label: '赛季管理' }]" title="赛季管理" subtitle="管理报名、排赛与赛季生命周期。"><template
        #actions><el-button type="primary" @click="open()">创建赛季</el-button></template>
    </PageHeader>
    <MetricStrip class="season-metrics" :items="metrics" label="赛季运营概览" />
    <TableWrapper label="赛季列表">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无赛季"
        empty-description="创建首个赛季后，报名窗口与预计结束时间会由系统生成。" @retry="load">
        <el-table :data="rows">
          <el-table-column prop="seasonName" label="赛季" min-width="180" show-overflow-tooltip />
          <el-table-column label="赛季周期" min-width="190"><template #default="{ row }">
              <div class="date-range"><b>{{ $formatDate(row.startDate) }}</b><span>至 {{ $formatDate(row.endDate)
                  }}</span></div>
            </template></el-table-column>
          <el-table-column label="报名状态" min-width="190"><template #default="{ row }">
              <div class="registration-cell"><strong class="tabular-nums">{{ row.submittedTeamCount ?? 0 }} / {{
                row.maxClubs }}</strong><span>截止 {{ $formatDateTime(row.registrationDeadline) }}</span></div>
            </template></el-table-column>
          <el-table-column label="排赛状态" width="120"><template #default="{ row }">
              <StatusTag v-if="row.scheduleBatchStatus" :value="row.scheduleBatchStatus" /><span v-else
                class="muted-text">未生成</span>
            </template></el-table-column>
          <el-table-column label="比赛阶段" width="120"><template #default="{ row }">
              <StatusTag :value="row.seasonStatus" />
            </template></el-table-column>
          <el-table-column label="操作" min-width="260" fixed="right"><template #default="{ row }">
              <ActionToolbar class="season-actions"><RouterLink :to="`/admin/seasons/${row.seasonId}`" class="table-link">查看详情</RouterLink><el-button
                  v-if="row.seasonStatus === 'DRAFT'" link type="primary" @click="open(row)">调整赛季</el-button><el-button
                  v-if="row.seasonStatus === 'DRAFT'" link type="primary" @click="openRegistration(row)">开启报名</el-button><el-button
                  v-if="row.seasonStatus === 'REGISTRATION'" link type="primary" @click="closeRegistration(row)">结束报名</el-button><el-button
                  v-if="row.seasonStatus === 'IN_PROGRESS'" link type="danger" @click="finishSeasonAction(row)">结束赛季</el-button></ActionToolbar>
            </template></el-table-column>
        </el-table>
      </DataState>
    </TableWrapper>

    <el-dialog v-model="visible" class="app-dialog" :title="editingId ? '调整赛季' : '创建赛季'" width="500px">
      <el-alert title="预计结束时间和报名窗口由系统生成；售票时间按每场比赛日期自动计算。" type="info" :closable="false" />
      <el-form ref="formRef" class="season-form" :model="form" :rules="rules" label-position="top"><el-form-item
          label="赛季名称" prop="seasonName" :error="createConflict.nameError"><el-input v-model="form.seasonName" name="season-name" autocomplete="off"
            maxlength="80" show-word-limit placeholder="例如：2027 城市足球联赛…" /></el-form-item><el-form-item label="赛季开始日期"
          prop="startDate"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD"
            aria-label="赛季开始日期" /></el-form-item><el-form-item label="参赛队伍上限" prop="maxClubs"><el-input
            v-model.number="form.maxClubs" name="season-max-clubs" type="number" inputmode="numeric" min="2" max="20"
            autocomplete="off" @change="cap(form.maxClubs)" /></el-form-item></el-form>
      <template #footer><el-button :disabled="saving" @click="visible = false">取消</el-button><el-button type="primary"
          :loading="saving" @click="save">保存赛季</el-button></template>
    </el-dialog>
    <ConfirmDialog v-model="confirmVisible" :title="confirmation.title || '确认赛季操作'"
      :message="confirmation.message || ''" :impact="confirmation.impact || ''"
      :confirm-text="confirmation.confirmText || '确认'" :danger="confirmation.danger" :loading="actionSeasonId !== null"
      @confirm="runConfirmation" />
  </div>
</template>

<style scoped>
.season-metrics {
  margin-bottom: var(--space-6)
}

.date-range,
.registration-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0
}

.date-range span,
.registration-cell span,
.muted-text {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs)
}

.registration-cell strong {
  font-size: var(--font-size-lg)
}

.table-link {
  margin-right: var(--space-2);
  color: var(--color-pitch);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold)
}

.table-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px
}

.season-actions {
  gap: var(--space-2)
}

.season-actions :deep(.action-toolbar__main) {
  flex-wrap: nowrap
}

.season-actions {
  min-height: auto;
  padding: 0;
  border: 0;
  background: transparent
}

.season-form {
  margin-top: var(--space-5)
}

.season-form :deep(.el-date-editor) {
  width: 100%
}
</style>
