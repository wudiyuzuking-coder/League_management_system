<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getMatch} from '../../api/match'
import {getStadium} from '../../api/stadium'
import {getTicketZones} from '../../api/ticket'
import TicketZoneList from '../../components/TicketZoneList.vue'
const route=useRoute(),match=ref({}),stadium=ref({}),ticketZones=ref([]),loading=ref(false),error=ref('')
const remaining=computed(()=>ticketZones.value.reduce((sum,z)=>sum+Number(z.availableSeatCount||0),0))
const saleStatus=computed(()=>{const states=ticketZones.value.map(z=>z.saleState);if(states.includes('AVAILABLE'))return'ON_SALE';if(states.includes('NOT_STARTED'))return'NOT_STARTED';if(states.length&&states.every(state=>state==='SOLD_OUT'))return'SOLD_OUT';if(states.includes('PAUSED'))return'PAUSED';return states[0]||'NOT_ENABLED'})
const load=async()=>{loading.value=true;error.value='';try{match.value=(await getMatch(route.params.id)).data;const tasks=[getTicketZones(route.params.id)];if(match.value.stadiumId)tasks.push(getStadium(match.value.stadiumId));const values=await Promise.all(tasks);ticketZones.value=values[0].data;if(values[1])stadium.value=values[1].data}catch(e){error.value=e?.message||'加载比赛失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div class="match-page">
    <PageHeader title="比赛详情" subtitle="查看对阵、比赛信息与当前可售票种。" :breadcrumb="[{label:'联赛赛季',to:'/user/seasons'},{label:match.seasonName||'赛季',to:match.seasonId?`/user/seasons/${match.seasonId}/rounds`:undefined},{label:match.roundName||'比赛'}]" />
    <DataState :loading="loading" :error="error" :empty="!match.matchId" empty-title="比赛不存在或暂不可查看" @retry="load">
      <section class="match-hero">
        <div class="match-hero__meta"><span>{{match.seasonName}}</span><span>{{match.roundName}}</span><StatusTag :value="match.matchStatus"/></div>
        <div class="match-hero__fixture">
          <RouterLink :to="`/user/clubs/${match.homeClubId}`" class="club-link">
            <el-avatar :size="88" :src="match.homeLogoUrl" :alt="`${match.homeClubName}队徽`">{{match.homeClubName?.[0]}}</el-avatar><h2>{{match.homeClubName}}</h2><span>主队</span>
          </RouterLink>
          <div class="match-score tabular-nums"><strong>{{match.homeScore==null?'VS':`${match.homeScore} : ${match.awayScore}`}}</strong><small>{{match.homeScore==null?'比赛日':'全场比分'}}</small></div>
          <RouterLink :to="`/user/clubs/${match.awayClubId}`" class="club-link">
            <el-avatar :size="88" :src="match.awayLogoUrl" :alt="`${match.awayClubName}队徽`">{{match.awayClubName?.[0]}}</el-avatar><h2>{{match.awayClubName}}</h2><span>客队</span>
          </RouterLink>
        </div>
        <div class="match-hero__details">
          <span><small>比赛时间</small><b>{{$formatDateTime(match.matchTime)}}</b></span>
          <span><small>比赛场馆</small><b>{{match.stadiumName||'待确认'}}</b></span>
          <span><small>场馆地址</small><b>{{stadium.address||'地址待确认'}}</b></span>
          <span><small>售票状态</small><b><StatusTag :value="saleStatus" /></b></span>
          <span v-if="saleStatus==='NOT_STARTED'"><small>开售时间</small><b>{{$formatDateTime(match.saleStartTime)}}</b></span>
          <span v-else><small>剩余票数</small><b class="tabular-nums">{{remaining}}</b></span>
        </div>
      </section>
      <TicketZoneList :match-id="route.params.id" :match="match" />
    </DataState>
  </div>
</template>

<style scoped>
.match-hero{overflow:hidden;border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface);box-shadow:var(--shadow-sm)}
.match-hero__meta{display:flex;align-items:center;justify-content:center;gap:12px;padding:13px 20px;color:var(--text-secondary);font-size:13px;background:var(--surface-muted)}
.match-hero__fixture{display:grid;grid-template-columns:1fr 150px 1fr;align-items:center;max-width:760px;margin:0 auto;padding:36px 28px 28px;text-align:center}.club-link{display:flex;align-items:center;flex-direction:column;color:inherit}.club-link h2{margin:12px 0 4px;font-size:22px}.club-link span{color:var(--text-muted);font-size:12px}.club-link:hover h2{color:var(--primary)}.match-score{display:flex;align-items:center;flex-direction:column;gap:7px}.match-score strong{font-size:36px;letter-spacing:.02em}.match-score small{color:var(--primary);font-size:10px;font-weight:800;letter-spacing:.15em}
.match-hero__details{display:grid;grid-template-columns:1.1fr 1fr 1.5fr .9fr;gap:var(--space-md);padding:18px 24px;border-top:1px solid var(--border-color);background:var(--color-surface-subtle)}.match-hero__details span{display:flex;flex-direction:column;gap:5px}.match-hero__details small{color:var(--text-muted)}.match-hero__details b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
</style>
