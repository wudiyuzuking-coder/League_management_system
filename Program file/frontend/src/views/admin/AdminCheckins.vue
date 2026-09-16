<script setup>
import {onMounted,ref} from 'vue'
import {getAdminCheckin,getAdminCheckins} from '../../api/checkin'

const records=ref([]),total=ref(0),page=ref(1),result=ref(''),matchId=ref(''),checkerId=ref(''),loading=ref(false),error=ref(''),detail=ref(null),detailLoading=ref(false)
const results=['SUCCESS','CODE_NOT_FOUND','WRONG_MATCH','ORDER_INVALID','TICKET_USED','TICKET_REFUNDED','TICKET_VOID']
const checkinLabels={SUCCESS:'检票成功',CODE_NOT_FOUND:'未找到票码',WRONG_MATCH:'非本场比赛',ORDER_INVALID:'订单无效',TICKET_USED:'票券已使用',TICKET_REFUNDED:'票券已退款',TICKET_VOID:'票券已作废'}
const checkinLabel=value=>checkinLabels[value]||'未知状态'
const load=async()=>{loading.value=true;error.value='';try{const d=(await getAdminCheckins({matchId:matchId.value||undefined,checkerId:checkerId.value||undefined,checkResult:result.value||undefined,page:page.value,size:10})).data;records.value=d.records;total.value=d.total}catch(e){error.value=e?.message||'加载检票记录失败，请稍后重试。'}finally{loading.value=false}}
const search=()=>{page.value=1;load()}
const reset=()=>{matchId.value='';checkerId.value='';result.value='';search()}
const show=async id=>{detailLoading.value=true;try{detail.value=(await getAdminCheckin(id)).data}finally{detailLoading.value=false}}
onMounted(load)
</script>

<template>
  <div class="governance-page">
    <PageHeader :breadcrumb="[{label:'系统管理'},{label:'检票记录'}]" title="检票记录" subtitle="追踪票券核验结果、异常原因与现场操作记录。" />
    <FilterBar label="检票记录筛选">
      <el-form-item label="比赛编号"><el-input v-model="matchId" name="checkin-match-id" inputmode="numeric" autocomplete="off" placeholder="例如：12…" /></el-form-item>
      <el-form-item label="检票员编号"><el-input v-model="checkerId" name="checker-id" inputmode="numeric" autocomplete="off" placeholder="例如：5…" /></el-form-item>
      <el-form-item label="检票结果"><el-select v-model="result" clearable placeholder="全部结果…"><el-option v-for="item in results" :key="item" :label="checkinLabel(item)" :value="item" /></el-select></el-form-item>
      <template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></template>
    </FilterBar>
    <TableWrapper label="检票记录表格">
      <DataState :loading="loading" :error="error" :empty="!records.length" empty-title="暂无检票记录" empty-description="调整筛选条件后重新查询。" @retry="load">
        <el-table :data="records"><el-table-column label="检票时间" min-width="175"><template #default="{row}">{{$formatDateTime(row.checkedAt)}}</template></el-table-column><el-table-column prop="matchName" label="比赛" min-width="220" show-overflow-tooltip /><el-table-column prop="checkerUsername" label="检票员" min-width="120" /><el-table-column prop="inputTicketCode" label="输入票码" min-width="210" show-overflow-tooltip /><el-table-column label="结果" width="140"><template #default="{row}"><StatusTag :value="row.checkResult" :label="checkinLabel(row.checkResult)" /></template></el-table-column><el-table-column label="操作" width="90" fixed="right"><template #default="{row}"><el-button link type="primary" :loading="detailLoading" @click="show(row.checkinId)">查看详情</el-button></template></el-table-column></el-table>
      </DataState>
      <template #footer><el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load" /></template>
    </TableWrapper>
    <el-dialog :model-value="Boolean(detail)" title="检票记录详情" width="600px" @update:model-value="value=>{if(!value)detail=null}"><el-descriptions v-if="detail" :column="1" border><el-descriptions-item label="结果"><StatusTag :value="detail.checkResult" :label="checkinLabel(detail.checkResult)" /></el-descriptions-item><el-descriptions-item label="比赛">{{detail.matchName}}</el-descriptions-item><el-descriptions-item label="检票员">{{detail.checkerName}}（{{detail.checkerUsername}}）</el-descriptions-item><el-descriptions-item label="票码">{{detail.inputTicketCode}}</el-descriptions-item><el-descriptions-item label="座位">{{detail.zoneName}} {{detail.rowLabel}} {{detail.seatLabel}}</el-descriptions-item><el-descriptions-item label="时间">{{$formatDateTime(detail.checkedAt)}}</el-descriptions-item><el-descriptions-item label="说明">{{detail.remark||'—'}}</el-descriptions-item></el-descriptions></el-dialog>
  </div>
</template>
