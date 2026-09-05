<script setup>
import {onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {getCheckerMatches} from '../../api/checkin'
const router=useRouter(),records=ref([]),loading=ref(false),status=ref(''),dates=ref([])
const load=async()=>{loading.value=true;try{const params={matchStatus:status.value||undefined};if(dates.value?.length===2){params.startDate=dates.value[0];params.endDate=dates.value[1]}records.value=(await getCheckerMatches(params)).data}finally{loading.value=false}}
onMounted(load)
</script>
<template><el-card><template #header><div class="head"><b>主场检票比赛</b><div><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期"/><el-select v-model="status" clearable placeholder="全部状态" style="width:150px;margin-left:8px"><el-option label="已发布" value="PUBLISHED"/><el-option label="进行中" value="IN_PROGRESS"/><el-option label="已结束" value="FINISHED"/><el-option label="已取消" value="CANCELLED"/></el-select><el-button type="primary" @click="load">查询</el-button></div></div></template><el-table :data="records" v-loading="loading" empty-text="暂无可检票比赛"><el-table-column label="对阵" min-width="220"><template #default="{row}">{{row.homeClubName}} vs {{row.awayClubName}}</template></el-table-column><el-table-column label="比赛时间" min-width="175"><template #default="{row}">{{$formatDateTime(row.matchTime)}}</template></el-table-column><el-table-column prop="stadiumName" label="场馆"/><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.matchStatus"/></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button type="primary" :disabled="!row.checkinAvailable" @click="router.push(`/checker/matches/${row.matchId}/checkin`)">进入工作台</el-button></template></el-table-column></el-table></el-card></template>
<style scoped>.head{display:flex;justify-content:space-between;align-items:center;gap:16px}</style>
