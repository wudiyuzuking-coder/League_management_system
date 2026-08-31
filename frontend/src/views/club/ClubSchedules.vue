<script setup>
import {onMounted,ref} from 'vue'
import {getClubSchedules} from '../../api/club'
import {formatDateTime} from '../../utils/format'
const rows=ref([]),loading=ref(false)
const load=async()=>{loading.value=true;try{rows.value=(await getClubSchedules()).data}finally{loading.value=false}}
onMounted(load)
</script>
<template><el-card v-loading="loading"><template #header><div class="head"><h2>已确认赛程</h2><el-button @click="load">刷新</el-button></div></template><el-table :data="rows" empty-text="暂无已确认赛程"><el-table-column prop="seasonName" label="赛季"/><el-table-column prop="roundNo" label="轮次"/><el-table-column label="主/客"><template #default="{row}"><el-tag :type="row.home?'success':'info'">{{row.home?'主场':'客场'}}</el-tag></template></el-table-column><el-table-column prop="opponentClubName" label="对手"/><el-table-column label="比赛时间"><template #default="{row}">{{formatDateTime(row.matchDateTime)}}</template></el-table-column><el-table-column prop="stadiumName" label="场馆"/><el-table-column label="距离比赛"><template #default="{row}">{{row.daysUntilMatch>=0?`${row.daysUntilMatch}天`:`已过去${-row.daysUntilMatch}天`}}</template></el-table-column></el-table></el-card></template>
<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.head h2{margin:0}</style>
