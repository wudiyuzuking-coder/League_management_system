<script setup>
import {onMounted,ref} from 'vue'
import {getMyTickets} from '../../api/eTicket'

const loading=ref(false),error=ref(''),records=ref([]),total=ref(0),status=ref('UNUSED'),page=ref(1)
const load=async()=>{loading.value=true;error.value='';try{const d=(await getMyTickets({ticketStatus:status.value||undefined,page:page.value,size:10})).data;records.value=d.records;total.value=d.total}catch(e){error.value=e?.message||'加载电子票失败，请稍后重试。'}finally{loading.value=false}}
const filter=()=>{page.value=1;load()}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="我的电子票" subtitle="比赛入场凭证与当前票券状态。" />
    <ActionToolbar title="票券状态" description="按使用和退款状态筛选电子票。"><template #actions><el-radio-group v-model="status" @change="filter"><el-radio-button value="">全部</el-radio-button><el-radio-button value="UNUSED">未使用</el-radio-button><el-radio-button value="USED">已使用</el-radio-button><el-radio-button value="REFUNDED">已退款</el-radio-button></el-radio-group></template></ActionToolbar>
    <DataState class="ticket-state" :loading="loading" :error="error" :empty="!records.length" empty-title="暂无电子票" empty-description="支付成功后，电子票会显示在这里。" @retry="load">
      <div class="ticket-grid">
        <CardShell v-for="row in records" :key="row.ticketId" variant="fixture" compact class="ticket-card">
          <template #header><div class="ticket-card__header"><span>比赛门票</span><StatusTag :value="row.ticketStatus"/></div></template>
          <div class="ticket-card__match"><h2>{{row.homeClubName}} <span>VS</span> {{row.awayClubName}}</h2><p>{{$formatDateTime(row.matchTime)}}</p></div>
          <dl><div><dt>票区</dt><dd>{{row.zoneName}}</dd></div><div><dt>座位</dt><dd>{{row.rowNo}} {{row.seatNo}}</dd></div><div><dt>购票人</dt><dd>{{row.passengerName}}</dd></div></dl>
          <template #footer><div class="ticket-card__footer"><span>{{$statusLabel(row.ticketStatus)}}</span><RouterLink :to="`/user/tickets/${row.ticketId}`" class="el-button el-button--primary">查看电子票</RouterLink></div></template>
        </CardShell>
      </div>
      <el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load"/>
    </DataState>
  </div>
</template>

<style scoped>.ticket-state{margin-top:var(--space-4)}.ticket-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4)}.ticket-card__header,.ticket-card__footer{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3);width:100%;color:var(--color-text-muted);font-size:var(--font-size-sm)}.ticket-card__match{text-align:center}.ticket-card__match h2{margin:0;font-size:var(--font-size-lg)}.ticket-card__match h2 span{margin:0 var(--space-2);color:var(--role-accent);font-size:var(--font-size-xs)}.ticket-card__match p{margin:var(--space-2) 0 var(--space-5);color:var(--color-text-secondary)}.ticket-card dl{display:grid;grid-template-columns:repeat(3,1fr);gap:var(--space-3);margin:0;padding-top:var(--space-4);border-top:1px dashed var(--color-line-strong)}.ticket-card dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.ticket-card dd{margin:var(--space-1) 0 0;font-weight:var(--font-weight-semibold)}@media(max-width:800px){.ticket-grid{grid-template-columns:1fr}}</style>
