<script setup>
import {computed} from 'vue'
import {statusLabel,statusType} from '../constants/status'
const props=defineProps({
  value:{type:String,default:''},
  label:{type:String,default:''},
  type:{type:String,default:''},
})
const liveStatuses=new Set(['IN_PROGRESS'])
const attentionStatuses=new Set(['PENDING_ACTIVATION','PENDING_CLUB_APPROVAL','PENDING_PAYMENT','REFUND_PENDING','PENDING','GENERATED','PENDING_ADMIN_REVIEW'])
const showDot=computed(()=>liveStatuses.has(props.value)||attentionStatuses.has(props.value))
const isLive=computed(()=>liveStatuses.has(props.value))
</script>
<template><el-tag class="status-tag" :class="{'status-tag--live':isLive}" :type="type||statusType(value)" effect="light" :data-status="value"><span v-if="showDot" class="status-tag__dot" aria-hidden="true"/>{{label||statusLabel(value)}}</el-tag></template>

<style scoped>
.status-tag { font-weight: var(--font-weight-semibold); }
.status-tag__dot { display: inline-block; width: 6px; height: 6px; margin-right: var(--space-2); border-radius: var(--radius-full); background: currentColor; vertical-align: 1px; }
.status-tag--live { color: var(--color-live); border-color: var(--color-live-border); background: var(--color-live-soft); }
</style>
