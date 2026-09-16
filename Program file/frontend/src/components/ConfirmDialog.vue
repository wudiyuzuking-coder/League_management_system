<script setup>
const visible = defineModel({ type: Boolean, default: false })

defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  impact: { type: String, default: '' },
  confirmText: { type: String, default: '确认' },
  cancelText: { type: String, default: '取消' },
  danger: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  width: { type: [String, Number], default: '480px' },
})

const emit = defineEmits(['confirm', 'cancel'])
const cancel = () => {
  visible.value = false
  emit('cancel')
}
</script>

<template>
  <el-dialog v-model="visible" class="confirm-dialog" :title="title" :width="width" :close-on-click-modal="!loading" :close-on-press-escape="!loading" :show-close="!loading">
    <div class="confirm-dialog__content" :class="{ 'confirm-dialog__content--danger': danger }">
      <span class="confirm-dialog__signal" aria-hidden="true">!</span>
      <div>
        <p>{{ message }}</p>
        <small v-if="impact">{{ impact }}</small>
      </div>
    </div>
    <slot />
    <template #footer>
      <el-button :disabled="loading" @click="cancel">{{ cancelText }}</el-button>
      <el-button :type="danger ? 'danger' : 'primary'" :loading="loading" @click="emit('confirm')">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.confirm-dialog__content { display: grid; grid-template-columns: 36px 1fr; align-items: start; gap: var(--space-3); }
.confirm-dialog__signal { display: grid; width: 32px; height: 32px; border-radius: var(--radius-full); color: var(--color-info); background: var(--color-info-soft); font-weight: var(--font-weight-bold); place-items: center; }
.confirm-dialog__content--danger .confirm-dialog__signal { color: var(--color-danger); background: var(--color-danger-soft); }
.confirm-dialog__content p { margin: var(--space-1) 0 0; color: var(--color-text-primary); font-weight: var(--font-weight-medium); line-height: var(--line-height-body); }
.confirm-dialog__content small { display: block; margin-top: var(--space-2); color: var(--color-text-muted); font-size: var(--font-size-sm); line-height: var(--line-height-body); }
</style>
