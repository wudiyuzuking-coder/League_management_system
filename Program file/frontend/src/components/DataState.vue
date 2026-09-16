<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  loading: { type: Boolean, default: false },
  error: { type: [String, Boolean, Error], default: '' },
  empty: { type: Boolean, default: false },
  emptyTitle: { type: String, default: '暂无数据' },
  emptyDescription: { type: String, default: '' },
  errorTitle: { type: String, default: '加载失败' },
  errorDescription: { type: String, default: '请检查网络后重新尝试。' },
  skeletonRows: { type: Number, default: 4 },
})

defineEmits(['retry'])
</script>

<template>
  <section class="data-state" :aria-busy="loading ? 'true' : 'false'">
    <div v-if="loading" class="data-state__loading" aria-live="polite">
      <span class="sr-only">正在加载…</span>
      <el-skeleton :rows="skeletonRows" animated />
    </div>
    <div v-else-if="error" role="alert" aria-live="assertive">
      <EmptyState tone="danger" :title="errorTitle" :description="typeof error === 'string' ? error : errorDescription">
        <el-button type="primary" @click="$emit('retry')">重新加载</el-button>
      </EmptyState>
    </div>
    <EmptyState v-else-if="empty" :title="emptyTitle" :description="emptyDescription">
      <slot name="empty-action" />
    </EmptyState>
    <slot v-else />
  </section>
</template>

<style scoped>
.data-state { min-width: 0; }
.data-state__loading { min-height: 180px; padding: var(--space-6) 0; }
</style>
