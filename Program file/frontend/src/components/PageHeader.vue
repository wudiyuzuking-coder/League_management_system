<script setup>
import BackButton from './BackButton.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  breadcrumb: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false },
  back: { type: Boolean, default: false },
  backFallback: { type: [String, Object], default: '' },
})
</script>

<template>
  <header class="page-header" :class="{ 'page-header--compact': compact }">
    <div class="page-header__content">
      <BackButton v-if="back && backFallback" :fallback="backFallback" />
      <nav v-if="breadcrumb.length" class="page-header__breadcrumb" aria-label="面包屑导航">
        <template v-for="(item, index) in breadcrumb" :key="`${item.label}-${index}`">
          <RouterLink v-if="item.to" :to="item.to">{{ item.label }}</RouterLink>
          <span v-else aria-current="page">{{ item.label }}</span>
          <span v-if="index < breadcrumb.length - 1" class="page-header__separator" aria-hidden="true">/</span>
        </template>
      </nav>
      <div class="page-header__title-row">
        <h1>{{ title }}</h1>
        <slot name="status" />
      </div>
      <p v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</p>
      <div v-if="$slots.meta" class="page-header__meta"><slot name="meta" /></div>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-6);
}

.page-header__content { min-width: 0; }
.page-header__breadcrumb { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-2); color: var(--color-text-muted); font-size: var(--font-size-sm); }
.page-header__breadcrumb a { color: var(--text-secondary); }
.page-header__breadcrumb a:hover { color: var(--primary); }
.page-header__breadcrumb a:focus-visible { border-radius: var(--radius-sm); outline: none; box-shadow: var(--focus-ring); }
.page-header__separator { color: var(--color-line-strong); }
.page-header__title-row { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-2); }
.page-header h1 { margin: 0; color: var(--color-text-primary); font-size: var(--font-size-3xl); font-weight: var(--font-weight-bold); line-height: var(--line-height-tight); letter-spacing: -.02em; text-wrap: balance; }
.page-header__subtitle { max-width: 72ch; margin: var(--space-2) 0 0; color: var(--color-text-secondary); font-size: var(--font-size-md); line-height: var(--line-height-body); }
.page-header__meta { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-3); margin-top: var(--space-3); color: var(--color-text-muted); font-size: var(--font-size-sm); }
.page-header__actions { display: flex; align-items: center; flex: none; flex-wrap: wrap; justify-content: flex-end; gap: var(--space-2); padding-top: var(--space-1); }
.page-header--compact { margin-bottom: var(--space-4); }
.page-header--compact h1 { font-size: var(--font-size-2xl); }
@media (max-width: 720px) { .page-header { flex-direction: column; } .page-header__actions { width: 100%; justify-content: flex-start; } }
</style>
