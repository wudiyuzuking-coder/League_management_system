<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {getClubProfile,updateClubProfile,uploadClubLogo} from '../../api/club'

const loading=ref(false),error=ref(''),saving=ref(false),logoBusy=ref(false),formRef=ref()
const form=reactive({clubName:'',shortName:'',logoUrl:'',description:'',venueModel:null,standardHomeComplete:false,city:'',stadiumName:'',address:'',rowsPerZone:null,longSideSeatsPerRow:null,shortSideSeatsPerRow:null,vipPrice:null,normalPrice:null})
const capacity=computed(()=>{const {rowsPerZone:r,longSideSeatsPerRow:l,shortSideSeatsPerRow:s}=form;return r&&l&&s?(l+s)*2*r*2:0})
const structureLocked=computed(()=>form.venueModel==='STANDARD_8'&&form.standardHomeComplete)
const required=message=>[{required:true,message,trigger:'blur'}]
const vipPriceValidator=(_,value,callback)=>Number(value)>Number(form.normalPrice)?callback():callback(new Error('VIP 默认票价必须高于普通票价'))
const normalPriceValidator=(_,value,callback)=>Number(form.vipPrice)>Number(value)?callback():callback(new Error('VIP 默认票价必须高于普通票价'))
const rules={clubName:required('请输入俱乐部名称'),city:required('请输入主场城市'),stadiumName:required('请输入场馆名称'),address:required('请输入场馆地址'),rowsPerZone:required('请输入每区排数'),longSideSeatsPerRow:required('请输入长边每排座位数'),shortSideSeatsPerRow:required('请输入宽边每排座位数'),vipPrice:[...required('请输入VIP默认票价'),{validator:vipPriceValidator,trigger:'change'}],normalPrice:[...required('请输入普通默认票价'),{validator:normalPriceValidator,trigger:'change'}]}
const load=async()=>{loading.value=true;error.value='';try{Object.assign(form,(await getClubProfile()).data)}catch(e){error.value=e?.message||'加载俱乐部资料失败，请稍后重试。'}finally{loading.value=false}}
const save=async()=>{await formRef.value.validate();if(Number(form.vipPrice)<=Number(form.normalPrice)){ElMessage.error('VIP 默认票价必须高于普通票价');return}saving.value=true;try{const payload={clubName:form.clubName,shortName:form.shortName||null,description:form.description||null,homeStadium:{city:form.city,stadiumName:form.stadiumName,address:form.address,rowsPerZone:form.rowsPerZone,longSideSeatsPerRow:form.longSideSeatsPerRow,shortSideSeatsPerRow:form.shortSideSeatsPerRow,vipPrice:form.vipPrice,normalPrice:form.normalPrice}};Object.assign(form,(await updateClubProfile(payload)).data);ElMessage.success('俱乐部及标准私有主场已保存')}finally{saving.value=false}}
const beforeLogo=file=>{const name=(file.name||'').toLowerCase(),declared=file.type||'';if(!/\.(jpg|jpeg|png)$/.test(name)||!['image/jpeg','image/png','application/octet-stream',''].includes(declared)){ElMessage.error('队徽仅支持JPEG或PNG格式');return false}if(file.size>2*1024*1024){ElMessage.error('队徽文件不能超过2MB');return false}return true}
const uploadLogo=async({file})=>{logoBusy.value=true;try{form.logoUrl=(await uploadClubLogo(file)).data.avatarUrl;ElMessage.success('队徽已更新')}finally{logoBusy.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="俱乐部资料" subtitle="维护俱乐部品牌资料、队徽与标准私有主场。" />
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
    <section class="app-surface page-card">
    <el-alert v-if="form.venueModel==='LEGACY'&&!form.standardHomeComplete" title="当前绑定的是历史共享场馆。首次保存将新建本俱乐部私有的标准8票区主场；历史场馆、比赛和库存不会被修改。" type="warning" :closable="false" class="notice"/>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" class="edit-form">
      <el-divider content-position="left">俱乐部信息</el-divider>
      <el-form-item label="俱乐部名称" prop="clubName"><el-input v-model="form.clubName"/></el-form-item>
      <el-form-item label="简称"><el-input v-model="form.shortName"/></el-form-item>
      <el-form-item label="队徽"><div class="logo-row"><el-avatar :size="88" :src="form.logoUrl||undefined" :alt="`${form.clubName||'俱乐部'}队徽`">{{form.shortName||form.clubName?.slice(0,1)}}</el-avatar><el-upload accept=".jpg,.jpeg,.png,image/jpeg,image/png" :show-file-list="false" :before-upload="beforeLogo" :http-request="uploadLogo" :disabled="logoBusy"><el-button type="primary" :loading="logoBusy">上传或更换队徽</el-button></el-upload><small>支持 JPEG、PNG，最大 2MB</small></div></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="4"/></el-form-item>
      <el-divider content-position="left">标准私有主场</el-divider>
      <el-form-item label="主场城市" prop="city"><el-input v-model="form.city"/></el-form-item>
      <el-form-item label="场馆名称" prop="stadiumName"><el-input v-model="form.stadiumName"/></el-form-item>
      <el-form-item label="场馆地址" prop="address"><el-input v-model="form.address"/></el-form-item>
      <el-form-item label="每区排数" prop="rowsPerZone"><el-input-number v-model="form.rowsPerZone" :min="1" :disabled="structureLocked"/></el-form-item>
      <el-form-item label="长边每排座位数" prop="longSideSeatsPerRow"><el-input-number v-model="form.longSideSeatsPerRow" :min="1" :disabled="structureLocked"/></el-form-item>
      <el-form-item label="宽边每排座位数" prop="shortSideSeatsPerRow"><el-input-number v-model="form.shortSideSeatsPerRow" :min="1" :disabled="structureLocked"/></el-form-item>
      <el-form-item label="VIP 默认票价" prop="vipPrice"><el-input-number v-model="form.vipPrice" :min="0" :precision="2"/></el-form-item>
      <el-form-item label="普通默认票价" prop="normalPrice"><el-input-number v-model="form.normalPrice" :min="0" :precision="2"/></el-form-item>
      <el-form-item label="系统计算容量"><strong>{{capacity}} 座</strong></el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="save">保存俱乐部资料</el-button></el-form-item>
    </el-form>
    </section>
    </DataState>
  </div>
</template>

<style scoped>.page-card h2{margin:0}.notice{margin-bottom:18px}.edit-form{max-width:760px}.logo-row{display:flex;align-items:center;gap:14px}.logo-row small{color:#6b7280}</style>
