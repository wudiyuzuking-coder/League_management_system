<script setup>
defineProps({
  model: { type: Object, default: undefined },
  label: { type: String, default: '筛选条件' },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <el-form :model="model" class="filter-bar" :class="{ 'filter-bar--compact': compact }" inline role="search" :aria-label="label" @submit.prevent>
    <div class="filter-bar__fields"><slot /></div>
    <div v-if="$slots.actions" class="filter-bar__actions"><slot name="actions" /></div>
  </el-form>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-surface-subtle);
}
.filter-bar__fields { display: flex; align-items: flex-end; flex: 1; flex-wrap: wrap; gap: var(--space-3) var(--space-4); min-width: 0; }
.filter-bar__actions { display: flex; align-items: center; flex: none; flex-wrap: wrap; gap: var(--space-2); }
.filter-bar :deep(.el-form-item) { margin: 0; }
.filter-bar :deep(.el-input), .filter-bar :deep(.el-select) { width: 180px; }
.filter-bar--compact { padding: var(--space-3); }
@media (max-width: 720px) { .filter-bar { align-items: stretch; flex-direction: column; } .filter-bar__actions { justify-content: flex-end; } .filter-bar :deep(.el-input), .filter-bar :deep(.el-select) { width: min(100%, 240px); } }
</style>
