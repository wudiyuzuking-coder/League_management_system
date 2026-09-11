<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getMatch} from '../../api/match'
import {getStadium} from '../../api/stadium'
import {getTicketZones} from '../../api/ticket'
import TicketZoneList from '../../components/TicketZoneList.vue'
const route=useRoute(),router=useRouter(),match=ref({}),stadium=ref({}),ticketZones=ref([]),loading=ref(false)
const remaining=computed(()=>ticketZones.value.reduce((sum,z)=>sum+Number(z.availableSeatCount||0),0))
onMounted(async()=>{loading.value=true;try{match.value=(await getMatch(route.params.id)).data;const tasks=[getTicketZones(route.params.id)];if(match.value.stadiumId)tasks.push(getStadium(match.value.stadiumId));const values=await Promise.all(tasks);ticketZones.value=values[0].data;if(values[1])stadium.value=values[1].data}finally{loading.value=false}})
</script>
<template><el-card v-loading="loading"><template #header><el-page-header @back="router.push('/user/matches')"><template #content>比赛详情</template></el-page-header></template><div class="fixture"><button class="club-link" @click="router.push(`/user/clubs/${match.homeClubId}`)"><el-avatar :size="72" :src="match.homeLogoUrl">{{match.homeClubName?.[0]}}</el-avatar><h2>{{match.homeClubName}}</h2></button><strong>{{match.homeScore==null?'VS':`${match.homeScore} : ${match.awayScore}`}}</strong><button class="club-link" @click="router.push(`/user/clubs/${match.awayClubId}`)"><el-avatar :size="72" :src="match.awayLogoUrl">{{match.awayClubName?.[0]}}</el-avatar><h2>{{match.awayClubName}}</h2></button></div><el-descriptions :column="2" border><el-descriptions-item label="赛季">{{match.seasonName}}</el-descriptions-item><el-descriptions-item label="轮次">{{match.roundName}}</el-descriptions-item><el-descriptions-item label="比赛时间">{{$formatDateTime(match.matchTime)}}</el-descriptions-item><el-descriptions-item label="剩余票数">{{remaining}}</el-descriptions-item><el-descriptions-item label="场馆地址" :span="2">{{stadium.address||'—'}} {{match.stadiumName||''}}</el-descriptions-item></el-descriptions><TicketZoneList :match-id="route.params.id"/></el-card></template>
<style scoped>.fixture{display:grid;grid-template-columns:1fr 120px 1fr;align-items:center;text-align:center;margin:24px}.fixture strong{font-size:30px}.fixture h2{font-size:18px;margin:8px 0}.club-link{border:0;background:transparent;cursor:pointer;color:inherit}.club-link:hover h2{color:#409eff}</style>
