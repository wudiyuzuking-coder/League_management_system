<script setup>
import {onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {getMyTickets} from '../../api/eTicket'
const router=useRouter(),loading=ref(false),records=ref([]),total=ref(0),status=ref(''),page=ref(1)
const load=async()=>{loading.value=true;try{const d=(await getMyTickets({ticketStatus:status.value||undefined,page:page.value,size:10})).data;records.value=d.records;total.value=d.total}finally{loading.value=false}}
const filter=()=>{page.value=1;load()}
onMounted(load)
</script>
<template><el-card><template #header><div class="head"><b>我的电子票</b><el-radio-group v-model="status" @change="filter"><el-radio-button value="">全部</el-radio-button><el-radio-button value="UNUSED">未使用</el-radio-button><el-radio-button value="USED">已使用</el-radio-button><el-radio-button value="REFUNDED">已退票</el-radio-button></el-radio-group></div></template><el-table :data="records" v-loading="loading" empty-text="暂无电子票"><el-table-column label="比赛" min-width="190"><template #default="{row}">{{row.homeClubName}} vs {{row.awayClubName}}</template></el-table-column><el-table-column label="比赛时间" min-width="165"><template #default="{row}">{{$formatDateTime(row.matchTime)}}</template></el-table-column><el-table-column prop="zoneName" label="比赛票区"/><el-table-column label="座位"><template #default="{row}">{{row.rowNo}}排 {{row.seatNo}}座</template></el-table-column><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.ticketStatus"/></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button link type="primary" @click="router.push(`/user/tickets/${row.ticketId}`)">查看票券</el-button></template></el-table-column></el-table><el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load"/></el-card></template>
<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.el-pagination{margin-top:16px;justify-content:flex-end}</style>
