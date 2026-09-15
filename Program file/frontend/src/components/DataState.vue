<script setup>
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
    <el-result
      v-else-if="error"
      icon="error"
      :title="errorTitle"
      :sub-title="typeof error === 'string' ? error : errorDescription"
      role="alert"
    >
      <template #extra><el-button type="primary" @click="$emit('retry')">重新加载</el-button></template>
    </el-result>
    <el-empty v-else-if="empty" :description="emptyTitle">
      <p v-if="emptyDescription" class="data-state__description">{{ emptyDescription }}</p>
      <slot name="empty-action" />
    </el-empty>
    <slot v-else />
  </section>
</template>

<style scoped>
.data-state { min-width: 0; }
.data-state__loading { padding: var(--space-md) 0; }
.data-state__description { margin: 0 0 var(--space-md); color: var(--text-muted); }
</style>
