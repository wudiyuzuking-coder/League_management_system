<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmMatchResult, getPendingResultReviews } from '../../api/match'

const rows = ref([]), loading = ref(false), error = ref(''), visible = ref(false), saving = ref(false), selected = ref(null)
const score = reactive({ homeScore: 0, awayScore: 0 })
const load = async () => { loading.value = true; error.value = ''; try { rows.value = (await getPendingResultReviews()).data } catch (e) { error.value = e?.message || '加载待确认赛果失败，请稍后重试。' } finally { loading.value = false } }
const open = row => { selected.value = row; const first = row.submissions?.[0]; score.homeScore = first?.homeScore || 0; score.awayScore = first?.awayScore || 0; visible.value = true }
const confirm = async () => { if (!selected.value || saving.value) return; saving.value = true; try { await confirmMatchResult(selected.value.matchId, score); ElMessage.success('最终比分已发布'); visible.value = false; await load() } catch (e) { if (!e?.__notified) ElMessage.error(e?.message || '最终比分发布失败') } finally { saving.value = false } }
onMounted(load)
</script>

<template>
  <div class="governance-page">
    <PageHeader :breadcrumb="[{label:'系统管理'},{label:'赛果确认'}]" title="赛果确认" subtitle="处理多人提交冲突或仅有一次提交的比赛结果。"><template #status><StatusTag v-if="rows.length" value="PENDING_ADMIN_REVIEW" /></template></PageHeader>
    <TableWrapper label="待确认赛果列表">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无待确认赛果" empty-description="需要系统管理员确认的比赛结果会显示在这里。" @retry="load">
        <el-table :data="rows"><el-table-column prop="matchId" label="比赛编号" width="110"><template #default="{row}"><span class="tabular-nums">#{{ row.matchId }}</span></template></el-table-column><el-table-column label="审核原因" width="150"><template #default="{row}"><StatusTag :value="row.reviewReason === 'CONFLICT' ? 'CONFLICT' : 'PENDING_ADMIN_REVIEW'" /></template></el-table-column><el-table-column label="提交记录" min-width="300"><template #default="{row}"><span v-for="submission in row.submissions" :key="submission.submissionId" class="submission"><b>{{ submission.submitterName }}</b><span class="score-nums">{{ submission.homeScore }} : {{ submission.awayScore }}</span></span></template></el-table-column><el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button type="primary" link @click="open(row)">确认最终比分</el-button></template></el-table-column></el-table>
      </DataState>
    </TableWrapper>
    <ConfirmDialog v-model="visible" title="确认最终比分" :message="`将比赛 #${selected?.matchId || ''} 的最终比分发布为 ${score.homeScore} : ${score.awayScore}。`" impact="发布后将作为正式赛果进入后续统计；原始提交记录会保留。" confirm-text="发布最终比分" danger :loading="saving" @confirm="confirm">
      <el-form class="score-form" label-position="top"><el-form-item label="主队比分"><el-input-number v-model="score.homeScore" :min="0" aria-label="主队比分" /></el-form-item><span aria-hidden="true">:</span><el-form-item label="客队比分"><el-input-number v-model="score.awayScore" :min="0" aria-label="客队比分" /></el-form-item></el-form>
    </ConfirmDialog>
  </div>
</template>

<style scoped>.submission{display:flex;align-items:center;justify-content:space-between;gap:var(--space-4);max-width:360px;padding:var(--space-1) 0}.submission b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.submission .score-nums{font-size:var(--font-size-lg);font-weight:var(--font-weight-bold)}.score-form{display:grid;grid-template-columns:1fr auto 1fr;align-items:end;gap:var(--space-3);margin-top:var(--space-5)}.score-form>span{padding-bottom:var(--space-3);font-family:var(--font-score);font-size:var(--font-size-2xl);font-weight:var(--font-weight-bold)}</style>
