<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminSeasons } from '../../api/league'
import { getSeasonRevenue } from '../../api/statistics'

const seasons = ref([]), seasonId = ref(), data = ref({}), loading = ref(false), error = ref('')
const money = value => `￥${Number(value || 0).toFixed(2)}`
const metrics = computed(() => [
  { label: '有效票务营收', value: money(data.value.effectiveRevenue) },
  { label: '平台分成（10%）', value: money(data.value.platformShare) },
])
const load = async () => { if (!seasonId.value) return; loading.value = true; error.value = ''; try { data.value = (await getSeasonRevenue(seasonId.value)).data } catch (e) { error.value = e?.message || '加载赛季营收失败，请稍后重试。' } finally { loading.value = false } }
const initialize = async () => { loading.value = true; error.value = ''; try { seasons.value = (await getAdminSeasons()).data; if (seasons.value.length) { seasonId.value = seasons.value[0].seasonId; data.value = (await getSeasonRevenue(seasonId.value)).data } } catch (e) { error.value = e?.message || '加载赛季营收失败，请稍后重试。' } finally { loading.value = false } }
onMounted(initialize)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{label:'数据'},{label:'赛季营收'}]" title="赛季营收" subtitle="查看赛季有效票务营收与平台分成。" />
    <ActionToolbar title="统计赛季" description="切换赛季后自动刷新营收数据。"><el-select v-model="seasonId" aria-label="统计赛季" filterable placeholder="选择赛季" @change="load"><el-option v-for="seasonItem in seasons" :key="seasonItem.seasonId" :label="seasonItem.seasonName" :value="seasonItem.seasonId" /></el-select></ActionToolbar>
    <DataState :loading="loading" :error="error" :empty="!seasons.length" empty-title="暂无可统计赛季" empty-description="创建赛季并产生有效订单后可查看营收。" @retry="initialize">
      <MetricStrip class="revenue-metrics" :items="metrics" label="赛季营收摘要" />
      <CardShell title="统计口径" compact><p class="revenue-note">仅统计当前仍为“已支付”的订单明细；已退款明细不计入。</p></CardShell>
    </DataState>
  </div>
</template>

<style scoped>.action-toolbar{margin-bottom:var(--space-6)}.action-toolbar :deep(.el-select){width:260px}.revenue-metrics{margin-bottom:var(--space-6)}.revenue-note{margin:0;color:var(--color-text-secondary);line-height:var(--line-height-body)}</style>
