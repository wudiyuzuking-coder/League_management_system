<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {createInternalUser,getInternalUsers,updateInternalUserStatus} from '../../api/user'
import {accountStatusLabel} from '../../constants/status'
import {confirmAction} from '../../utils/confirmAction'
import {maskPhone} from '../../utils/privacy'

const rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),saving=ref(false),visible=ref(false),formRef=ref(),actionUserId=ref(null)
const query=reactive({username:'',roleCode:'EVENT_ADMIN',userStatus:'',page:1,size:10})
const form=reactive({roleCode:'EVENT_ADMIN',phone:'',realName:'',employeeNo:''})
const roleOptions=Object.freeze([{value:'EVENT_ADMIN',label:'赛事管理员'},{value:'ADMIN',label:'系统管理员'}])
const statusOptions=Object.freeze([{value:'PENDING_ACTIVATION',label:'待激活'},{value:'ENABLED',label:'正常'},{value:'DISABLED',label:'已停用'},{value:'CANCELLED',label:'已注销'}])
const roleName=code=>code==='EVENT_ADMIN'?'赛事管理员':'系统管理员'
const prefix=computed(()=>form.roleCode==='EVENT_ADMIN'?'EA':'SA')
const rules={roleCode:[{required:true,message:'请选择管理员类型'}],phone:[{required:true,message:'请输入手机号'},{pattern:/^1\d{10}$/,message:'请输入11位手机号'}],realName:[{required:true,message:'请输入姓名'}],employeeNo:[{required:true,message:'请输入4位工号数字'},{pattern:/^\d{4}$/,message:'请输入4位工号数字'}]}
const load=async()=>{loading.value=true;error.value='';try{const data=(await getInternalUsers(query)).data;rows.value=data.records;total.value=data.total}catch(e){error.value=e?.message||'加载内部人员失败，请稍后重试。'}finally{loading.value=false}}
const reset=()=>{Object.assign(query,{username:'',roleCode:'EVENT_ADMIN',userStatus:'',page:1});load()}
const open=()=>{Object.assign(form,{roleCode:query.roleCode,phone:'',realName:'',employeeNo:''});visible.value=true}
const save=async()=>{await formRef.value.validate();saving.value=true;try{await createInternalUser(form);visible.value=false;ElMessage.success('预登记成功，账号待本人首次启用');query.roleCode=form.roleCode;await load()}finally{saving.value=false}}
const toggle=async row=>{if(row.userStatus==='PENDING_ACTIVATION')return ElMessage.warning('等待本人完成首次启用');if(row.userStatus==='CANCELLED')return;const next=row.userStatus==='ENABLED'?'DISABLED':'ENABLED',enabling=next==='ENABLED';await confirmAction({title:enabling?'启用管理账号':'停用管理账号',message:`确定${enabling?'启用':'停用'}“${row.realName}”的账号吗？`,impact:enabling?'启用后，该人员可以重新登录管理端。':'停用后，该人员将无法登录；历史操作和业务数据不会删除。',confirmButtonText:enabling?'确认启用':'确认停用',danger:!enabling});actionUserId.value=row.userId;try{await updateInternalUserStatus(row.userId,next);ElMessage.success(enabling?'账号已重新启用':'账号已停用');await load()}finally{actionUserId.value=null}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="内部人员管理" subtitle="预登记赛事管理员和系统管理员，并管理账号状态。">
      <template #actions><el-button type="primary" @click="open">新增管理账号</el-button></template>
    </PageHeader>
    <section class="app-surface">
      <el-alert class="tip" type="info" :closable="false" title="预登记后状态为待激活；本人使用手机号和 4 位工号数字设置密码。"/>
      <FilterBar :model="query">
        <el-form-item label="姓名"><el-input v-model="query.username" name="internal-user-name" autocomplete="off" clearable/></el-form-item>
        <el-form-item label="管理员类型"><el-select v-model="query.roleCode" @change="query.page=1;load()"><el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.userStatus" clearable><el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select></el-form-item>
        <template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="query.page=1;load()">查询</el-button></template>
      </FilterBar>
      <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无内部人员" @retry="load">
        <el-table :data="rows"><el-table-column prop="realName" label="姓名"/><el-table-column label="手机号"><template #default="{row}">{{maskPhone(row.phone)}}</template></el-table-column><el-table-column prop="employeeNo" label="工号"/><el-table-column label="类型"><template #default="{row}">{{roleName(row.roleCode)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.userStatus"/></template></el-table-column><el-table-column label="操作"><template #default="{row}"><span v-if="row.userStatus==='CANCELLED'">不可操作</span><el-button v-else link :loading="actionUserId===row.userId" :disabled="row.userStatus==='PENDING_ACTIVATION'" :type="row.userStatus==='ENABLED'?'danger':'success'" @click="toggle(row)">{{row.userStatus==='ENABLED'?'停用':'启用'}}</el-button></template></el-table-column></el-table>
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load"/>
      </DataState>
    </section>
    <el-dialog v-model="visible" class="app-dialog" title="预登记管理账号" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px"><el-form-item label="管理员类型" prop="roleCode"><el-select v-model="form.roleCode"><el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" name="management-phone" type="tel" inputmode="tel" autocomplete="off"/></el-form-item><el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" name="management-real-name" autocomplete="off"/></el-form-item><el-form-item label="4位工号数字" prop="employeeNo"><el-input v-model="form.employeeNo" name="management-employee-no" inputmode="numeric" autocomplete="off" maxlength="4"><template #prepend>{{prefix}}</template></el-input></el-form-item></el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确认预登记</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.tip{margin-bottom:var(--space-md)}</style>
