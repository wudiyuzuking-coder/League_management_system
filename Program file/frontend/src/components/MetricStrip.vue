<script setup>
defineProps({
  items: { type: Array, default: () => [] },
  label: { type: String, default: '关键指标' },
})
</script>

<template>
  <section class="metric-strip" :aria-label="label">
    <slot>
      <div v-for="(item, index) in items" :key="item.key ?? item.label ?? index" class="metric-strip__item" :class="item.tone ? `metric-strip__item--${item.tone}` : ''">
        <span>{{ item.label }}</span>
        <StatusTag v-if="item.status" class="metric-strip__status" :value="item.status" />
        <strong v-else class="score-nums">{{ item.value ?? '—' }}</strong>
        <small v-if="item.meta">{{ item.meta }}</small>
      </div>
    </slot>
  </section>
</template>

<style scoped>
.metric-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); overflow: hidden; border: 1px solid var(--color-line); border-radius: var(--radius-md); background: var(--color-surface); }
.metric-strip__item { position: relative; display: flex; flex-direction: column; justify-content: center; min-height: 104px; padding: var(--space-4) var(--space-5); }
.metric-strip__item + .metric-strip__item { border-left: 1px solid var(--color-line); }
.metric-strip__item::before { position: absolute; top: var(--space-4); bottom: var(--space-4); left: 0; width: 3px; border-radius: var(--radius-full); background: transparent; content: ""; }
.metric-strip__item--success::before { background: var(--color-success); }
.metric-strip__item--warning::before { background: var(--color-warning); }
.metric-strip__item--danger::before { background: var(--color-danger); }
.metric-strip__item > span { color: var(--color-text-muted); font-size: var(--font-size-xs); line-height: 18px; }
.metric-strip__item > strong { margin-top: var(--space-1); color: var(--color-text-primary); font-size: var(--font-size-3xl); font-weight: var(--font-weight-bold); line-height: 32px; }
.metric-strip__status { align-self: flex-start; margin-top: var(--space-2); }
.metric-strip__item > small { margin-top: var(--space-1); color: var(--color-text-secondary); font-size: var(--font-size-xs); }
@media (max-width: 720px) { .metric-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); } .metric-strip__item + .metric-strip__item { border-left: 0; } .metric-strip__item:nth-child(even) { border-left: 1px solid var(--color-line); } .metric-strip__item:nth-child(n + 3) { border-top: 1px solid var(--color-line); } }
</style>
