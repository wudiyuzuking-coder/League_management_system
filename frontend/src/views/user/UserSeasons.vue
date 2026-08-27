<script setup>
import {onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {getSeasons} from '../../api/league'
const router=useRouter(),rows=ref([]),loading=ref(false)
onMounted(async()=>{loading.value=true;try{rows.value=(await getSeasons()).data}finally{loading.value=false}})
</script>
<template><el-card><template #header><h2>联赛赛季</h2></template><el-row v-loading="loading" :gutter="18"><el-col v-for="s in rows" :key="s.seasonId" :md="12"><el-card shadow="hover" class="season"><div class="title"><strong>{{s.seasonName}}</strong><StatusTag :value="s.seasonStatus"/></div><p>{{$formatDate(s.startDate)}} 至 {{$formatDate(s.endDate)}}</p><p>{{s.description||'暂无说明'}}</p><el-button type="primary" @click="router.push(`/user/seasons/${s.seasonId}/rounds`)">查看轮次</el-button><el-button @click="router.push(`/user/seasons/${s.seasonId}/standings`)">查看积分榜</el-button></el-card></el-col></el-row><el-empty v-if="!loading&&!rows.length" description="暂无赛季"/></el-card></template>
<style scoped>h2{margin:0}.season{margin-bottom:18px}.title{display:flex;justify-content:space-between;font-size:18px}</style>
