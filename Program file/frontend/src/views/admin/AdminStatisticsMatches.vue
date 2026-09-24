<script setup>
import {onMounted,ref} from 'vue'
import {getMatchStatistics,getMatchStatistic} from '../../api/statistics'
import {statusLabel} from '../../constants/status'

const records=ref([]),total=ref(0),page=ref(1),status=ref(''),clubId=ref(''),loading=ref(false),error=ref(''),detail=ref(null),detailLoading=ref(false)
const matchStatuses=['DRAFT','PUBLISHED','IN_PROGRESS','FINISHED','CANCELLED']
const money=value=>`￥${Number(value||0).toFixed(2)}`,rate=value=>`${Number(value||0).toFixed(2)}%`
const load=async()=>{loading.value=true;error.value='';try{const response=(await getMatchStatistics({matchStatus:status.value||undefined,clubId:clubId.value||undefined,page:page.value,size:10})).data;records.value=response.records;total.value=response.total}catch(e){error.value=e?.message||'加载比赛统计失败，请稍后重试。'}finally{loading.value=false}}
const search=()=>{page.value=1;load()}
const reset=()=>{clubId.value='';status.value='';search()}
const show=async id=>{detailLoading.value=true;try{detail.value=(await getMatchStatistic(id)).data}finally{detailLoading.value=false}}
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader back back-fallback="/admin/statistics" :breadcrumb="[{label:'运营统计',to:'/admin/statistics'},{label:'比赛统计'}]" title="比赛销售统计" subtitle="按比赛查看售票、入场、退款和检票异常。" />
    <FilterBar label="比赛统计筛选">
      <el-form-item label="俱乐部编号"><el-input v-model="clubId" name="statistics-club-id" inputmode="numeric" autocomplete="off" placeholder="例如：4…" /></el-form-item>
      <el-form-item label="比赛状态"><el-select v-model="status" clearable placeholder="全部状态…"><el-option v-for="item in matchStatuses" :key="item" :label="statusLabel(item)" :value="item" /></el-select></el-form-item>
      <template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="search">查询</el-button></template>
    </FilterBar>
    <TableWrapper label="比赛销售统计表格">
      <DataState :loading="loading" :error="error" :empty="!records.length" empty-title="暂无比赛统计" empty-description="调整筛选条件后重新查询。" @retry="load">
        <el-table :data="records"><el-table-column label="比赛" min-width="220"><template #default="{row}">{{row.homeClubName}} 对阵 {{row.awayClubName}}</template></el-table-column><el-table-column label="比赛时间" min-width="170"><template #default="{row}">{{$formatDateTime(row.matchTime)}}</template></el-table-column><el-table-column label="状态" width="120"><template #default="{row}"><StatusTag :value="row.matchStatus" /></template></el-table-column><el-table-column prop="totalSeatCount" label="有效容量" align="right" /><el-table-column prop="validSoldCount" label="有效售票" align="right" /><el-table-column prop="checkedInCount" label="入场" align="right" /><el-table-column label="售票率" align="right"><template #default="{row}">{{rate(row.ticketSaleRate)}}</template></el-table-column><el-table-column label="上座率" align="right"><template #default="{row}">{{rate(row.attendanceRate)}}</template></el-table-column><el-table-column label="净销售额" min-width="120" align="right"><template #default="{row}"><span class="score-nums">{{money(row.netSalesAmount)}}</span></template></el-table-column><el-table-column label="操作" width="90" fixed="right"><template #default="{row}"><el-button link type="primary" :loading="detailLoading" @click="show(row.matchId)">查看详情</el-button></template></el-table-column></el-table>
      </DataState>
      <template #footer><el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load" /></template>
    </TableWrapper>
    <el-dialog :model-value="Boolean(detail)" title="单场统计详情" width="860px" @update:model-value="value=>{if(!value)detail=null}">
      <template v-if="detail">
        <CardShell :title="`${detail.homeClubName} 对阵 ${detail.awayClubName}`" subtitle="销售、退款与现场核验统计。" compact><dl class="summary-grid"><div><dt>毛销售额</dt><dd>{{money(detail.grossSalesAmount)}}</dd></div><div><dt>退票金额</dt><dd>{{money(detail.refundAmount)}}</dd></div><div><dt>净销售额</dt><dd>{{money(detail.netSalesAmount)}}</dd></div><div><dt>有效售票</dt><dd>{{detail.validSoldCount}}</dd></div><div><dt>退票票数</dt><dd>{{detail.refundedCount}}</dd></div><div><dt>已入场</dt><dd>{{detail.checkedInCount}}</dd></div></dl></CardShell>
        <CardShell class="dialog-section" title="检票结果" subtitle="未知结果会统一显示为“未知状态”。" compact><dl class="checkin-grid"><div><dt>检票成功</dt><dd>{{detail.successCount}}</dd></div><div><dt>未找到票码</dt><dd>{{detail.codeNotFoundCount}}</dd></div><div><dt>非本场比赛</dt><dd>{{detail.wrongMatchCount}}</dd></div><div><dt>订单无效</dt><dd>{{detail.orderInvalidCount}}</dd></div><div><dt>票券已使用</dt><dd>{{detail.ticketUsedCount}}</dd></div><div><dt>票券已退款</dt><dd>{{detail.ticketRefundedCount}}</dd></div><div><dt>票券已作废</dt><dd>{{detail.ticketVoidCount}}</dd></div></dl></CardShell>
        <TableWrapper class="dialog-section" title="票区销售情况" compact label="票区销售统计表格"><el-table :data="detail.zones||[]" size="small"><el-table-column prop="zoneName" label="票区" min-width="130" /><el-table-column label="票价" align="right"><template #default="{row}">{{money(row.ticketPrice)}}</template></el-table-column><el-table-column prop="totalSeatCount" label="有效容量" align="right" /><el-table-column prop="validSoldCount" label="有效售票" align="right" /><el-table-column prop="refundedCount" label="退票" align="right" /><el-table-column label="售票率" align="right"><template #default="{row}">{{rate(row.ticketSaleRate)}}</template></el-table-column></el-table></TableWrapper>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.summary-grid,.checkin-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--space-4);margin:0}.checkin-grid{grid-template-columns:repeat(4,minmax(0,1fr))}.summary-grid dt,.checkin-grid dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.summary-grid dd,.checkin-grid dd{margin:var(--space-1) 0 0;font-family:var(--font-score);font-size:var(--font-size-lg);font-weight:var(--font-weight-bold)}.dialog-section{margin-top:var(--space-4)}
</style>
