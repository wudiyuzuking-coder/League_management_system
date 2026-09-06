<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ roleCode: '', phone: '', password: '', employeeNo: '' })
const managementRole = computed(() => ['EVENT_ADMIN', 'ADMIN'].includes(form.roleCode))
const roleOptions = [
  { code: 'USER', label: '普通用户' },
  { code: 'CLUB', label: '俱乐部负责人' },
  { code: 'EVENT_ADMIN', label: '赛事管理员' },
  { code: 'ADMIN', label: '系统管理员' },
]
const rules = {
  roleCode: [{ required: true, message: '请选择身份', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入11位手机号', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  employeeNo: [{ validator: (_rule, value, done) => {
    if (!managementRole.value) return done()
    if (!value) return done(new Error('请输入工号'))
    const pattern = form.roleCode === 'EVENT_ADMIN' ? /^EA\d{4}$/ : /^SA\d{4}$/
    return pattern.test(value) ? done() : done(new Error(form.roleCode === 'EVENT_ADMIN' ? '请输入EA加4位数字工号' : '请输入SA加4位数字工号'))
  }, trigger: 'blur' }],
}

watch(() => form.roleCode, () => {
  form.employeeNo = ''
  formRef.value?.clearValidate('employeeNo')
})

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const payload = { roleCode: form.roleCode, phone: form.phone, password: form.password }
    if (managementRole.value) payload.employeeNo = form.employeeNo
    const home = await authStore.login(payload)
    ElMessage.success('登录成功')
    await router.replace(home)
  } catch (error) {
    if (!error.__notified) ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <template #header>
        <div class="auth-heading">
          <span class="auth-kicker">LEAGUE TICKET</span>
          <h1>足球联赛购票系统</h1>
          <p>选择身份后使用手机号登录</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="身份" prop="roleCode">
          <el-select v-model="form.roleCode" placeholder="请选择身份" style="width:100%">
            <el-option v-for="role in roleOptions" :key="role.code" :label="role.label" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" autocomplete="tel" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item v-if="managementRole" label="工号" prop="employeeNo">
          <el-input v-model="form.employeeNo" :placeholder="form.roleCode === 'EVENT_ADMIN' ? 'EA0001' : 'SA0001'" />
        </el-form-item>
        <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <p class="auth-switch">还没有普通用户账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </el-card>
  </main>
</template>
