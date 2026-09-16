<script setup>
defineProps({
  as: { type: String, default: 'section' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  variant: {
    type: String,
    default: 'surface',
    validator: value => ['surface', 'fixture', 'action'].includes(value),
  },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <component :is="as" class="card-shell" :class="[`card-shell--${variant}`, { 'card-shell--compact': compact }]">
    <header v-if="title || subtitle || $slots.header || $slots.actions" class="card-shell__header">
      <slot name="header">
        <div class="card-shell__heading">
          <h2 v-if="title">{{ title }}</h2>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
      </slot>
      <div v-if="$slots.actions" class="card-shell__actions"><slot name="actions" /></div>
    </header>
    <div class="card-shell__body"><slot /></div>
    <footer v-if="$slots.footer" class="card-shell__footer"><slot name="footer" /></footer>
  </component>
</template>

<style scoped>
.card-shell {
  overflow: hidden;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-none);
}
.card-shell--fixture { border-radius: var(--radius-lg); border-top: 3px solid var(--role-accent); }
.card-shell--action { border-color: var(--color-line-strong); box-shadow: var(--shadow-raised); }
.card-shell__header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); min-height: 56px; padding: var(--space-4) var(--space-6); border-bottom: 1px solid var(--color-line); }
.card-shell__heading { min-width: 0; }
.card-shell__heading h2 { margin: 0; font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); line-height: var(--line-height-heading); }
.card-shell__heading p { max-width: 72ch; margin: var(--space-1) 0 0; color: var(--color-text-muted); font-size: var(--font-size-sm); line-height: var(--line-height-body); }
.card-shell__actions { display: flex; align-items: center; flex: none; flex-wrap: wrap; gap: var(--space-2); }
.card-shell__body { padding: var(--space-6); }
.card-shell__footer { padding: var(--space-4) var(--space-6); border-top: 1px solid var(--color-line); background: var(--color-surface-subtle); }
.card-shell--compact .card-shell__header { padding: var(--space-3) var(--space-4); }
.card-shell--compact .card-shell__body { padding: var(--space-4); }
.card-shell--compact .card-shell__footer { padding: var(--space-3) var(--space-4); }
</style>
