<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getSeason,getSeasonSchedule} from '../../api/league'

const route=useRoute(),season=ref({}),loading=ref(false),error=ref('')
const rounds=computed(()=>season.value.rounds||[])
const matchCount=computed(()=>rounds.value.reduce((sum,round)=>sum+(round.matches?.length||0),0))
const summary=computed(()=>[
  {label:'参赛球队',value:season.value.teamCount??season.value.clubCount??0,meta:'支球队'},
  {label:'比赛轮次',value:rounds.value.length,meta:'轮'},
  {label:'比赛总数',value:matchCount.value,meta:'场'},
  {label:'赛季状态',status:season.value.publicStatus},
])
const load=async()=>{loading.value=true;error.value='';try{const summary=(await getSeason(route.params.id)).data;season.value=summary;if(!summary.scheduleConfirmed)return;season.value={...summary,...(await getSeasonSchedule(route.params.id)).data}}catch(e){error.value=e?.message||'加载赛程失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div class="season-detail">
    <PageHeader back back-fallback="/user/seasons" class="schedule-page-header" :title="season.seasonName||'赛季详情'" subtitle="按轮次查看完整赛程、比赛状态与售票信息。" :breadcrumb="[{label:'联赛赛季',to:'/user/seasons'},{label:'完整赛程'}]">
      <template #status><StatusTag v-if="season.publicStatus" :value="season.publicStatus"/></template>
      <template #actions><RouterLink :to="`/user/seasons/${route.params.id}/standings`" class="el-button">查看积分榜</RouterLink></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!season.scheduleConfirmed||!rounds.length" :empty-title="season.scheduleConfirmed?'暂无可展示比赛':'当前赛季正在报名阶段'" :empty-description="season.scheduleConfirmed?'正式赛程中尚无可展示的比赛。':'完整赛程将在报名结束后公布。'" @retry="load">
      <MetricStrip :items="summary" label="赛季摘要" />
      <p class="season-dates">赛季周期 {{$formatDate(season.startDate)}} — {{$formatDate(season.endDate)}}</p>
      <section v-for="round in rounds" :key="round.roundNo" class="round-section">
        <div class="round-heading"><h2>第 {{round.roundNo}} 轮</h2><span>{{round.matches?.length||0}} 场比赛</span><div aria-hidden="true"/></div>
        <div class="fixture-list">
          <CardShell v-for="match in round.matches" :key="match.matchId" class="fixture-card" variant="fixture" compact>
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
              <span><small>比赛场馆</small><b>{{match.stadiumName||'场馆待确认'}}</b></span>
              <span><small>剩余票数</small><b class="tabular-nums">{{match.remainingTickets??0}}</b></span>
            </div>
            <div class="fixture-card__footer">
              <div><StatusTag :value="match.saleStatus"/><span v-if="match.saleStatus==='NOT_STARTED'">{{$formatDateTime(match.saleStartTime)}} 开售</span></div>
              <RouterLink :to="`/user/matches/${match.matchId}`" class="el-button el-button--primary">{{match.purchasable?'查看比赛并购票':'查看比赛'}}</RouterLink>
            </div>
          </CardShell>
        </div>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.season-dates{margin:var(--space-3) 0 0;color:var(--color-text-muted);font-size:var(--font-size-sm)}
.schedule-page-header :deep(.page-header__breadcrumb a){display:inline-flex;align-items:center;min-height:36px;margin:calc(var(--space-2) * -1);padding:var(--space-2) var(--space-3);border-radius:var(--radius-sm);font-size:var(--font-size-md);font-weight:var(--font-weight-semibold)}
.schedule-page-header :deep(.page-header__breadcrumb a:hover){background:var(--color-surface-subtle);color:var(--primary);text-decoration:underline;text-underline-offset:3px}
.schedule-page-header :deep(.page-header__breadcrumb a:focus-visible){background:var(--color-surface-subtle);box-shadow:var(--focus-ring)}
.round-section{margin-top:var(--space-xl)}.round-heading{display:grid;grid-template-columns:auto auto 1fr;align-items:center;gap:12px;margin-bottom:var(--space-md)}.round-heading>span{color:var(--color-text-muted);font-size:var(--font-size-sm)}.round-heading h2{margin:0;font-size:20px}.round-heading div{height:1px;background:var(--border-color)}
.fixture-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-md)}.fixture-card :deep(.card-shell__body){padding:var(--space-5)}
.fixture-card__teams{display:grid;grid-template-columns:1fr 90px 1fr;align-items:center;gap:12px}.team-link{display:flex;align-items:center;flex-direction:column;gap:7px;min-width:0;text-align:center}.team-link strong{max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.team-link:hover strong{color:var(--primary)}.versus{display:flex;align-items:center;flex-direction:column;gap:10px}.versus>strong{font-size:23px}
.fixture-card__facts{display:grid;grid-template-columns:1.2fr 1fr .7fr;gap:12px;margin:20px 0;padding:16px 0;border-top:1px solid var(--border-color);border-bottom:1px solid var(--border-color)}.fixture-card__facts span{display:flex;flex-direction:column;gap:5px}.fixture-card__facts b{overflow:hidden;font-size:13px;text-overflow:ellipsis;white-space:nowrap}
.fixture-card__footer{display:flex;align-items:center;justify-content:space-between;gap:var(--space-md)}.fixture-card__footer>div{display:flex;align-items:center;gap:10px;color:var(--text-muted);font-size:12px}
@media(max-width:900px){.fixture-list{grid-template-columns:1fr}}
</style>
