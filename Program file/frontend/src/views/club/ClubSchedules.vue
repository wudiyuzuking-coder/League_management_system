<script setup>
import {computed,onMounted,ref} from 'vue'
import {getClubSchedules} from '../../api/club'

const rows=ref([]),loading=ref(false),error=ref(''),expanded=ref([])
const groups=computed(()=>[
  {key:'upcoming',title:'未开始',rows:rows.value.filter(row=>!['IN_PROGRESS','FINISHED'].includes(row.matchStatus))},
  {key:'ongoing',title:'进行中',rows:rows.value.filter(row=>row.matchStatus==='IN_PROGRESS')},
  {key:'finished',title:'已结束',rows:rows.value.filter(row=>row.matchStatus==='FINISHED')},
])
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getClubSchedules()).data}catch(e){error.value=e?.message||'加载俱乐部赛程失败，请稍后重试。'}finally{loading.value=false}}
const toggle=row=>{expanded.value=expanded.value.includes(row.matchId)?expanded.value.filter(id=>id!==row.matchId):[...expanded.value,row.matchId]}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="已确认赛程" subtitle="按比赛状态查看本俱乐部正式赛程与票务概览。"><template #actions><el-button @click="load">刷新</el-button></template></PageHeader>
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无已确认赛程" empty-description="赛事管理员确认赛程后会显示在这里。" @retry="load">
      <section v-for="group in groups" :key="group.key" class="schedule-group"><div class="schedule-group__heading"><h2>{{group.title}}</h2><span>{{group.rows.length}} 场</span></div><div v-if="!group.rows.length" class="group-empty">该分组暂无比赛</div><article v-for="row in group.rows" :key="row.matchId" class="club-fixture"><div class="club-fixture__identity"><el-avatar :size="50" :src="row.opponentLogoUrl||undefined" :alt="`${row.opponentClubName}队徽`">{{row.opponentClubName?.slice(0,1)}}</el-avatar><div><span>{{row.home?'主场':'客场'}} · {{row.seasonName}} · 第 {{row.roundNo}} 轮</span><h3>{{row.home?'VS ':''}}{{row.opponentClubName}}{{row.home?'':' VS'}}</h3><p>{{$formatDateTime(row.matchDateTime)}} · {{row.stadiumName}}</p></div></div><div v-if="row.matchStatus==='FINISHED'" class="score tabular-nums">{{row.ownScore}} : {{row.opponentScore}}</div><StatusTag :value="row.matchStatus"/><div class="club-fixture__actions"><el-button link @click="toggle(row)">{{expanded.includes(row.matchId)?'收起票务':'票务概览'}}</el-button><RouterLink :to="`/club/matches/${row.matchId}/tickets`" class="el-button el-button--primary is-link">进入票务</RouterLink></div><div v-if="expanded.includes(row.matchId)" class="ticketing"><span>已售 VIP <b>{{row.soldVipCount||0}}</b></span><span>已售普通票 <b>{{row.soldNormalCount||0}}</b></span><span v-if="row.home">总营收 <b>{{$formatMoney(row.totalRevenue)}}</b></span><span>本队收益（{{row.home?'60%':'30%'}}）<b>{{$formatMoney(row.clubRevenue)}}</b></span></div></article></section>
    </DataState>
  </div>
</template>

<style scoped>.schedule-group{overflow:hidden;margin-bottom:var(--space-lg);border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface)}.schedule-group__heading{display:flex;align-items:center;justify-content:space-between;padding:16px 20px;border-bottom:1px solid var(--border-color);background:var(--surface-muted)}.schedule-group__heading h2{margin:0;font-size:18px}.schedule-group__heading span{color:var(--text-muted);font-size:13px}.group-empty{padding:26px;color:var(--text-muted);text-align:center}.club-fixture{display:grid;grid-template-columns:minmax(360px,1fr) auto auto auto;align-items:center;gap:var(--space-lg);padding:18px 20px;border-bottom:1px solid var(--border-color)}.club-fixture:last-child{border-bottom:0}.club-fixture__identity{display:flex;align-items:center;gap:12px;min-width:0}.club-fixture__identity span,.club-fixture__identity p{color:var(--text-muted);font-size:12px}.club-fixture__identity h3{margin:4px 0;font-size:17px}.club-fixture__identity p{margin:0}.score{font-size:24px;font-weight:800}.club-fixture__actions{display:flex;align-items:center;gap:var(--space-sm)}.ticketing{grid-column:1/-1;display:flex;gap:30px;padding:13px 16px;border-radius:var(--radius-md);background:var(--surface-muted);color:var(--text-secondary)}.ticketing b{margin-left:5px;color:var(--text-primary)}</style>
