<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getUserClubDetail} from '../../api/club'

const route=useRoute(),router=useRouter(),data=ref(null),loading=ref(false)
const positions={GOALKEEPER:'守门员',DEFENDER:'后卫',MIDFIELDER:'中场',FORWARD:'前锋'}
const coachTitles={HEAD_COACH:'主教练',ASSISTANT_COACH:'助理教练',GOALKEEPER_COACH:'守门员教练',FITNESS_COACH:'体能教练'}
const club=computed(()=>data.value?.club||{})
const load=async()=>{loading.value=true;try{data.value=(await getUserClubDetail(route.params.clubId)).data}finally{loading.value=false}}
const fixture=m=>`${m.homeClubName} vs ${m.awayClubName}`
const score=m=>m.homeScore==null?'—':`${m.homeScore} : ${m.awayScore}`
onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header><el-page-header @back="router.back()"><template #content>俱乐部详情</template></el-page-header></template>
    <template v-if="data">
      <section class="hero">
        <el-avatar :size="86" :src="club.logoUrl">{{club.clubName?.[0]}}</el-avatar>
        <div><h2>{{club.clubName}}</h2><p>{{club.description||'暂无俱乐部简介'}}</p></div>
      </section>

      <h3>基础信息</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="简称">{{club.shortName||'—'}}</el-descriptions-item>
        <el-descriptions-item label="所在城市">{{club.homeCity||'—'}}</el-descriptions-item>
        <el-descriptions-item label="主场">{{club.homeStadium?.stadiumName||'暂未配置'}}</el-descriptions-item>
        <el-descriptions-item label="场馆容量">{{club.homeStadium?.capacity??'—'}}</el-descriptions-item>
        <el-descriptions-item label="场馆地址" :span="2">{{club.homeStadium?.address||'—'}}</el-descriptions-item>
      </el-descriptions>

      <h3>当前球员</h3>
      <el-table :data="data.players" empty-text="暂无ACTIVE球员">
        <el-table-column prop="number" label="号码" width="90"><template #default="{row}">{{row.number??'—'}}</template></el-table-column>
        <el-table-column prop="name" label="姓名"/>
        <el-table-column label="位置"><template #default="{row}">{{positions[row.position]||row.position}}</template></el-table-column>
        <el-table-column prop="age" label="年龄"><template #default="{row}">{{row.age==null?'—':`${row.age}岁`}}</template></el-table-column>
        <el-table-column prop="nationality" label="国籍"/>
      </el-table>

      <h3>教练团队</h3>
      <el-table :data="data.coaches" empty-text="暂无ACTIVE教练">
        <el-table-column prop="name" label="姓名"/><el-table-column label="职务"><template #default="{row}">{{coachTitles[row.title]||row.title}}</template></el-table-column><el-table-column prop="nationality" label="国籍"/>
      </el-table>

      <h3>当前 / 最近赛季战绩</h3>
      <el-descriptions v-if="data.standing" :column="4" border>
        <el-descriptions-item label="赛季">{{data.standing.seasonName}}</el-descriptions-item>
        <el-descriptions-item label="排名">第{{data.standing.rank}}名</el-descriptions-item>
        <el-descriptions-item label="积分">{{data.standing.points}}</el-descriptions-item>
        <el-descriptions-item label="场次">{{data.standing.matchesPlayed}}</el-descriptions-item>
        <el-descriptions-item label="胜/平/负">{{data.standing.wins}} / {{data.standing.draws}} / {{data.standing.losses}}</el-descriptions-item>
        <el-descriptions-item label="进/失球">{{data.standing.goalsFor}} / {{data.standing.goalsAgainst}}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无赛季战绩"/>

      <h3>最近结束比赛</h3>
      <el-table :data="data.recentMatches" empty-text="暂无已结束公开比赛">
        <el-table-column prop="matchTime" label="日期" min-width="170"/><el-table-column label="对阵" min-width="220"><template #default="{row}">{{fixture(row)}}</template></el-table-column><el-table-column label="比分"><template #default="{row}">{{score(row)}}</template></el-table-column><el-table-column prop="stadiumName" label="场馆"/>
      </el-table>

      <div class="next-title"><h3>接下来比赛</h3><el-tag v-if="data.nextMatch" type="success">最近比赛还有 {{data.daysUntilNextMatch}} 天</el-tag></div>
      <el-table :data="data.upcomingMatches" empty-text="暂无未来公开比赛">
        <el-table-column prop="matchTime" label="时间" min-width="170"/><el-table-column label="对阵" min-width="220"><template #default="{row}">{{fixture(row)}}</template></el-table-column><el-table-column prop="stadiumName" label="场馆"/><el-table-column label="操作"><template #default="{row}"><el-button link type="primary" @click="router.push(`/user/matches/${row.matchId}`)">比赛详情</el-button></template></el-table-column>
      </el-table>
    </template>
  </el-card>
</template>

<style scoped>.hero{display:flex;align-items:center;gap:18px;margin-bottom:24px}.hero h2{margin:0 0 8px}.hero p{margin:0;color:#606266}h3{margin:26px 0 12px}.next-title{display:flex;align-items:center;gap:14px}.next-title h3{margin-right:4px}</style>
