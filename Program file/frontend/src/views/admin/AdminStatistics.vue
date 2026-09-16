<script setup>
import {computed,onMounted,ref} from 'vue'
import {getOverviewStatistics,getPopularMatches,getSalesTrend,getClubStatistics,getRefundStatistics,getCheckinStatistics} from '../../api/statistics'

const loading=ref(false),error=ref(''),overview=ref({}),popular=ref([]),trend=ref([]),clubs=ref([]),refunds=ref({}),checkins=ref({})
const money=value=>`￥${Number(value||0).toFixed(2)}`,rate=value=>`${Number(value||0).toFixed(2)}%`
const metrics=computed(()=>[
  {label:'净销售额',value:money(overview.value.netSalesAmount)},
  {label:'有效售票量',value:overview.value.validTicketsSold||0,meta:'张'},
  {label:'退票金额',value:money(overview.value.refundAmount)},
  {label:'已检票',value:overview.value.checkedInTickets||0,meta:'张'},
  {label:'平均上座率',value:rate(overview.value.averageAttendanceRate)},
])
const load=async()=>{loading.value=true;error.value='';try{const [summary,popularResponse,trendResponse,clubResponse,refundResponse,checkinResponse]=await Promise.all([getOverviewStatistics(),getPopularMatches({limit:5}),getSalesTrend(),getClubStatistics(),getRefundStatistics(),getCheckinStatistics()]);overview.value=summary.data;popular.value=popularResponse.data;trend.value=trendResponse.data;clubs.value=clubResponse.data.slice(0,5);refunds.value=refundResponse.data;checkins.value=checkinResponse.data}catch(e){error.value=e?.message||'加载运营统计失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{label:'数据运营'},{label:'运营统计'}]" title="运营统计" subtitle="查看售票、入场、退款和主场运营表现。"><template #actions><RouterLink to="/admin/statistics/matches" class="el-button el-button--primary">查看比赛统计</RouterLink></template></PageHeader>
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
      <MetricStrip :items="metrics" label="运营核心指标" />
      <div class="statistics-grid">
        <TableWrapper title="销售趋势" description="按日期汇总毛销售额、退票金额与净销售额。" label="销售趋势表格"><el-table :data="trend" max-height="310"><el-table-column prop="statDate" label="日期" min-width="120" /><el-table-column label="毛销售额" align="right"><template #default="{row}"><span class="score-nums">{{money(row.grossSalesAmount)}}</span></template></el-table-column><el-table-column label="退票" align="right"><template #default="{row}"><span class="score-nums">{{money(row.refundAmount)}}</span></template></el-table-column><el-table-column label="净销售额" align="right"><template #default="{row}"><strong class="score-nums">{{money(row.netSalesAmount)}}</strong></template></el-table-column></el-table></TableWrapper>
        <TableWrapper title="热门比赛" description="按当前统计口径展示售票表现领先的比赛。" label="热门比赛表格"><el-table :data="popular" max-height="310"><el-table-column label="比赛" min-width="200"><template #default="{row}">{{row.homeClubName}} 对阵 {{row.awayClubName}}</template></el-table-column><el-table-column prop="validSoldCount" label="有效售票" align="right" /><el-table-column label="售票率" align="right"><template #default="{row}">{{rate(row.ticketSaleRate)}}</template></el-table-column><el-table-column label="净销售额" align="right"><template #default="{row}"><span class="score-nums">{{money(row.netSalesAmount)}}</span></template></el-table-column></el-table></TableWrapper>
        <TableWrapper title="俱乐部主场排行" description="比较各俱乐部主场售票与上座表现。" label="俱乐部主场排行表格"><el-table :data="clubs"><el-table-column prop="clubName" label="俱乐部" min-width="160" /><el-table-column prop="validTicketsSold" label="售票" align="right" /><el-table-column label="净销售额" align="right"><template #default="{row}"><span class="score-nums">{{money(row.netSalesAmount)}}</span></template></el-table-column><el-table-column label="上座率" align="right"><template #default="{row}">{{rate(row.averageAttendanceRate)}}</template></el-table-column></el-table></TableWrapper>
        <CardShell title="检票与退票" subtitle="当前统计周期的现场核验与退票概况。"><dl class="operations-summary"><div><dt>检票尝试</dt><dd>{{checkins.totalAttempts||0}}</dd></div><div><dt>检票成功率</dt><dd>{{rate(checkins.successRate)}}</dd></div><div><dt>检票成功</dt><dd>{{checkins.successCount||0}}</dd></div><div><dt>检票失败</dt><dd>{{checkins.failedCount||0}}</dd></div><div><dt>退票申请</dt><dd>{{refunds.totalApplications||0}}</dd></div><div><dt>退票率</dt><dd>{{rate(refunds.refundRate)}}</dd></div></dl></CardShell>
      </div>
    </DataState>
  </div>
</template>

<style scoped>
.statistics-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4);margin-top:var(--space-4)}.operations-summary{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-5);margin:0}.operations-summary dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.operations-summary dd{margin:var(--space-1) 0 0;font-family:var(--font-score);font-size:var(--font-size-xl);font-weight:var(--font-weight-bold)}
@media(max-width:1100px){.statistics-grid{grid-template-columns:1fr}}
</style>
