<script setup>
import {computed,onMounted,ref} from 'vue'
import {getClubSchedules} from '../../api/club'

const rows=ref([]),loading=ref(false),error=ref(''),expanded=ref([])
const groups=computed(()=>[
  {key:'upcoming',title:'未开始',rows:rows.value.filter(row=>!['IN_PROGRESS','FINISHED'].includes(row.matchStatus))},
  {key:'ongoing',title:'进行中',rows:rows.value.filter(row=>row.matchStatus==='IN_PROGRESS')},
  {key:'finished',title:'已结束',rows:rows.value.filter(row=>row.matchStatus==='FINISHED')},
])
const summary=computed(()=>groups.value.map(group=>({label:group.title,value:group.rows.length,meta:'场比赛'})))
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getClubSchedules()).data}catch(e){error.value=e?.message||'加载俱乐部赛程失败，请稍后重试。'}finally{loading.value=false}}
const toggle=row=>{expanded.value=expanded.value.includes(row.matchId)?expanded.value.filter(id=>id!==row.matchId):[...expanded.value,row.matchId]}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="已发布赛程" subtitle="按比赛状态查看本俱乐部正式赛程与票务概览。"><template #actions><el-button @click="load">刷新</el-button></template></PageHeader>
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无已发布赛程" empty-description="报名结束后，系统自动生成并发布的赛程会显示在这里。" @retry="load">
      <MetricStrip :items="summary" label="赛程状态摘要" />
      <section v-for="group in groups" :key="group.key" class="schedule-group"><div class="schedule-group__heading"><h2>{{group.title}}</h2><span>{{group.rows.length}} 场</span></div><div v-if="!group.rows.length" class="group-empty">该分组暂无比赛</div><article v-for="row in group.rows" :key="row.matchId" class="club-fixture"><div class="club-fixture__meta"><span>{{row.seasonName}} · 第 {{row.roundNo}} 轮</span><StatusTag :value="row.matchStatus"/></div><div class="fixture-board"><div class="fixture-team fixture-team--own"><strong>本队</strong><small>{{row.home?'主场':'客场'}}</small></div><div class="fixture-score"><strong v-if="row.matchStatus==='FINISHED'" class="score-nums">{{row.ownScore}} : {{row.opponentScore}}</strong><strong v-else>VS</strong><span>{{$formatDateTime(row.matchDateTime)}}</span></div><div class="fixture-team fixture-team--opponent"><el-avatar :size="42" :src="row.opponentLogoUrl||undefined" :alt="`${row.opponentClubName}队徽`">{{row.opponentClubName?.slice(0,1)}}</el-avatar><strong>{{row.opponentClubName}}</strong><small>{{row.home?'客场':'主场'}}</small></div></div><p class="fixture-venue">{{row.stadiumName}}</p><div class="club-fixture__actions"><el-button link @click="toggle(row)">{{expanded.includes(row.matchId)?'收起票务':'票务概览'}}</el-button><RouterLink :to="`/club/matches/${row.matchId}/tickets`" class="el-button el-button--primary is-link">进入票务</RouterLink></div><div v-if="expanded.includes(row.matchId)" class="ticketing"><template v-if="row.home"><span>VIP 已售 <b>{{row.soldVipCount||0}}</b></span><span>普通票已售 <b>{{row.soldNormalCount||0}}</b></span><span>票务收入 <b>{{$formatMoney(row.totalRevenue)}}</b></span></template><span>俱乐部收益（{{row.home?'60%':'30%'}}）<b>{{$formatMoney(row.clubRevenue)}}</b></span></div></article></section>
    </DataState>
  </div>
</template>

<style scoped>.schedule-group{overflow:hidden;margin-top:var(--space-4);border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface)}.schedule-group__heading{display:flex;align-items:center;justify-content:space-between;padding:var(--space-4) var(--space-5);border-bottom:1px solid var(--border-color);background:var(--surface-muted)}.schedule-group__heading h2{margin:0;font-size:var(--font-size-lg)}.schedule-group__heading span{color:var(--text-muted);font-size:var(--font-size-sm)}.group-empty{padding:var(--space-6);color:var(--text-muted);text-align:center}.club-fixture{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:var(--space-3) var(--space-5);padding:var(--space-5);border-bottom:1px solid var(--border-color)}.club-fixture:last-child{border-bottom:0}.club-fixture__meta{grid-column:1/-1;display:flex;align-items:center;justify-content:space-between;color:var(--color-text-muted);font-size:var(--font-size-xs)}.fixture-board{display:grid;grid-template-columns:minmax(120px,1fr) 150px minmax(160px,1fr);align-items:center;gap:var(--space-4);min-width:0}.fixture-team{display:grid;gap:var(--space-1)}.fixture-team strong{font-size:var(--font-size-lg)}.fixture-team small,.fixture-score span,.fixture-venue{color:var(--color-text-muted);font-size:var(--font-size-xs)}.fixture-team--opponent{grid-template-columns:42px 1fr;align-items:center;text-align:right}.fixture-team--opponent small{grid-column:2}.fixture-score{display:grid;justify-items:center;gap:var(--space-1);text-align:center}.fixture-score>strong{font-size:var(--font-size-2xl)}.fixture-venue{grid-column:1;margin:0}.club-fixture__actions{grid-column:2;grid-row:2/4;display:flex;align-items:center;gap:var(--space-sm)}.ticketing{grid-column:1/-1;display:flex;flex-wrap:wrap;gap:var(--space-6);padding:var(--space-3) var(--space-4);border-radius:var(--radius-md);background:var(--surface-muted);color:var(--text-secondary)}.ticketing b{margin-left:var(--space-1);color:var(--text-primary)}@media(max-width:900px){.club-fixture{grid-template-columns:1fr}.club-fixture__actions{grid-column:1;grid-row:auto}.fixture-board{grid-template-columns:1fr 100px 1fr}}
</style>
