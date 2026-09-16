<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  sticky: { type: Boolean, default: false },
})
</script>

<template>
  <div class="action-toolbar" :class="{ 'action-toolbar--sticky': sticky }">
    <div class="action-toolbar__main">
      <slot name="start">
        <div v-if="title || description" class="action-toolbar__heading">
          <strong v-if="title">{{ title }}</strong>
          <span v-if="description">{{ description }}</span>
        </div>
      </slot>
      <slot />
    </div>
    <div v-if="$slots.actions" class="action-toolbar__actions"><slot name="actions" /></div>
  </div>
</template>

<style scoped>
.action-toolbar { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); min-height: 56px; padding: var(--space-2) var(--space-4); border: 1px solid var(--color-line); border-radius: var(--radius-md); background: var(--color-surface); }
.action-toolbar--sticky { position: sticky; z-index: 10; bottom: var(--space-4); box-shadow: var(--shadow-overlay); }
.action-toolbar__main { display: flex; align-items: center; flex: 1; flex-wrap: wrap; gap: var(--space-3); min-width: 0; }
.action-toolbar__heading { display: flex; flex-direction: column; min-width: 0; }
.action-toolbar__heading strong { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); }
.action-toolbar__heading span { overflow: hidden; color: var(--color-text-muted); font-size: var(--font-size-xs); text-overflow: ellipsis; white-space: nowrap; }
.action-toolbar__actions { display: flex; align-items: center; flex: none; flex-wrap: wrap; gap: var(--space-2); }
@media (max-width: 640px) { .action-toolbar { align-items: stretch; flex-direction: column; } .action-toolbar__actions { justify-content: flex-end; } }
</style>
