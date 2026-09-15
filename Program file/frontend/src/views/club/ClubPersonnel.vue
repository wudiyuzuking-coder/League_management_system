<script setup>
import {computed,onMounted,ref} from 'vue'
import {getClubPersonnelOverview} from '../../api/club'

const loading=ref(false),error=ref(''),overview=ref({players:[],coaches:[]})
const starters=computed(()=>overview.value.players?.filter(p=>p.status==='在队'&&p.lineupRole==='STARTER')||[])
const substitutes=computed(()=>overview.value.players?.filter(p=>p.status==='在队'&&p.lineupRole==='SUBSTITUTE')||[])
const activeCoaches=computed(()=>overview.value.coaches?.filter(c=>c.status==='现役')||[])
const positionLabel={GOALKEEPER:'门将',DEFENDER:'后卫',MIDFIELDER:'中场',FORWARD:'前锋'}
const titleLabel={HEAD_COACH:'主教练',ASSISTANT_COACH:'副教练'}
const load=async()=>{loading.value=true;error.value='';try{overview.value=(await getClubPersonnelOverview()).data}catch(e){error.value=e?.message||'加载人员信息失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="人员管理" subtitle="检查报名阵容合规性，并维护球员与教练。">
      <template #actions><RouterLink to="/club/players" class="el-button">球员管理</RouterLink><RouterLink to="/club/coaches" class="el-button el-button--primary">教练管理</RouterLink></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
    <el-card>
      <template #header><b>阵容合规性</b></template>
      <el-alert :type="overview.compliant?'success':'error'" :title="overview.message" :description="overview.reason||''" :closable="false" show-icon/>
      <el-row :gutter="14" class="summary">
        <el-col :span="8"><el-card shadow="never"><span>首发球员</span><strong>{{overview.starterCount||0}} / 11</strong></el-card></el-col>
        <el-col :span="8"><el-card shadow="never"><span>替补球员</span><strong>{{overview.substituteCount||0}} / 7</strong></el-card></el-col>
        <el-col :span="8"><el-card shadow="never"><span>现役教练</span><strong>{{overview.activeCoachCount||0}}</strong></el-card></el-col>
      </el-row>
    </el-card>
    <el-card class="section"><template #header><b>首发球员</b></template><el-table :data="starters" empty-text="暂无首发球员"><el-table-column prop="shirtNo" label="号码" width="72"/><el-table-column prop="playerName" label="姓名"/><el-table-column prop="age" label="年龄"/><el-table-column prop="nationality" label="国籍"/><el-table-column label="位置"><template #default="{row}">{{positionLabel[row.position]||'-'}}</template></el-table-column></el-table></el-card>
    <el-card class="section"><template #header><b>替补球员</b></template><el-table :data="substitutes" empty-text="暂无替补球员"><el-table-column prop="shirtNo" label="号码" width="72"/><el-table-column prop="playerName" label="姓名"/><el-table-column prop="age" label="年龄"/><el-table-column prop="nationality" label="国籍"/><el-table-column label="位置"><template #default="{row}">{{positionLabel[row.position]||'-'}}</template></el-table-column></el-table></el-card>
    <el-card class="section"><template #header><b>现役教练</b></template><el-table :data="activeCoaches" empty-text="暂无现役教练"><el-table-column prop="coachName" label="姓名"/><el-table-column prop="age" label="年龄"/><el-table-column prop="nationality" label="国籍"/><el-table-column label="职务"><template #default="{row}">{{titleLabel[row.title]||'-'}}</template></el-table-column><el-table-column prop="status" label="状态"/></el-table></el-card>
    </DataState>
  </div>
</template>

<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0}.summary{margin-top:18px}.summary .el-card :deep(.el-card__body){display:flex;justify-content:space-between;align-items:center}.summary span{color:#6b7280}.summary strong{font-size:22px}.section{margin-top:16px}</style>
