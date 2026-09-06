<script setup>
import {ref,onMounted} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getMatch} from '../../api/match'
import TicketZoneList from '../../components/TicketZoneList.vue'
const route=useRoute(),router=useRouter(),match=ref({})
onMounted(async()=>{match.value=(await getMatch(route.params.id)).data})
</script>
<template><el-card><template #header><el-page-header @back="router.push('/club/matches')"><template #content>主场比赛票务（只读）</template></el-page-header></template><el-descriptions :column="2" border><el-descriptions-item label="对阵">{{match.homeClubName}} vs {{match.awayClubName}}</el-descriptions-item><el-descriptions-item label="比赛时间">{{$formatDateTime(match.matchTime)}}</el-descriptions-item><el-descriptions-item label="场馆">{{match.stadiumName}}</el-descriptions-item><el-descriptions-item label="比赛状态"><StatusTag :value="match.matchStatus"/></el-descriptions-item></el-descriptions><TicketZoneList :match-id="route.params.id"/></el-card></template>
