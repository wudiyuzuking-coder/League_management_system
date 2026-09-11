<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {createSeason,getSeasons,updateSeason,updateSeasonStatus} from '../../api/league'
const router=useRouter(),rows=ref([]),loading=ref(false),visible=ref(false),editingId=ref(null),formRef=ref()
const empty=()=>({startDate:'',maxClubs:16}),form=reactive(empty())
const rules={startDate:[{required:true,message:'请选择开始日期'}],maxClubs:[{required:true,type:'number',min:2,max:20,message:'参赛队伍上限为2至20'}]}
const cap=value=>{const n=Number(value);form.maxClubs=Number.isFinite(n)?Math.max(2,Math.min(20,n)):2}
const load=async()=>{loading.value=true;try{rows.value=(await getSeasons()).data}finally{loading.value=false}}
const open=row=>{editingId.value=row?.seasonId||null;Object.assign(form,empty(),row?{startDate:row.startDate,maxClubs:row.maxClubs}:{});visible.value=true}
const save=async()=>{cap(form.maxClubs);await formRef.value.validate();editingId.value?await updateSeason(editingId.value,form):await createSeason(form);visible.value=false;ElMessage.success('赛季及自动时间已生成');await load()}
const advance=async row=>{const next={DRAFT:'ACTIVE',ACTIVE:'FINISHED'}[row.seasonStatus];if(!next)return;await updateSeasonStatus(row.seasonId,next);ElMessage.success('状态已更新');await load()}
onMounted(load)
</script>
<template><el-card><template #header><div class="head"><h2>赛季管理</h2><el-button type="primary" @click="open()">新增赛季</el-button></div></template><el-table v-loading="loading" :data="rows"><el-table-column prop="seasonName" label="赛季"/><el-table-column prop="startDate" label="开始日期"/><el-table-column prop="endDate" label="预计结束"/><el-table-column prop="registrationStartTime" label="报名开始" min-width="170"/><el-table-column prop="registrationDeadline" label="报名截止" min-width="170"/><el-table-column prop="ticketSaleStartTime" label="统一开售" min-width="170"/><el-table-column prop="maxClubs" label="队伍上限"/><el-table-column prop="seasonStatus" label="状态"/><el-table-column label="操作" width="260"><template #default="{row}"><el-button link type="primary" @click="router.push(`/admin/seasons/${row.seasonId}`)">轮次/积分榜</el-button><el-button v-if="row.seasonStatus==='DRAFT'" link type="primary" @click="open(row)">调整</el-button><el-button v-if="row.seasonStatus!=='FINISHED'" link type="success" @click="advance(row)">{{row.seasonStatus==='DRAFT'?'启用':'结束'}}</el-button></template></el-table-column></el-table></el-card><el-dialog v-model="visible" :title="editingId?'调整赛季':'新增赛季'" width="480px"><el-alert title="赛季名称、预计结束时间、报名及售票时间由系统自动生成" type="info" :closable="false"/><el-form ref="formRef" :model="form" :rules="rules" label-width="130px" style="margin-top:16px"><el-form-item label="赛季开始日期" prop="startDate"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="参赛队伍上限" prop="maxClubs"><el-input v-model.number="form.maxClubs" type="number" min="2" max="20" @change="cap(form.maxClubs)"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0}</style>
