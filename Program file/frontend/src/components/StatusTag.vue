<script setup>
import {statusLabel,statusType} from '../constants/status'
defineProps({value:{type:String,default:''}})
</script>
<template><el-tag class="status-tag" :type="statusType(value)" effect="light"><span class="status-tag__dot" aria-hidden="true"/>{{statusLabel(value)}}</el-tag></template>

<style scoped>
.status-tag { font-weight: 650; }
.status-tag__dot { display: inline-block; width: 6px; height: 6px; margin-right: 6px; border-radius: 50%; background: currentColor; vertical-align: 1px; }
</style>
