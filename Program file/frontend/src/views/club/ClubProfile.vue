<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {getClubProfile,updateClubProfile,uploadClubLogo} from '../../api/club'

const loading=ref(false),error=ref(''),saving=ref(false),logoBusy=ref(false),editing=ref(false),formRef=ref(),saved=ref({})
const form=reactive({clubName:'',shortName:'',logoUrl:'',description:'',clubStatus:'',leaderName:'',venueModel:null,standardHomeComplete:false,city:'',stadiumName:'',address:'',rowsPerZone:null,longSideSeatsPerRow:null,shortSideSeatsPerRow:null,vipPrice:null,normalPrice:null})
const capacity=computed(()=>{const {rowsPerZone:r,longSideSeatsPerRow:l,shortSideSeatsPerRow:s}=form;return r&&l&&s?(l+s)*2*r*2:0})
const structureLocked=computed(()=>form.venueModel==='STANDARD_8'&&form.standardHomeComplete)
const metrics=computed(()=>[
  {label:'球队状态',status:form.clubStatus==='DISABLED'?'DISABLED':'ENABLED'},
  {label:'参赛资格',status:form.standardHomeComplete?'READY':'NOT_READY'},
  {label:'主场容量',value:capacity.value||form.capacity||0,meta:'座位'},
  {label:'票区模型',value:form.standardHomeComplete?'8':'—',meta:form.standardHomeComplete?'标准八区':'待完善'},
])
const required=message=>[{required:true,message,trigger:'blur'}]
const vipPriceValidator=(_,value,callback)=>Number(value)>Number(form.normalPrice)?callback():callback(new Error('VIP 默认票价必须高于普通票价'))
const normalPriceValidator=(_,value,callback)=>Number(form.vipPrice)>Number(value)?callback():callback(new Error('VIP 默认票价必须高于普通票价'))
const rules={clubName:required('请输入俱乐部名称'),city:required('请输入主场城市'),stadiumName:required('请输入场馆名称'),address:required('请输入场馆地址'),rowsPerZone:required('请输入每区排数'),longSideSeatsPerRow:required('请输入长边每排座位数'),shortSideSeatsPerRow:required('请输入宽边每排座位数'),vipPrice:[...required('请输入VIP默认票价'),{validator:vipPriceValidator,trigger:'change'}],normalPrice:[...required('请输入普通默认票价'),{validator:normalPriceValidator,trigger:'change'}]}
const snapshot=()=>({...form})
const load=async()=>{loading.value=true;error.value='';try{Object.assign(form,(await getClubProfile()).data);saved.value=snapshot()}catch(e){error.value=e?.message||'加载俱乐部资料失败，请稍后重试。'}finally{loading.value=false}}
const beginEdit=()=>{saved.value=snapshot();editing.value=true}
const cancelEdit=()=>{Object.assign(form,saved.value);editing.value=false}
const save=async()=>{await formRef.value.validate();if(Number(form.vipPrice)<=Number(form.normalPrice)){ElMessage.error('VIP 默认票价必须高于普通票价');return}saving.value=true;try{const payload={clubName:form.clubName,shortName:form.shortName||null,description:form.description||null,homeStadium:{city:form.city,stadiumName:form.stadiumName,address:form.address,rowsPerZone:form.rowsPerZone,longSideSeatsPerRow:form.longSideSeatsPerRow,shortSideSeatsPerRow:form.shortSideSeatsPerRow,vipPrice:form.vipPrice,normalPrice:form.normalPrice}};Object.assign(form,(await updateClubProfile(payload)).data);saved.value=snapshot();editing.value=false;ElMessage.success('俱乐部及标准私有主场已保存')}finally{saving.value=false}}
const beforeLogo=file=>{const name=(file.name||'').toLowerCase(),declared=file.type||'';if(!/\.(jpg|jpeg|png)$/.test(name)||!['image/jpeg','image/png','application/octet-stream',''].includes(declared)){ElMessage.error('队徽仅支持JPEG或PNG格式');return false}if(file.size>2*1024*1024){ElMessage.error('队徽文件不能超过2MB');return false}return true}
const uploadLogo=async({file})=>{logoBusy.value=true;try{form.logoUrl=(await uploadClubLogo(file)).data.avatarUrl;saved.value={...saved.value,logoUrl:form.logoUrl};ElMessage.success('队徽已更新')}finally{logoBusy.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="form.clubName||'俱乐部资料'" subtitle="球队档案、品牌信息与标准私有主场。">
      <template #status><StatusTag :value="form.clubStatus==='DISABLED'?'DISABLED':'ENABLED'" :label="form.clubStatus==='DISABLED'?'已停用':'运营中'" /></template>
      <template #actions><el-button v-if="!editing" type="primary" @click="beginEdit">编辑资料</el-button><template v-else><el-button :disabled="saving" @click="cancelEdit">取消编辑</el-button><el-button type="primary" :loading="saving" @click="save">保存资料</el-button></template></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
      <MetricStrip :items="metrics" label="球队状态摘要" />
      <el-alert v-if="form.venueModel==='LEGACY'&&!form.standardHomeComplete" title="当前绑定的是历史共享场馆。首次保存将新建本俱乐部私有的标准8票区主场；历史场馆、比赛和库存不会被修改。" type="warning" :closable="false" class="notice"/>

      <template v-if="!editing">
        <div class="profile-grid">
          <CardShell title="球队档案" subtitle="用于赛事展示和球队识别的公开资料。">
            <div class="club-identity"><el-avatar :size="96" :src="form.logoUrl||undefined" :alt="`${form.clubName||'俱乐部'}队徽`">{{form.shortName||form.clubName?.slice(0,1)}}</el-avatar><div><h2>{{form.clubName}}</h2><p>{{form.shortName||'未设置简称'}}</p><p class="description">{{form.description||'暂未填写俱乐部简介。'}}</p></div></div>
          </CardShell>
          <CardShell title="Home Venue" subtitle="STANDARD_8 标准私有主场">
            <dl class="venue-facts"><div><dt>主场名称</dt><dd>{{form.stadiumName||'待完善'}}</dd></div><div><dt>地址</dt><dd>{{[form.city,form.address].filter(Boolean).join(' · ')||'待完善'}}</dd></div><div><dt>容量</dt><dd class="tabular-nums">{{capacity||form.capacity||0}} 座</dd></div><div><dt>VIP 价格</dt><dd>{{$formatMoney(form.vipPrice)}}</dd></div><div><dt>普通票价</dt><dd>{{$formatMoney(form.normalPrice)}}</dd></div><div><dt>八区状态</dt><dd><StatusTag :value="form.standardHomeComplete?'READY':'NOT_READY'" :label="form.standardHomeComplete?'已完成':'待完善'" /></dd></div></dl>
          </CardShell>
        </div>
      </template>

      <CardShell v-else title="编辑球队档案" subtitle="保存后将更新俱乐部资料和标准私有主场。" class="edit-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" class="edit-form">
          <el-divider content-position="left">俱乐部信息</el-divider><el-form-item label="俱乐部名称" prop="clubName"><el-input v-model="form.clubName" name="club-name" autocomplete="organization"/></el-form-item><el-form-item label="简称"><el-input v-model="form.shortName" name="club-short-name" autocomplete="off"/></el-form-item><el-form-item label="队徽"><div class="logo-row"><el-avatar :size="88" :src="form.logoUrl||undefined" :alt="`${form.clubName||'俱乐部'}队徽`">{{form.shortName||form.clubName?.slice(0,1)}}</el-avatar><el-upload accept=".jpg,.jpeg,.png,image/jpeg,image/png" :show-file-list="false" :before-upload="beforeLogo" :http-request="uploadLogo" :disabled="logoBusy"><el-button :loading="logoBusy">上传或更换队徽</el-button></el-upload><small>支持 JPEG、PNG，最大 2MB</small></div></el-form-item><el-form-item label="简介"><el-input v-model="form.description" name="club-description" type="textarea" :rows="4"/></el-form-item>
          <el-divider content-position="left">Home Venue · 标准私有主场</el-divider><el-form-item label="主场城市" prop="city"><el-input v-model="form.city" name="venue-city" autocomplete="address-level2"/></el-form-item><el-form-item label="场馆名称" prop="stadiumName"><el-input v-model="form.stadiumName" name="venue-name" autocomplete="off"/></el-form-item><el-form-item label="场馆地址" prop="address"><el-input v-model="form.address" name="venue-address" autocomplete="street-address"/></el-form-item><el-form-item label="每区排数" prop="rowsPerZone"><el-input-number v-model="form.rowsPerZone" :min="1" :disabled="structureLocked" aria-label="每区排数"/></el-form-item><el-form-item label="长边每排座位数" prop="longSideSeatsPerRow"><el-input-number v-model="form.longSideSeatsPerRow" :min="1" :disabled="structureLocked" aria-label="长边每排座位数"/></el-form-item><el-form-item label="宽边每排座位数" prop="shortSideSeatsPerRow"><el-input-number v-model="form.shortSideSeatsPerRow" :min="1" :disabled="structureLocked" aria-label="宽边每排座位数"/></el-form-item><el-form-item label="VIP 默认票价" prop="vipPrice"><el-input-number v-model="form.vipPrice" :min="0" :precision="2" aria-label="VIP 默认票价"/></el-form-item><el-form-item label="普通默认票价" prop="normalPrice"><el-input-number v-model="form.normalPrice" :min="0" :precision="2" aria-label="普通默认票价"/></el-form-item><el-form-item label="系统计算容量"><strong class="score-nums">{{capacity}} 座</strong></el-form-item>
        </el-form>
      </CardShell>
    </DataState>
  </div>
</template>

<style scoped>
.notice{margin-top:var(--space-4)}.profile-grid{display:grid;grid-template-columns:minmax(0,.9fr) minmax(0,1.1fr);gap:var(--space-4);margin-top:var(--space-4)}.club-identity{display:flex;align-items:flex-start;gap:var(--space-5)}.club-identity h2{margin:0;font-size:var(--font-size-xl)}.club-identity p{margin:var(--space-1) 0 0;color:var(--color-text-muted)}.club-identity .description{margin-top:var(--space-4);color:var(--color-text-secondary);line-height:var(--line-height-body)}.venue-facts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-5);margin:0}.venue-facts div{min-width:0}.venue-facts dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.venue-facts dd{margin:var(--space-1) 0 0;color:var(--color-text-primary);font-weight:var(--font-weight-semibold);line-height:var(--line-height-body)}.edit-card{margin-top:var(--space-4)}.edit-form{max-width:780px}.logo-row{display:flex;align-items:center;flex-wrap:wrap;gap:var(--space-3)}.logo-row small{color:var(--color-text-muted)}@media(max-width:860px){.profile-grid{grid-template-columns:1fr}}
</style>
