<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createClubCoach, getClubCoaches, updateClubCoach, updateClubCoachStatus } from '../../api/club'
const rows=ref([]), loading=ref(false), visible=ref(false), editingId=ref(null), formRef=ref()
const empty=()=>({coachName:'',title:'',nationality:'',description:''}), form=reactive(empty())
const rules={coachName:[{required:true,message:'请输入教练姓名'}],title:[{required:true,message:'请输入职务'}]}
const load=async()=>{loading.value=true;try{rows.value=(await getClubCoaches()).data}finally{loading.value=false}}
const open=(row)=>{editingId.value=row?.coachId||null;Object.assign(form,empty(),row||{});visible.value=true}
const save=async()=>{await formRef.value.validate();editingId.value?await updateClubCoach(editingId.value,form):await createClubCoach(form);visible.value=false;ElMessage.success('保存成功');await load()}
const toggle=async(row)=>{await updateClubCoachStatus(row.coachId,row.coachStatus==='ACTIVE'?'INACTIVE':'ACTIVE');ElMessage.success('状态已更新');await load()}
onMounted(load)
</script>
<template><el-card><template #header><div class="head"><h2>教练管理</h2><el-button type="primary" @click="open()">新增教练</el-button></div></template><el-table v-loading="loading" :data="rows"><el-table-column prop="coachName" label="姓名"/><el-table-column prop="title" label="职务"/><el-table-column prop="nationality" label="国籍"/><el-table-column prop="description" label="说明" show-overflow-tooltip/><el-table-column prop="coachStatus" label="状态"/><el-table-column label="操作" width="180"><template #default="{row}"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link :type="row.coachStatus==='ACTIVE'?'danger':'success'" @click="toggle(row)">{{row.coachStatus==='ACTIVE'?'停用':'启用'}}</el-button></template></el-table-column></el-table></el-card><el-dialog v-model="visible" :title="editingId?'编辑教练':'新增教练'" width="520px"><el-form ref="formRef" :model="form" :rules="rules" label-width="80px"><el-form-item label="姓名" prop="coachName"><el-input v-model="form.coachName"/></el-form-item><el-form-item label="职务" prop="title"><el-input v-model="form.title"/></el-form-item><el-form-item label="国籍"><el-input v-model="form.nationality"/></el-form-item><el-form-item label="说明"><el-input v-model="form.description" type="textarea"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0}</style>
