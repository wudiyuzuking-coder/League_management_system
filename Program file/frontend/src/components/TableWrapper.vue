<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  label: { type: String, default: '数据表格' },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <section class="table-wrapper" :class="{ 'table-wrapper--compact': compact }" :aria-label="label">
    <header v-if="title || description || $slots.toolbar" class="table-wrapper__header">
      <div v-if="title || description" class="table-wrapper__heading">
        <h2 v-if="title">{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.toolbar" class="table-wrapper__toolbar"><slot name="toolbar" /></div>
    </header>
    <div class="table-wrapper__viewport"><slot /></div>
    <footer v-if="$slots.footer" class="table-wrapper__footer"><slot name="footer" /></footer>
  </section>
</template>

<style scoped>
.table-wrapper { overflow: hidden; border: 1px solid var(--color-line); border-radius: var(--radius-md); background: var(--color-surface); }
.table-wrapper__header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); min-height: 56px; padding: var(--space-3) var(--space-4); border-bottom: 1px solid var(--color-line); }
.table-wrapper__heading { min-width: 0; }
.table-wrapper__heading h2 { margin: 0; font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); }
.table-wrapper__heading p { max-width: 72ch; margin: var(--space-1) 0 0; color: var(--color-text-muted); font-size: var(--font-size-xs); }
.table-wrapper__toolbar { display: flex; align-items: center; flex: none; flex-wrap: wrap; gap: var(--space-2); }
.table-wrapper__viewport { min-width: 0; overflow-x: auto; }
.table-wrapper__viewport :deep(.el-table) { min-width: 720px; }
.table-wrapper--compact .table-wrapper__viewport :deep(.el-table td.el-table__cell) { height: 40px; }
.table-wrapper__footer { display: flex; align-items: center; justify-content: flex-end; min-height: 56px; padding: var(--space-2) var(--space-4); border-top: 1px solid var(--color-line); background: var(--color-surface-subtle); }
.table-wrapper__footer:empty { display: none; }
.table-wrapper__footer :deep(.el-pagination) { margin-top: 0; }
</style>
