<script setup>
import {onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {confirmSchedule,getSchedule,getSchedules} from '../../api/league'
import {confirmAction} from '../../utils/confirmAction'

const rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),visible=ref(false),detail=ref({}),confirming=ref(false)
const query=reactive({seasonId:null,batchStatus:'',page:1,size:20})
const load=async()=>{loading.value=true;error.value='';try{const result=(await getSchedules(query)).data;rows.value=result.records;total.value=result.total}catch(e){error.value=e?.message||'加载赛程失败，请稍后重试。'}finally{loading.value=false}}
const show=async row=>{detail.value=(await getSchedule(row.seasonId)).data;visible.value=true}
const confirm=async()=>{await confirmAction({title:'确认正式赛程',message:`确定确认“${detail.value.seasonName}”的当前赛程吗？`,impact:'确认后赛程将进入正式业务流程，请先核对球队、轮次、比赛时间与场馆。',confirmButtonText:'确认赛程'});confirming.value=true;try{detail.value=(await confirmSchedule(detail.value.seasonId)).data;ElMessage.success('赛程已确认');await load()}finally{confirming.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="赛程管理" subtitle="查询自动生成的赛程批次并完成正式确认。"><template #actions><el-button @click="load">刷新</el-button></template></PageHeader>
    <section class="app-surface">
      <FilterBar :model="query"><el-form-item label="赛季 ID"><el-input-number v-model="query.seasonId" :min="1"/></el-form-item><el-form-item label="状态"><el-select v-model="query.batchStatus" clearable><el-option label="待确认" value="GENERATED"/><el-option label="已确认" value="CONFIRMED"/></el-select></el-form-item><template #actions><el-button type="primary" @click="query.page=1;load()">查询</el-button></template></FilterBar>
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无赛程批次" empty-description="满足排赛条件后，自动生成的赛程会显示在这里。" @retry="load">
        <el-table :data="rows"><el-table-column prop="seasonName" label="赛季" min-width="170"/><el-table-column prop="clubCount" label="球队数" align="right"/><el-table-column prop="roundCount" label="轮次" align="right"/><el-table-column prop="matchCount" label="比赛数" align="right"/><el-table-column prop="triggerType" label="生成方式"/><el-table-column label="生成时间" min-width="170"><template #default="{row}">{{$formatDateTime(row.generatedAt)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.batchStatus"/></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button link type="primary" @click="show(row)">查看赛程</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="query.page" :total="total" layout="total, prev, pager, next" @current-change="load"/>
      </DataState>
    </section>
    <el-dialog v-model="visible" class="app-dialog schedule-dialog" title="赛程详情" width="900px"><div class="schedule-summary"><div><span>赛季</span><b>{{detail.seasonName}}</b></div><div><span>球队</span><b>{{detail.clubCount}}</b></div><div><span>轮次</span><b>{{detail.roundCount}}</b></div><div><span>比赛</span><b>{{detail.matchCount}}</b></div><StatusTag :value="detail.batchStatus"/></div><section v-for="round in detail.rounds||[]" :key="round.roundNo" class="round"><h2>第 {{round.roundNo}} 轮</h2><el-table :data="round.matches"><el-table-column label="时间" min-width="170"><template #default="{row}">{{$formatDateTime(row.matchDateTime)}}</template></el-table-column><el-table-column prop="homeClubName" label="主队"/><el-table-column prop="awayClubName" label="客队"/><el-table-column prop="stadiumName" label="主场"/><el-table-column label="比赛状态"><template #default="{row}"><StatusTag :value="row.matchStatus"/></template></el-table-column></el-table></section><template #footer><el-button @click="visible=false">关闭</el-button><el-button v-if="detail.batchStatus==='GENERATED'" type="primary" :loading="confirming" @click="confirm">确认赛程</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.schedule-summary{display:grid;grid-template-columns:2fr repeat(3,1fr) auto;align-items:center;gap:var(--space-md);padding:var(--space-md);border-radius:var(--radius-md);background:var(--surface-muted)}.schedule-summary div{display:flex;flex-direction:column;gap:4px}.schedule-summary span{color:var(--text-muted);font-size:12px}.round{margin-top:var(--space-lg)}.round h2{margin:0 0 var(--space-sm);font-size:17px}</style>
