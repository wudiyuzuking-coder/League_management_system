<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {cleanupClubCoach,createClubCoach,getClubCoaches,updateClubCoach,updateClubCoachStatus} from '../../api/club'

const rows=ref([]),loading=ref(false),error=ref(''),visible=ref(false),editingId=ref(null),organized=ref(false),formRef=ref(),saving=ref(false)
const confirmVisible=ref(false),confirming=ref(false),pendingAction=ref(null)
const empty=()=>({coachName:'',nationality:'',birthYear:null,title:'ASSISTANT_COACH',description:''}),form=reactive(empty())
const titleLabel={HEAD_COACH:'主教练',ASSISTANT_COACH:'助理教练'}
const rules={coachName:[{required:true,message:'请输入教练姓名'}],nationality:[{required:true,message:'请输入国籍'}],birthYear:[{required:true,message:'请输入出生年份'}],title:[{required:true,message:'请选择职务'}]}
const displayed=computed(()=>!organized.value?rows.value:[...rows.value].sort((a,b)=>(a.coachStatus==='ACTIVE'?0:1)-(b.coachStatus==='ACTIVE'?0:1)||(a.title==='HEAD_COACH'?0:1)-(b.title==='HEAD_COACH'?0:1)||a.coachId-b.coachId))
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getClubCoaches()).data}catch(e){error.value=e?.message||'加载教练失败，请稍后重试。'}finally{loading.value=false}}
const open=row=>{editingId.value=row?.coachId||null;Object.assign(form,empty(),row||{});visible.value=true}
const save=async()=>{await formRef.value.validate();saving.value=true;try{editingId.value?await updateClubCoach(editingId.value,{title:form.title}):await createClubCoach({...form,birthYear:Number(form.birthYear)});visible.value=false;ElMessage.success('保存成功');await load()}finally{saving.value=false}}
const restore=async row=>{await updateClubCoachStatus(row.coachId,'ACTIVE');ElMessage.success('已归队');await load()}
const requestRisk=(row,kind)=>{pendingAction.value={row,kind};confirmVisible.value=true}
const riskCopy=computed(()=>pendingAction.value?.kind==='cleanup'?{title:'清理离队教练',message:`确认清理离队教练「${pendingAction.value?.row.coachName||''}」？`,impact:'当前教练记录将被清理，既有历史业务引用仍由系统规则处理。',confirm:'确认清理'}:{title:'确认教练离队',message:`确认将「${pendingAction.value?.row.coachName||''}」设为离队？`,impact:'教练将退出当前教练组，历史报名快照和比赛记录仍保留。',confirm:'确认离队'})
const executeRisk=async()=>{const action=pendingAction.value;if(!action)return;confirming.value=true;try{action.kind==='cleanup'?await cleanupClubCoach(action.row.coachId):await updateClubCoachStatus(action.row.coachId,'INACTIVE');ElMessage.success(action.kind==='cleanup'?'已清理':'已离队');confirmVisible.value=false;pendingAction.value=null;await load()}finally{confirming.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="教练管理" subtitle="维护教练组角色和在队状态。">
      <template #actions><el-button @click="organized=!organized">{{organized?'恢复添加顺序':'按角色整理'}}</el-button><el-button type="primary" @click="open()">新增教练</el-button></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无教练" empty-description="新增教练后即可组建教练团队。" @retry="load">
      <TableWrapper title="教练组" description="主教练优先展示，离队人员保留为可追溯记录。" label="教练列表">
        <el-table :data="displayed" row-key="coachId">
          <el-table-column prop="coachName" label="姓名" min-width="160"/>
          <el-table-column label="角色" width="130"><template #default="{row}"><StatusTag :value="row.title" :label="titleLabel[row.title]||'—'" type="info" /></template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.coachStatus" :label="row.coachStatus==='ACTIVE'?'现役':'已离队'" :type="row.coachStatus==='ACTIVE'?'success':'info'" /></template></el-table-column>
          <el-table-column prop="nationality" label="国籍" min-width="120"/>
          <el-table-column label="年龄" width="90"><template #default="{row}">{{row.age??(row.birthYear?new Date().getFullYear()-row.birthYear:'—')}}</template></el-table-column>
          <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button v-if="row.coachStatus==='ACTIVE'" link type="primary" @click="open(row)">调整</el-button><el-button v-else link type="primary" @click="restore(row)">归队</el-button><el-dropdown trigger="click"><el-button link>更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-if="row.coachStatus==='ACTIVE'" @click="requestRisk(row,'leave')">离队</el-dropdown-item><el-dropdown-item v-else @click="requestRisk(row,'cleanup')">清理记录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
        </el-table>
      </TableWrapper>
    </DataState>
    <el-dialog v-model="visible" class="app-dialog" :title="editingId?'调整教练':'新增教练'" width="520px" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"><el-form ref="formRef" :model="form" :rules="rules" label-width="90px"><el-form-item label="姓名" prop="coachName"><el-input v-model="form.coachName" name="coach-name" autocomplete="off" :disabled="!!editingId"/></el-form-item><el-form-item label="国籍" prop="nationality"><el-input v-model="form.nationality" name="coach-nationality" autocomplete="off" :disabled="!!editingId"/></el-form-item><el-form-item label="出生年份" prop="birthYear"><el-input v-model="form.birthYear" name="coach-birth-year" inputmode="numeric" autocomplete="off" :disabled="!!editingId"/></el-form-item><el-form-item label="职务" prop="title"><el-select v-model="form.title" aria-label="教练职务"><el-option label="主教练" value="HEAD_COACH"/><el-option label="助理教练" value="ASSISTANT_COACH"/></el-select></el-form-item></el-form><template #footer><el-button :disabled="saving" @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
    <ConfirmDialog v-model="confirmVisible" :title="riskCopy.title" :message="riskCopy.message" :impact="riskCopy.impact" :confirm-text="riskCopy.confirm" danger :loading="confirming" @confirm="executeRisk" />
  </div>
</template>
