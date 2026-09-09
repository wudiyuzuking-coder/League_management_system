<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useAuthStore} from '../../stores/auth'
import {useRouter} from 'vue-router'
import {getMatches} from '../../api/match'
const auth=useAuthStore(),router=useRouter(),rows=ref([]),total=ref(0),loading=ref(false),query=reactive({page:1,size:10,clubId:auth.user?.clubId})
const load=async()=>{loading.value=true;try{const r=(await getMatches(query)).data;rows.value=r.records;total.value=r.total}finally{loading.value=false}}
onMounted(load)
</script>
<template><el-card><template #header><h2>本俱乐部比赛</h2></template><el-alert title="本页面仅展示当前登录俱乐部作为主队或客队参与的比赛；票务信息只读。" type="info" :closable="false"/><el-table v-loading="loading" :data="rows"><el-table-column prop="seasonName" label="赛季"/><el-table-column prop="roundName" label="轮次"/><el-table-column label="对阵" min-width="220"><template #default="{row}">{{row.homeClubName}} vs {{row.awayClubName}}</template></el-table-column><el-table-column prop="matchTime" label="时间"/><el-table-column prop="stadiumName" label="场馆"/><el-table-column label="比分"><template #default="{row}">{{row.homeScore==null?'—':`${row.homeScore}:${row.awayScore}`}}</template></el-table-column><el-table-column prop="matchStatus" label="状态"/><el-table-column label="操作"><template #default="{row}"><el-button link type="primary" @click="router.push(`/club/matches/${row.matchId}/tickets`)">票务详情</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="query.page" :total="total" layout="total,prev,pager,next" @current-change="load"/></el-card></template>
<style scoped>h2{margin:0}.el-alert{margin-bottom:16px}.el-pagination{justify-content:flex-end;margin-top:16px}</style>
