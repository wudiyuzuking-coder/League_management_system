<script setup>
import {onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getSeasonSchedule} from '../../api/league'
const route=useRoute(),router=useRouter(),season=ref({}),loading=ref(false)
const matchStatus=value=>({PUBLISHED:'未开始',IN_PROGRESS:'进行中',FINISHED:'已结束'}[value]||value)
const matchType=value=>({PUBLISHED:'info',IN_PROGRESS:'warning',FINISHED:'success'}[value]||'info')
const saleStatus=value=>({NOT_STARTED:'未开售',ON_SALE:'售票中',ENDED:'已停售',SOLD_OUT:'已售罄',NOT_ENABLED:'票务未启用',MATCH_UNAVAILABLE:'不可购票'}[value]||value)
const saleType=value=>({ON_SALE:'success',NOT_STARTED:'warning',ENDED:'info',SOLD_OUT:'danger'}[value]||'info')
onMounted(async()=>{loading.value=true;try{season.value=(await getSeasonSchedule(route.params.id)).data}finally{loading.value=false}})
</script>
<template><el-card v-loading="loading"><template #header><div class="head"><el-page-header @back="router.push('/user/seasons')"><template #content>{{season.seasonName}} · 完整赛程</template></el-page-header><el-button @click="router.push(`/user/seasons/${route.params.id}/standings`)">积分榜</el-button></div></template>
  <el-descriptions :column="4" border class="summary"><el-descriptions-item label="赛季名称">{{season.seasonName||'—'}}</el-descriptions-item><el-descriptions-item label="开始时间">{{$formatDate(season.startDate)}}</el-descriptions-item><el-descriptions-item label="预计结束时间">{{$formatDate(season.endDate)}}</el-descriptions-item><el-descriptions-item label="参赛俱乐部">{{season.clubCount??'—'}} 支</el-descriptions-item></el-descriptions>
  <el-empty v-if="!loading&&!season.rounds?.length" description="赛程尚未正式确认"/>
  <section v-for="round in season.rounds||[]" :key="round.roundNo" class="round"><h3>第{{round.roundNo}}轮</h3>
    <el-card v-for="match in round.matches" :key="match.matchId" shadow="hover" class="match" @click="router.push(`/user/matches/${match.matchId}`)">
      <div class="fixture"><button @click.stop="router.push(`/user/clubs/${match.homeClubId}`)"><el-avatar :size="42" :src="match.homeLogoUrl">{{match.homeClubName?.[0]}}</el-avatar><span>{{match.homeClubName}}</span></button><b>VS</b><button @click.stop="router.push(`/user/clubs/${match.awayClubId}`)"><el-avatar :size="42" :src="match.awayLogoUrl">{{match.awayClubName?.[0]}}</el-avatar><span>{{match.awayClubName}}</span></button></div>
      <div class="facts"><span>{{$formatDateTime(match.matchDateTime)}}</span><span>{{match.stadiumAddress}} {{match.stadiumName}}</span><span>余票 {{match.remainingTickets}}</span></div>
      <div class="states"><el-tag :type="matchType(match.matchStatus)">比赛状态：{{matchStatus(match.matchStatus)}}</el-tag><el-tag :type="saleType(match.saleStatus)">售票状态：{{saleStatus(match.saleStatus)}}</el-tag><span v-if="match.saleStatus==='NOT_STARTED'">本场比赛将于 {{$formatDateTime(match.saleStartTime)}} 开售</span></div>
      <div class="actions"><el-button @click.stop="router.push(`/user/matches/${match.matchId}`)">比赛详情</el-button><el-button type="primary" :disabled="!match.purchasable" @click.stop="router.push(`/user/matches/${match.matchId}`)">购票</el-button></div>
    </el-card>
  </section>
</el-card></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.summary{margin:20px 0}.round{margin-top:24px}.round h3{border-left:4px solid #409eff;padding-left:10px}.match{margin-bottom:14px;cursor:pointer}.match :deep(.el-card__body){display:grid;grid-template-columns:minmax(280px,1.2fr) minmax(220px,1fr) minmax(220px,1fr) auto;gap:18px;align-items:center}.fixture{display:grid;grid-template-columns:1fr auto 1fr;align-items:center;gap:12px}.fixture button{display:flex;align-items:center;gap:8px;border:0;background:none;cursor:pointer;color:inherit}.facts,.states{display:flex;flex-direction:column;gap:8px}.actions{display:flex;gap:8px}@media(max-width:1000px){.match :deep(.el-card__body){grid-template-columns:1fr}.actions{justify-content:flex-end}}</style>
