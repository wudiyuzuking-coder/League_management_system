<script setup>
import {onMounted,ref} from 'vue'
import {getCheckerCheckins} from '../../api/checkin'
const records=ref([]),total=ref(0),page=ref(1),result=ref(''),matchId=ref(''),loading=ref(false)
const results=['SUCCESS','CODE_NOT_FOUND','WRONG_MATCH','ORDER_INVALID','TICKET_USED','TICKET_REFUNDED','TICKET_VOID']
const load=async()=>{loading.value=true;try{const d=(await getCheckerCheckins({matchId:matchId.value||undefined,checkResult:result.value||undefined,page:page.value,size:10})).data;records.value=d.records;total.value=d.total}finally{loading.value=false}}
const search=()=>{page.value=1;load()};onMounted(load)
</script>
<template><el-card><template #header><div class="head"><b>我的检票记录</b><div><el-input v-model="matchId" placeholder="比赛ID" style="width:120px"/><el-select v-model="result" clearable placeholder="全部结果" style="width:190px;margin-left:8px"><el-option v-for="x in results" :key="x" :label="x" :value="x"/></el-select><el-button type="primary" @click="search">查询</el-button></div></div></template><el-table :data="records" v-loading="loading"><el-table-column prop="checkedAt" label="检票时间" min-width="175"/><el-table-column prop="matchName" label="比赛" min-width="220"/><el-table-column prop="inputTicketCode" label="输入票码" min-width="220"/><el-table-column label="结果"><template #default="{row}"><el-tag :type="row.checkResult==='SUCCESS'?'success':'danger'">{{row.checkResult}}</el-tag></template></el-table-column><el-table-column prop="remark" label="说明" min-width="220"/></el-table><el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load"/></el-card></template>
<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.el-pagination{margin-top:16px;justify-content:flex-end}</style>
