<script setup>
import {onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getAdminClub} from '../../api/club'
import {accountStatusLabel,clubStatusLabel,statusType} from '../../constants/status'

const route=useRoute(),router=useRouter(),club=ref({}),loading=ref(false)
const load=async()=>{loading.value=true;try{club.value=(await getAdminClub(route.params.id)).data}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header><el-page-header @back="router.push('/admin/clubs')"><template #content>{{club.clubName||'俱乐部详情'}}</template></el-page-header></template>
    <el-descriptions :column="3" border>
      <el-descriptions-item label="简称">{{club.shortName||'—'}}</el-descriptions-item>
      <el-descriptions-item label="城市">{{club.homeCity||'—'}}</el-descriptions-item>
      <el-descriptions-item label="俱乐部状态"><el-tag :type="statusType(club.clubStatus)">{{clubStatusLabel(club.clubStatus)}}</el-tag></el-descriptions-item>
      <el-descriptions-item label="负责人">{{club.leaderName||'未绑定'}}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{club.leaderPhone||'—'}}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{club.leaderNickname||'—'}}</el-descriptions-item>
      <el-descriptions-item label="负责人账号状态"><el-tag v-if="club.leaderStatus" :type="statusType(club.leaderStatus)">{{accountStatusLabel(club.leaderStatus)}}</el-tag><span v-else>—</span></el-descriptions-item>
      <el-descriptions-item label="简介" :span="3">{{club.description||'—'}}</el-descriptions-item>
    </el-descriptions>
    <el-alert class="notice" type="info" :closable="false" title="球员、教练与主场资料由俱乐部负责人自行维护。"/>
  </el-card>
</template>

<style scoped>.notice{margin-top:16px}</style>
