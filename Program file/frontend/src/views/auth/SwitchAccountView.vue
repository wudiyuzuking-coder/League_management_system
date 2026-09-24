<script setup>
import {computed,reactive,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {useAuthStore} from '../../stores/auth'
import {getLoginErrorMessage,isLoginFormValid,MANAGEMENT_ROLES,PHONE_PATTERN} from '../../utils/authForm'
import {ROLE_LABELS} from '../../config/navigation'
import {maskPhone} from '../../utils/privacy'
import BackButton from '../../components/BackButton.vue'

const router=useRouter(),authStore=useAuthStore(),formRef=ref(),loading=ref(false)
const form=reactive({roleCode:'',phone:'',password:'',employeeNo:''})
const canSubmit=computed(()=>isLoginFormValid(form))
const rules={roleCode:[{required:true,message:'请选择身份'}],phone:[{required:true,message:'请输入手机号'},{pattern:PHONE_PATTERN,message:'请输入11位手机号'}],password:[{required:true,message:'请输入密码'}],employeeNo:[{required:true,message:'请输入工号'}]}
const submit=async()=>{if(!canSubmit.value||loading.value)return;await formRef.value.validate();loading.value=true;try{const target=await authStore.switchAccount({roleCode:form.roleCode,phone:form.phone,password:form.password,employeeNo:MANAGEMENT_ROLES.includes(form.roleCode)?form.employeeNo:null});ElMessage.success('账户切换成功');await router.replace(target)}catch(error){ElMessage.error(getLoginErrorMessage(error))}finally{loading.value=false}}
</script>
<template><main class="auth-page"><el-card class="auth-card" shadow="hover"><template #header><BackButton :fallback="authStore.homePath" /><div class="auth-heading"><span class="auth-kicker">SWITCH ACCOUNT</span><h1>切换账户</h1><p>验证新账户成功前，当前账户保持登录</p></div></template><el-alert :title="`当前账户：${authStore.user?.username} · ${maskPhone(authStore.user?.phone)} · ${ROLE_LABELS[authStore.user?.roleCode]}`" type="info" :closable="false"/><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="新账户身份" prop="roleCode"><el-select v-model="form.roleCode" style="width:100%"><el-option v-for="(label,code) in ROLE_LABELS" :key="code" :label="label" :value="code"/></el-select></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model="form.phone"/></el-form-item><el-form-item v-if="MANAGEMENT_ROLES.includes(form.roleCode)" label="4位工号数字" prop="employeeNo"><el-input v-model="form.employeeNo" maxlength="4" placeholder="例如：0001"/></el-form-item><el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password/></el-form-item><el-button class="auth-submit" type="primary" :disabled="!canSubmit" :loading="loading" @click="submit">确认切换</el-button></el-form></el-card></main></template>
