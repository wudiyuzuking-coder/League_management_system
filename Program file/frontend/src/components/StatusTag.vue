<script setup>
import {statusLabel,statusType} from '../constants/status'
defineProps({value:{type:String,default:''}})
</script>
<template><el-tag :type="statusType(value)">{{statusLabel(value)}}</el-tag></template>
