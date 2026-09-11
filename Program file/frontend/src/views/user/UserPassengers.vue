<script setup>
import {onMounted,reactive,ref} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {addPrefilledPassenger,deletePrefilledPassenger,getPrefilledPassengers} from '../../api/passenger'

const rows=ref([]),loading=ref(false),saving=ref(false),formRef=ref()
const form=reactive({passengerName:'',idCardNo:''})
const rules={passengerName:[{required:true,message:'请输入姓名'}],idCardNo:[{required:true,message:'请输入身份证号'},{pattern:/^\d{17}[\dXx]$/,message:'请输入正确的18位身份证号'}]}
const load=async()=>{loading.value=true;try{rows.value=(await getPrefilledPassengers()).data}finally{loading.value=false}}
const add=async()=>{await formRef.value.validate();saving.value=true;try{await addPrefilledPassenger(form);form.passengerName='';form.idCardNo='';formRef.value.clearValidate();ElMessage.success('购票人已添加');await load()}finally{saving.value=false}}
const remove=async row=>{await ElMessageBox.confirm(`确认删除购票人“${row.passengerName}”？历史订单快照不会受影响。`,'删除购票人',{type:'warning'});await deletePrefilledPassenger(row.prefilledPassengerId);ElMessage.success('购票人已删除，名额已释放');await load()}
onMounted(load)
</script>
<template><el-card v-loading="loading"><template #header><div class="head"><div><h2>预填购票人</h2><small>每个用户最多维护4人；姓名和身份证号均为必填。</small></div><el-tag>{{rows.length}} / 4</el-tag></div></template><el-form ref="formRef" :model="form" :rules="rules" inline><el-form-item label="姓名" prop="passengerName"><el-input v-model="form.passengerName" maxlength="80"/></el-form-item><el-form-item label="身份证号" prop="idCardNo"><el-input v-model="form.idCardNo" maxlength="18"/></el-form-item><el-button type="primary" :disabled="rows.length>=4" :loading="saving" @click="add">新增</el-button></el-form><el-table :data="rows" empty-text="尚未添加购票人"><el-table-column prop="passengerName" label="姓名"/><el-table-column prop="idCardNo" label="身份证号" min-width="220"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table></el-card></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0 0 4px}.head small{color:#6b7280}</style>
