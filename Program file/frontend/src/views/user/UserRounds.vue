<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getSeasonSchedule} from '../../api/league'

const route=useRoute(),season=ref({}),loading=ref(false),error=ref('')
const rounds=computed(()=>season.value.rounds||[])
const matchCount=computed(()=>rounds.value.reduce((sum,round)=>sum+(round.matches?.length||0),0))
const load=async()=>{loading.value=true;error.value='';try{season.value=(await getSeasonSchedule(route.params.id)).data}catch(e){error.value=e?.message||'加载赛程失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div class="season-detail">
    <PageHeader :title="season.seasonName||'赛季详情'" subtitle="按轮次查看完整赛程、比赛状态与售票信息。" :breadcrumb="[{label:'联赛赛季',to:'/user/seasons'},{label:'完整赛程'}]">
      <template #status><StatusTag v-if="season.seasonStatus" :value="season.seasonStatus"/></template>
      <template #actions><RouterLink :to="`/user/seasons/${route.params.id}/standings`" class="el-button">查看积分榜</RouterLink></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!rounds.length" empty-title="赛程尚未正式确认" empty-description="赛程确认后会按轮次显示在这里。" @retry="load">
      <section class="season-summary" aria-label="赛季摘要">
        <div><span>参赛球队</span><strong>{{season.clubCount??'—'}}<small>支</small></strong></div>
        <div><span>比赛轮次</span><strong>{{rounds.length}}<small>轮</small></strong></div>
        <div><span>比赛总数</span><strong>{{matchCount}}<small>场</small></strong></div>
        <div class="season-summary__dates"><span>赛季周期</span><strong>{{$formatDate(season.startDate)}} — {{$formatDate(season.endDate)}}</strong></div>
      </section>
      <section v-for="round in rounds" :key="round.roundNo" class="round-section">
        <div class="round-heading"><span>ROUND {{round.roundNo}}</span><h2>第 {{round.roundNo}} 轮</h2><div aria-hidden="true"/></div>
        <div class="fixture-list">
          <article v-for="match in round.matches" :key="match.matchId" class="fixture-card">
            <div class="fixture-card__teams">
              <RouterLink :to="`/user/clubs/${match.homeClubId}`" class="team-link">
                <el-avatar :size="56" :src="match.homeLogoUrl" :alt="`${match.homeClubName}队徽`">{{match.homeClubName?.[0]}}</el-avatar><strong>{{match.homeClubName}}</strong><small>主队</small>
              </RouterLink>
              <div class="versus tabular-nums"><strong>{{match.homeScore==null?'VS':`${match.homeScore} : ${match.awayScore}`}}</strong><StatusTag :value="match.matchStatus"/></div>
              <RouterLink :to="`/user/clubs/${match.awayClubId}`" class="team-link">
                <el-avatar :size="56" :src="match.awayLogoUrl" :alt="`${match.awayClubName}队徽`">{{match.awayClubName?.[0]}}</el-avatar><strong>{{match.awayClubName}}</strong><small>客队</small>
              </RouterLink>
            </div>
            <div class="fixture-card__facts">
              <span><small>比赛时间</small><b>{{$formatDateTime(match.matchDateTime)}}</b></span>
              <span><small>比赛场馆</small><b>{{match.stadiumName||'待确认'}}</b></span>
              <span><small>剩余票数</small><b class="tabular-nums">{{match.remainingTickets??'—'}}</b></span>
            </div>
            <div class="fixture-card__footer">
              <div><StatusTag :value="match.saleStatus"/><span v-if="match.saleStatus==='NOT_STARTED'">{{$formatDateTime(match.saleStartTime)}} 开售</span></div>
              <RouterLink :to="`/user/matches/${match.matchId}`" class="el-button el-button--primary">{{match.purchasable?'查看比赛并购票':'查看比赛'}}</RouterLink>
            </div>
          </article>
        </div>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.season-summary{display:grid;grid-template-columns:repeat(3,150px) 1fr;gap:var(--space-md);padding:22px 24px;border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface)}
.season-summary div{display:flex;flex-direction:column;gap:6px}.season-summary span,.team-link small,.fixture-card__facts small{color:var(--text-muted);font-size:12px}.season-summary strong{font-size:26px}.season-summary strong small{margin-left:4px;color:var(--text-secondary);font-size:13px}.season-summary__dates{padding-left:var(--space-lg);border-left:1px solid var(--border-color)}.season-summary__dates strong{font-size:16px}
.round-section{margin-top:var(--space-xl)}.round-heading{display:grid;grid-template-columns:auto auto 1fr;align-items:center;gap:12px;margin-bottom:var(--space-md)}.round-heading>span{color:var(--primary);font-size:11px;font-weight:800;letter-spacing:.12em}.round-heading h2{margin:0;font-size:20px}.round-heading div{height:1px;background:var(--border-color)}
.fixture-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-md)}.fixture-card{padding:22px;border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface);box-shadow:var(--shadow-sm)}
.fixture-card__teams{display:grid;grid-template-columns:1fr 90px 1fr;align-items:center;gap:12px}.team-link{display:flex;align-items:center;flex-direction:column;gap:7px;min-width:0;text-align:center}.team-link strong{max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.team-link:hover strong{color:var(--primary)}.versus{display:flex;align-items:center;flex-direction:column;gap:10px}.versus>strong{font-size:23px}
.fixture-card__facts{display:grid;grid-template-columns:1.2fr 1fr .7fr;gap:12px;margin:20px 0;padding:16px 0;border-top:1px solid var(--border-color);border-bottom:1px solid var(--border-color)}.fixture-card__facts span{display:flex;flex-direction:column;gap:5px}.fixture-card__facts b{overflow:hidden;font-size:13px;text-overflow:ellipsis;white-space:nowrap}
.fixture-card__footer{display:flex;align-items:center;justify-content:space-between;gap:var(--space-md)}.fixture-card__footer>div{display:flex;align-items:center;gap:10px;color:var(--text-muted);font-size:12px}
</style>
