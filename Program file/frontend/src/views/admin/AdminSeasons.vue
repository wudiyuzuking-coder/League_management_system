<script setup>
import {onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {closeSeasonRegistration,createSeason,getAdminSeasons,updateSeason,updateSeasonStatus} from '../../api/league'
import {confirmAction} from '../../utils/confirmAction'

const rows=ref([]),loading=ref(false),error=ref(''),visible=ref(false),editingId=ref(null),formRef=ref(),saving=ref(false),actionSeasonId=ref(null),closingSeasonId=ref(null)
const empty=()=>({seasonName:'',startDate:'',maxClubs:16}),form=reactive(empty())
const rules={seasonName:[{required:true,message:'请输入赛季名称',trigger:'blur'},{validator:(_,value,callback)=>value?.trim()?callback():callback(new Error('赛季名称不能为空')),trigger:'blur'}],startDate:[{required:true,message:'请选择开始日期'}],maxClubs:[{required:true,type:'number',min:2,max:20,message:'参赛队伍上限为2至20'}]}
const cap=value=>{const n=Number(value);form.maxClubs=Number.isFinite(n)?Math.max(2,Math.min(20,n)):2}
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getAdminSeasons()).data}catch(e){error.value=e?.message||'加载赛季失败，请稍后重试。'}finally{loading.value=false}}
const open=row=>{editingId.value=row?.seasonId||null;Object.assign(form,empty(),row?{seasonName:row.seasonName,startDate:row.startDate,maxClubs:row.maxClubs}:{});visible.value=true}
const save=async()=>{cap(form.maxClubs);await formRef.value.validate();saving.value=true;try{editingId.value?await updateSeason(editingId.value,form):await createSeason(form);visible.value=false;ElMessage.success('赛季及自动时间已生成');await load()}finally{saving.value=false}}
const advance=async row=>{const next={DRAFT:'ACTIVE',ACTIVE:'FINISHED'}[row.seasonStatus];if(!next)return;const finishing=next==='FINISHED';await confirmAction({title:finishing?'结束赛季':'开始赛季',message:finishing?`确定结束赛季“${row.seasonName}”吗？`:`确定将赛季“${row.seasonName}”的状态调整为 ACTIVE 吗？`,impact:finishing?'结束后将关闭该赛季的后续运营流程，请确认相关比赛已经处理完成。':'该操作不会开启报名；报名由 DRAFT 状态和报名时间窗口共同控制。',confirmButtonText:finishing?'确认结束':'确认开始',danger:finishing});actionSeasonId.value=row.seasonId;try{await updateSeasonStatus(row.seasonId,next);ElMessage.success('赛季状态已更新');await load()}finally{actionSeasonId.value=null}}
const closeRegistration=async row=>{await confirmAction({title:'提前截止报名并生成赛程',message:`确定截止赛季“${row.seasonName}”的报名并按当前 ${row.submittedTeamCount??0} 支球队生成赛程吗？`,impact:'生成后俱乐部将无法继续报名，赛程需由赛事管理员另行确认后才会公布。',confirmButtonText:'确认截止并生成'});closingSeasonId.value=row.seasonId;try{await closeSeasonRegistration(row.seasonId);ElMessage.success('报名已截止，待确认赛程已生成');await load()}finally{closingSeasonId.value=null}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="赛季管理" subtitle="创建赛季、查看自动生成的时间窗口并管理赛季状态。"><template #actions><el-button type="primary" @click="open()">新增赛季</el-button></template></PageHeader>
    <section class="app-surface">
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无赛季" empty-description="创建首个赛季后，系统会自动生成报名与预计结束时间。" @retry="load">
        <el-table :data="rows"><el-table-column prop="seasonName" label="赛季" min-width="170"/><el-table-column label="开始日期"><template #default="{row}">{{$formatDate(row.startDate)}}</template></el-table-column><el-table-column label="预计结束"><template #default="{row}">{{$formatDate(row.endDate)}}</template></el-table-column><el-table-column label="报名开始" min-width="170"><template #default="{row}">{{$formatDateTime(row.registrationStartTime)}}</template></el-table-column><el-table-column label="报名截止" min-width="170"><template #default="{row}">{{$formatDateTime(row.registrationDeadline)}}</template></el-table-column><el-table-column prop="maxClubs" label="队伍上限" align="right"/><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.seasonStatus"/></template></el-table-column><el-table-column label="操作" min-width="390"><template #default="{row}"><RouterLink :to="`/admin/seasons/${row.seasonId}`" class="el-button el-button--primary is-link">轮次 / 积分榜</RouterLink><el-button v-if="row.seasonStatus==='DRAFT'" link @click="open(row)">调整</el-button><el-button v-if="row.seasonStatus==='DRAFT'&&!row.scheduleBatchStatus" link type="warning" :disabled="(row.submittedTeamCount??0)<2" :loading="closingSeasonId===row.seasonId" @click="closeRegistration(row)">提前截止报名并生成赛程</el-button><el-button v-if="row.seasonStatus!=='FINISHED'" link :loading="actionSeasonId===row.seasonId" :type="row.seasonStatus==='ACTIVE'?'danger':'success'" @click="advance(row)">{{row.seasonStatus==='DRAFT'?'开始赛季':'结束'}}</el-button></template></el-table-column></el-table>
      </DataState>
    </section>
    <el-dialog v-model="visible" class="app-dialog" :title="editingId?'调整赛季':'新增赛季'" width="480px"><el-alert title="预计结束时间和报名时间由系统生成；售票按每场比赛日期自动计算" type="info" :closable="false"/><el-form ref="formRef" :model="form" :rules="rules" label-width="130px" style="margin-top:16px"><el-form-item label="赛季名称" prop="seasonName"><el-input v-model="form.seasonName" name="season-name" autocomplete="off" maxlength="80" show-word-limit/></el-form-item><el-form-item label="赛季开始日期" prop="startDate"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="参赛队伍上限" prop="maxClubs"><el-input v-model.number="form.maxClubs" name="season-max-clubs" type="number" inputmode="numeric" min="2" max="20" @change="cap(form.maxClubs)"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存赛季</el-button></template></el-dialog>
  </div>
</template>
