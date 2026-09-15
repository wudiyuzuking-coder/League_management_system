<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  breadcrumb: { type: Array, default: () => [] },
})
</script>

<template>
  <header class="page-header">
    <div class="page-header__content">
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
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.page-header__content { min-width: 0; }
.page-header__breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; color: var(--text-muted); font-size: 13px; }
.page-header__breadcrumb a { color: var(--text-secondary); }
.page-header__breadcrumb a:hover { color: var(--primary); }
.page-header__breadcrumb a:focus-visible { border-radius: var(--radius-sm); outline: 3px solid var(--primary-soft); outline-offset: 2px; }
.page-header__separator { color: #cbd5e1; }
.page-header__title-row { display: flex; align-items: center; gap: var(--space-sm); }
.page-header h1 { margin: 0; color: var(--text-primary); font-size: 28px; line-height: 1.2; text-wrap: balance; }
.page-header__subtitle { max-width: 720px; margin: 8px 0 0; color: var(--text-secondary); line-height: 1.6; }
.page-header__actions { display: flex; align-items: center; flex: none; gap: var(--space-sm); }
</style>
