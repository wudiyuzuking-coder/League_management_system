<script setup>
import {onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getMatch} from '../../api/match'
import TicketZoneList from '../../components/TicketZoneList.vue'
const route=useRoute(),router=useRouter(),match=ref({}),loading=ref(false)
onMounted(async()=>{loading.value=true;try{match.value=(await getMatch(route.params.id)).data}finally{loading.value=false}})
</script>
<template><el-card v-loading="loading"><template #header><el-page-header @back="router.push('/user/matches')"><template #content>比赛详情</template></el-page-header></template><div class="fixture"><div><el-avatar :size="72" :src="match.homeLogoUrl">{{match.homeClubName?.[0]}}</el-avatar><h2>{{match.homeClubName}}</h2></div><strong>{{match.homeScore==null?'VS':`${match.homeScore} : ${match.awayScore}`}}</strong><div><el-avatar :size="72" :src="match.awayLogoUrl">{{match.awayClubName?.[0]}}</el-avatar><h2>{{match.awayClubName}}</h2></div></div><el-descriptions :column="2" border><el-descriptions-item label="赛季">{{match.seasonName}}</el-descriptions-item><el-descriptions-item label="轮次">{{match.roundName}}</el-descriptions-item><el-descriptions-item label="时间">{{$formatDateTime(match.matchTime)}}</el-descriptions-item><el-descriptions-item label="场馆">{{match.stadiumName}}</el-descriptions-item><el-descriptions-item label="状态"><StatusTag :value="match.matchStatus"/></el-descriptions-item><el-descriptions-item label="发布时间">{{$formatDateTime(match.publishedAt)}}</el-descriptions-item></el-descriptions><TicketZoneList :match-id="route.params.id"/></el-card></template>
<style scoped>.fixture{display:grid;grid-template-columns:1fr 120px 1fr;align-items:center;text-align:center;margin:24px}.fixture strong{font-size:30px}.fixture h2{font-size:18px}</style>
