<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {createSeason,getSeasons,updateSeason,updateSeasonStatus} from '../../api/league'
const router=useRouter(),rows=ref([]),loading=ref(false),visible=ref(false),editingId=ref(null),formRef=ref()
const empty=()=>({seasonName:'',startDate:'',endDate:'',description:''}),form=reactive(empty())
const rules={seasonName:[{required:true,message:'请输入赛季名称'}],startDate:[{required:true,message:'请选择开始日期'}],endDate:[{required:true,message:'请选择结束日期'}]}
const load=async()=>{loading.value=true;try{rows.value=(await getSeasons()).data}finally{loading.value=false}}
const open=row=>{editingId.value=row?.seasonId||null;Object.assign(form,empty(),row||{});visible.value=true}
const save=async()=>{await formRef.value.validate();if(form.endDate<form.startDate)return ElMessage.error('结束日期不能早于开始日期');editingId.value?await updateSeason(editingId.value,form):await createSeason(form);visible.value=false;ElMessage.success('赛季已保存');await load()}
const advance=async row=>{const next={DRAFT:'ACTIVE',ACTIVE:'FINISHED'}[row.seasonStatus];if(!next)return;await updateSeasonStatus(row.seasonId,next);ElMessage.success('状态已更新');await load()}
onMounted(load)
</script>
<template><el-card><template #header><div class="head"><h2>赛季管理</h2><el-button type="primary" @click="open()">新增赛季</el-button></div></template><el-table v-loading="loading" :data="rows"><el-table-column prop="seasonName" label="赛季"/><el-table-column prop="startDate" label="开始"/><el-table-column prop="endDate" label="结束"/><el-table-column prop="seasonStatus" label="状态"/><el-table-column label="操作" width="260"><template #default="{row}"><el-button link type="primary" @click="router.push(`/admin/seasons/${row.seasonId}`)">轮次/积分榜</el-button><el-button link type="primary" @click="open(row)">编辑</el-button><el-button v-if="row.seasonStatus!=='FINISHED'" link type="success" @click="advance(row)">{{row.seasonStatus==='DRAFT'?'启用':'结束'}}</el-button></template></el-table-column></el-table></el-card><el-dialog v-model="visible" :title="editingId?'编辑赛季':'新增赛季'" width="560px"><el-form ref="formRef" :model="form" :rules="rules" label-width="90px"><el-form-item label="名称" prop="seasonName"><el-input v-model="form.seasonName"/></el-form-item><el-form-item label="开始日期" prop="startDate"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="结束日期" prop="endDate"><el-date-picker v-model="form.endDate" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="说明"><el-input v-model="form.description" type="textarea"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0}</style>
