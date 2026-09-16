<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  tone: {
    type: String,
    default: 'neutral',
    validator: value => ['neutral', 'danger'].includes(value),
  },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <div class="empty-state" :class="[`empty-state--${tone}`, { 'empty-state--compact': compact }]" :role="tone === 'danger' ? 'alert' : undefined">
    <div class="empty-state__mark" aria-hidden="true"><span /></div>
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-state__actions"><slot /></div>
  </div>
</template>

<style scoped>
.empty-state { display: flex; align-items: center; flex-direction: column; justify-content: center; min-height: 260px; padding: var(--space-8) var(--space-6); text-align: center; }
.empty-state__mark { position: relative; display: grid; width: 64px; height: 44px; margin-bottom: var(--space-4); border: 2px solid var(--color-line-strong); border-radius: 50%; place-items: center; }
.empty-state__mark::before,
.empty-state__mark::after { position: absolute; content: ""; background: var(--color-line-strong); }
.empty-state__mark::before { width: 2px; height: 42px; }
.empty-state__mark::after { width: 18px; height: 2px; }
.empty-state__mark span { width: 8px; height: 8px; border: 2px solid var(--color-pitch); border-radius: 50%; background: var(--color-surface); }
.empty-state h2 { margin: 0; color: var(--color-text-primary); font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); }
.empty-state p { max-width: 52ch; margin: var(--space-2) 0 0; color: var(--color-text-muted); font-size: var(--font-size-sm); line-height: var(--line-height-body); }
.empty-state__actions { display: flex; justify-content: center; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-5); }
.empty-state--danger .empty-state__mark span { border-color: var(--color-danger); }
.empty-state--compact { min-height: 180px; padding: var(--space-6); }
.empty-state--compact .empty-state__mark { width: 52px; height: 36px; }
</style>
