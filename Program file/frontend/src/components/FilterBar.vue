<script setup>
defineProps({ model: { type: Object, default: undefined } })
</script>

<template>
  <el-form :model="model" class="filter-bar" inline @submit.prevent>
    <div class="filter-bar__fields"><slot /></div>
    <div class="filter-bar__actions"><slot name="actions" /></div>
  </el-form>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
  padding: var(--space-md);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}
.filter-bar__fields { display: flex; align-items: flex-end; flex-wrap: wrap; gap: var(--space-sm) var(--space-md); min-width: 0; }
.filter-bar__actions { display: flex; align-items: center; flex: none; gap: var(--space-sm); }
.filter-bar :deep(.el-form-item) { margin: 0; }
.filter-bar :deep(.el-input), .filter-bar :deep(.el-select) { width: 180px; }
</style>
