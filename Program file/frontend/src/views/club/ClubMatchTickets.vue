<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {useAuthStore} from '../../stores/auth'
import {getMatch} from '../../api/match'
import TicketZoneList from '../../components/TicketZoneList.vue'

const route=useRoute(),auth=useAuthStore(),match=ref({}),loading=ref(false),error=ref('')
const isHome=computed(()=>String(match.value.homeClubId)===String(auth.user?.clubId))
const load=async()=>{loading.value=true;error.value='';try{match.value=(await getMatch(route.params.id)).data}catch(e){error.value=e?.message||'加载比赛票务失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="比赛票务" subtitle="查看比赛与票区信息，不提供售票配置能力。" :breadcrumb="[{label:'比赛票务',to:'/club/matches'},{label:'票务详情'}]">
      <template #status><StatusTag :value="match.matchStatus"/></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!match.matchId" empty-title="比赛不存在" empty-description="该比赛可能已被移除或当前账号无权查看。" @retry="load">
      <CardShell variant="fixture" title="比赛信息" :subtitle="isHome?'本俱乐部主场':'本俱乐部客场'">
        <div class="match-board"><div><span>主队</span><strong>{{match.homeClubName}}</strong></div><div class="match-versus"><strong v-if="match.homeScore!=null" class="score-nums">{{match.homeScore}} : {{match.awayScore}}</strong><strong v-else>VS</strong><span>{{$formatDateTime(match.matchTime)}}</span></div><div><span>客队</span><strong>{{match.awayClubName}}</strong></div></div>
        <div class="match-meta"><span>{{match.stadiumName}}</span><StatusTag :value="match.matchStatus"/></div>
      </CardShell>
      <TicketZoneList v-if="isHome" :match-id="route.params.id" />
      <EmptyState v-else class="away-state" title="客场票务由主队运营" description="本页不展示无关的主场库存管理信息。" />
    </DataState>
  </div>
</template>

<style scoped>.match-board{display:grid;grid-template-columns:1fr 180px 1fr;align-items:center;gap:var(--space-5);text-align:center}.match-board>div{display:grid;gap:var(--space-2)}.match-board span,.match-versus span,.match-meta{color:var(--color-text-muted);font-size:var(--font-size-sm)}.match-board strong{font-size:var(--font-size-xl)}.match-versus strong{font-size:var(--font-size-3xl)}.match-meta{display:flex;align-items:center;justify-content:center;gap:var(--space-3);margin-top:var(--space-5);padding-top:var(--space-4);border-top:1px solid var(--color-line)}.away-state{margin-top:var(--space-4)}@media(max-width:700px){.match-board{grid-template-columns:1fr 90px 1fr}.match-board strong{font-size:var(--font-size-md)}}
</style>
