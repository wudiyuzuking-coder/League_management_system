<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getSafeBackTarget } from '../utils/safeBack'

const props = defineProps({
  fallback: { type: [String, Object], required: true },
})
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const goBack = () => router.push(getSafeBackTarget({
  role: authStore.user?.roleCode,
  fallback: props.fallback,
  currentPath: route.fullPath,
}))
</script>

<template>
  <button type="button" class="back-button" aria-label="返回上一页" @click="goBack">
    <span aria-hidden="true">←</span>
    <span>返回上一页</span>
  </button>
</template>

<style scoped>
.back-button { display: inline-flex; align-items: center; gap: var(--space-2); margin: 0 0 var(--space-3); padding: var(--space-2) var(--space-3); border: 1px solid var(--color-line-strong); border-radius: var(--radius-md); background: var(--color-surface); color: var(--color-text-secondary); cursor: pointer; font: inherit; font-weight: var(--font-weight-semibold); }
.back-button:hover { border-color: var(--primary); color: var(--primary); }
.back-button:focus-visible { outline: none; box-shadow: var(--focus-ring); }
</style>
