<script setup>
import {computed,reactive,ref,watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {probeManagementAccount} from '../../api/auth'
import {useAuthStore} from '../../stores/auth'
import {getLoginErrorMessage,PHONE_PATTERN} from '../../utils/authForm'

const router=useRouter(),authStore=useAuthStore(),loading=ref(false),stage=ref('IDENTITY')
const form=reactive({roleCode:'',phone:'',employeeNo:'',password:'',confirmPassword:'',realName:''})
const managementRole=computed(()=>['EVENT_ADMIN','ADMIN'].includes(form.roleCode))
const roleOptions=[{code:'USER',label:'普通用户'},{code:'CLUB',label:'俱乐部负责人'},{code:'EVENT_ADMIN',label:'赛事管理员'},{code:'ADMIN',label:'系统管理员'}]
const resetStage=()=>{stage.value='IDENTITY';form.password='';form.confirmPassword='';form.realName=''}
watch(()=>[form.roleCode,form.phone,form.employeeNo],resetStage)
const identityValid=computed(()=>form.roleCode&&PHONE_PATTERN.test(form.phone)&&(!managementRole.value||/^\d{4}$/.test(form.employeeNo)))
const canSubmit=computed(()=>identityValid.value&&(managementRole.value?stage.value!=='IDENTITY'&&form.password.length>=6&&(stage.value!=='ACTIVATE'||(form.realName.trim()&&form.confirmPassword===form.password)):Boolean(form.password)))
const showError=error=>{if(!error?.__notified)ElMessage.error(getLoginErrorMessage(error))}
const probe=async()=>{if(!identityValid.value||loading.value)return;loading.value=true;try{const data=(await probeManagementAccount({roleCode:form.roleCode,phone:form.phone,employeeNo:form.employeeNo})).data;stage.value=data.activationRequired?'ACTIVATE':'PASSWORD'}catch(e){showError(e)}finally{loading.value=false}}
const submit=async()=>{if(managementRole.value&&stage.value==='IDENTITY')return probe();if(!canSubmit.value||loading.value)return;loading.value=true;try{const payload={roleCode:form.roleCode,phone:form.phone,employeeNo:managementRole.value?form.employeeNo:null,password:form.password};const home=stage.value==='ACTIVATE'?await authStore.activateManagement({...payload,realName:form.realName,confirmPassword:form.confirmPassword}):await authStore.login(payload);ElMessage.success(stage.value==='ACTIVATE'?'首次启用成功':'登录成功');await router.replace(home)}catch(e){showError(e)}finally{loading.value=false}}
</script>

<template><main class="auth-page"><el-card class="auth-card" shadow="hover"><template #header><div class="auth-heading"><span class="auth-kicker">LEAGUE TICKET</span><h1>足球联赛购票系统</h1><p>{{managementRole?'先验证手机号与4位工号数字':'选择身份后使用手机号登录'}}</p></div></template><el-form label-position="top" @keyup.enter="submit"><el-form-item label="身份"><el-select v-model="form.roleCode" placeholder="请选择身份" style="width:100%"><el-option v-for="role in roleOptions" :key="role.code" :label="role.label" :value="role.code"/></el-select></el-form-item><el-form-item label="手机号"><el-input v-model="form.phone" autocomplete="tel" placeholder="请输入手机号"/></el-form-item><el-form-item v-if="managementRole" label="4位工号数字"><el-input v-model="form.employeeNo" maxlength="4" placeholder="例如：0001"/></el-form-item><template v-if="!managementRole||stage!=='IDENTITY'"><el-alert v-if="stage==='ACTIVATE'" title="首次启用：姓名必须与预登记信息完全一致" type="info" :closable="false"/><el-form-item v-if="stage==='ACTIVATE'" label="姓名"><el-input v-model="form.realName"/></el-form-item><el-form-item label="密码"><el-input v-model="form.password" type="password" show-password autocomplete="current-password"/></el-form-item><el-form-item v-if="stage==='ACTIVATE'" label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password/></el-form-item></template><el-button class="auth-submit" type="primary" :disabled="managementRole?(!identityValid||loading):(!canSubmit||loading)" :loading="loading" @click="submit">{{managementRole&&stage==='IDENTITY'?'下一步':stage==='ACTIVATE'?'启用并登录':'登录'}}</el-button></el-form><p class="auth-switch">还没有普通用户账号？<RouterLink to="/register">立即注册</RouterLink></p></el-card></main></template>
