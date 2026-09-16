<script setup>
import {onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getTicket} from '../../api/eTicket'
import {maskIdCard} from '../../utils/privacy'

const route=useRoute(),ticket=ref({}),loading=ref(false),error=ref('')
const load=async()=>{loading.value=true;error.value='';try{ticket.value=(await getTicket(route.params.id)).data}catch(e){error.value=e?.message||'加载电子票失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="电子票" subtitle="比赛入场时请出示本票券。" :breadcrumb="[{label:'我的电子票',to:'/user/tickets'},{label:'票券详情'}]">
      <template #status><StatusTag v-if="ticket.ticketStatus" :value="ticket.ticketStatus"/></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!ticket.ticketId" empty-title="电子票不存在或无权查看" @retry="load">
      <el-alert v-if="ticket.ticketStatus==='USED'" :title="`已入场，入场时间：${$formatDateTime(ticket.enterTime)}`" type="success" :closable="false" class="ticket-alert"/>
      <el-alert v-else-if="ticket.ticketStatus==='REFUNDED'" title="该票已退款，不可入场" type="warning" :closable="false" class="ticket-alert"/>
      <el-alert v-else-if="ticket.ticketStatus==='VOID'" title="票券已作废，不可入场" type="error" :closable="false" class="ticket-alert"/>
      <article class="event-ticket">
        <header><span>联赛比赛电子票</span><StatusTag :value="ticket.ticketStatus"/></header>
        <div class="event-ticket__match"><h2>{{ticket.homeClubName}} <span>VS</span> {{ticket.awayClubName}}</h2><p>{{$formatDateTime(ticket.matchTime)}}</p><p>{{ticket.stadiumName}}</p></div>
        <div class="event-ticket__tear" aria-hidden="true"/>
        <dl><div><dt>票种</dt><dd>{{String(ticket.zoneName||'').toUpperCase().includes('VIP')?'VIP':'普通票'}}</dd></div><div><dt>实际票区</dt><dd>{{ticket.zoneName}}</dd></div><div><dt>排号</dt><dd>{{ticket.rowNo}}排</dd></div><div><dt>座号</dt><dd>{{ticket.seatNo}}座</dd></div><div><dt>购票人</dt><dd>{{ticket.passengerName}}</dd></div><div><dt>身份证</dt><dd>{{maskIdCard(ticket.passengerIdCard)}}</dd></div></dl>
        <div class="event-ticket__code"><span>入场票码</span><strong>{{ticket.ticketCode}}</strong><small>请妥善保管票码，入场时由检票员核验。</small></div>
      </article>
    </DataState>
  </div>
</template>

<style scoped>.ticket-alert{max-width:760px;margin:0 auto var(--space-4)}.event-ticket{position:relative;max-width:760px;margin:0 auto;overflow:hidden;border:1px solid var(--color-line-strong);border-top:4px solid var(--role-accent);border-radius:var(--radius-lg);background:var(--color-surface);box-shadow:var(--shadow-raised)}.event-ticket header{display:flex;align-items:center;justify-content:space-between;padding:var(--space-4) var(--space-6);border-bottom:1px solid var(--color-line);color:var(--color-text-muted);font-size:var(--font-size-sm)}.event-ticket__match{padding:var(--space-8) var(--space-6);text-align:center}.event-ticket__match h2{margin:0;font-size:var(--font-size-2xl)}.event-ticket__match h2 span{margin:0 var(--space-3);color:var(--role-accent);font-size:var(--font-size-sm)}.event-ticket__match p{margin:var(--space-2) 0 0;color:var(--color-text-secondary)}.event-ticket__tear{border-top:2px dashed var(--color-line-strong)}.event-ticket dl{display:grid;grid-template-columns:repeat(3,1fr);gap:var(--space-5);margin:0;padding:var(--space-6)}.event-ticket dt,.event-ticket__code span,.event-ticket__code small{color:var(--color-text-muted);font-size:var(--font-size-xs)}.event-ticket dd{margin:var(--space-1) 0 0;font-weight:var(--font-weight-semibold)}.event-ticket__code{display:flex;align-items:center;flex-direction:column;padding:var(--space-5) var(--space-6);border-top:1px solid var(--color-line);background:var(--color-surface-subtle);text-align:center}.event-ticket__code strong{margin:var(--space-2) 0;font-family:var(--font-score);font-size:var(--font-size-xl);letter-spacing:.06em}@media(max-width:640px){.event-ticket dl{grid-template-columns:repeat(2,1fr)}}
</style>
