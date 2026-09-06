<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {createStadium,getAdminStadiums,updateStadium,updateStadiumStatus} from '../../api/stadium'

const router=useRouter(),rows=ref([]),loading=ref(false),visible=ref(false),editingId=ref(null),formRef=ref()
const query=reactive({name:'',city:''})
const blank=()=>({stadiumName:'',city:'',address:'',capacity:1,layoutDesc:''})
const form=reactive(blank())
const rules={stadiumName:[{required:true,message:'请输入场馆名称'}],city:[{required:true,message:'请输入城市'}],address:[{required:true,message:'请输入地址'}],capacity:[{required:true,type:'number',min:1,message:'容量必须大于0'}]}
const load=async()=>{loading.value=true;try{rows.value=(await getAdminStadiums(query)).data}finally{loading.value=false}}
const open=row=>{editingId.value=row?.stadiumId||null;Object.assign(form,blank(),row||{});visible.value=true}
const save=async()=>{await formRef.value.validate();editingId.value?await updateStadium(editingId.value,form):await createStadium(form);visible.value=false;ElMessage.success('场馆已保存');await load()}
const toggle=async row=>{await updateStadiumStatus(row.stadiumId,row.stadiumStatus==='ACTIVE'?'DISABLED':'ACTIVE');ElMessage.success('场馆状态已更新');await load()}
onMounted(load)
</script>

<template>
  <el-card>
    <template #header><div class="head"><h2>场馆管理</h2><el-button type="primary" @click="open()">新增场馆</el-button></div></template>
    <el-form inline>
      <el-form-item label="名称"><el-input v-model="query.name" clearable placeholder="模糊查询"/></el-form-item>
      <el-form-item label="城市"><el-input v-model="query.city" clearable placeholder="模糊查询"/></el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="stadiumName" label="名称" min-width="150"/>
      <el-table-column prop="city" label="城市"/>
      <el-table-column prop="address" label="地址" min-width="190"/>
      <el-table-column prop="capacity" label="申报容量" width="100"/>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.stadiumStatus==='ACTIVE'?'success':'info'">{{row.stadiumStatus}}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="230"><template #default="{row}">
        <el-button link type="primary" @click="router.push(`/admin/stadiums/${row.stadiumId}`)">座位布局</el-button>
        <el-button link type="primary" @click="open(row)">编辑</el-button>
        <el-button link :type="row.stadiumStatus==='ACTIVE'?'warning':'success'" @click="toggle(row)">{{row.stadiumStatus==='ACTIVE'?'停用':'启用'}}</el-button>
      </template></el-table-column>
    </el-table>
  </el-card>
  <el-dialog v-model="visible" :title="editingId?'编辑场馆':'新增场馆'" width="580px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="名称" prop="stadiumName"><el-input v-model="form.stadiumName" maxlength="100"/></el-form-item>
      <el-form-item label="城市" prop="city"><el-input v-model="form.city" maxlength="50"/></el-form-item>
      <el-form-item label="地址" prop="address"><el-input v-model="form.address" maxlength="255"/></el-form-item>
      <el-form-item label="申报容量" prop="capacity"><el-input-number v-model="form.capacity" :min="1"/></el-form-item>
      <el-form-item label="布局说明"><el-input v-model="form.layoutDesc" type="textarea" maxlength="500" show-word-limit/></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.head h2{margin:0}</style>
