<script setup>
import {onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getSeason,getStandings} from '../../api/league'
const route=useRoute(),router=useRouter(),season=ref({}),rows=ref([]),loading=ref(false)
onMounted(async()=>{loading.value=true;try{const [s,r]=await Promise.all([getSeason(route.params.id),getStandings(route.params.id)]);season.value=s.data;rows.value=r.data}finally{loading.value=false}})
</script>
<template><el-card v-loading="loading"><template #header><el-page-header @back="router.push('/user/seasons')"><template #content>{{season.seasonName}} · 积分榜</template></el-page-header></template><el-table :data="rows"><el-table-column prop="rank" label="排名" width="70"/><el-table-column label="俱乐部" min-width="180"><template #default="{row}"><span class="club"><el-avatar :size="28" :src="row.logoUrl">{{row.clubName?.[0]}}</el-avatar>{{row.clubName}}</span></template></el-table-column><el-table-column prop="matchesPlayed" label="场"/><el-table-column prop="wins" label="胜"/><el-table-column prop="draws" label="平"/><el-table-column prop="losses" label="负"/><el-table-column prop="goalsFor" label="进球"/><el-table-column prop="goalsAgainst" label="失球"/><el-table-column prop="goalDifference" label="净胜球"/><el-table-column prop="points" label="积分"/></el-table></el-card></template>
<style scoped>.club{display:flex;align-items:center;gap:8px}</style>
