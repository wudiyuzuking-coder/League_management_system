<script setup>
import {onMounted,reactive,ref,watch} from 'vue'
import {useRouter} from 'vue-router'
import {getResultReminders} from '../../api/match'
import {getSeasons} from '../../api/league'
import {useSystemTimeStore} from '../../stores/systemTime'

const router=useRouter(),systemTimeStore=useSystemTimeStore(),rows=ref([]),total=ref(0),seasons=ref([]),loading=ref(false)
const query=reactive({page:1,size:10,seasonId:null,reminderType:''})
const load=async()=>{loading.value=true;try{const data=(await getResultReminders(query)).data;rows.value=data.records;total.value=data.total}finally{loading.value=false}}
const search=()=>{query.page=1;load()}
watch(()=>systemTimeStore.revision,load)
onMounted(async()=>{seasons.value=(await getSeasons()).data;await load()})
</script>

<template>
  <el-card>
    <template #header><div class="head"><div><h2>赛果待维护</h2><p>列表按统一系统日期动态计算，不产生提醒记录。</p></div><el-tag type="warning">共 {{total}} 场</el-tag></div></template>
    <el-form inline>
      <el-form-item label="赛季"><el-select v-model="query.seasonId" clearable style="width:190px"><el-option v-for="s in seasons" :key="s.seasonId" :label="s.seasonName" :value="s.seasonId"/></el-select></el-form-item>
      <el-form-item label="提醒"><el-select v-model="query.reminderType" clearable style="width:150px"><el-option label="今天比赛" value="TODAY"/><el-option label="已逾期" value="OVERDUE"/></el-select></el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
    </el-form>
    <el-table :data="rows" v-loading="loading" empty-text="当前没有待维护赛果">
      <el-table-column label="提醒" width="120"><template #default="{row}"><el-tag :type="row.reminderType==='OVERDUE'?'danger':'warning'">{{row.reminderType==='TODAY'?'今天比赛':`逾期${row.daysOverdue}天`}}</el-tag></template></el-table-column>
      <el-table-column prop="matchTime" label="比赛时间" min-width="175"/>
      <el-table-column label="对阵" min-width="230"><template #default="{row}">{{row.homeClubName}} vs {{row.awayClubName}}</template></el-table-column>
      <el-table-column prop="seasonName" label="赛季" min-width="140"/>
      <el-table-column prop="roundName" label="轮次" min-width="110"/>
      <el-table-column label="状态" width="130"><template #default="{row}"><el-tag>{{row.matchStatus}}</el-tag></template></el-table-column>
      <el-table-column label="维护提示" min-width="220"><template #default="{row}">{{row.matchStatus==='PUBLISHED'?'请先将比赛状态更新为进行中':'比赛已开始，请及时录入赛果'}}</template></el-table-column>
      <el-table-column label="操作" width="110"><template #default="{row}"><el-button link type="primary" @click="router.push(`/admin/matches/${row.matchId}`)">{{row.matchStatus==='PUBLISHED'?'维护状态':'录入比分'}}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-if="total>query.size" v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load"/>
  </el-card>
</template>

<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.head h2{margin:0 0 6px}.head p{margin:0;color:#6b7280}.el-pagination{justify-content:flex-end;margin-top:16px}</style>
