<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', realName: '', phone: '', password: '', confirmPassword: '' })
const validateConfirm = (_rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度为4到50个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '只能使用字母、数字和下划线', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入11位手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度为6到72个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.register({ username: form.username, realName: form.realName, phone: form.phone, password: form.password })
    ElMessage.success('注册成功，请登录')
    await router.replace('/login')
  } catch (error) {
    if (!error.__notified) ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <el-card class="auth-card auth-card-wide" shadow="hover">
      <template #header>
        <div class="auth-heading">
          <span class="auth-kicker">CREATE ACCOUNT</span>
          <h1>普通用户注册</h1>
          <p>公开注册只会创建 USER 角色账号</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          <span />
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
        </div>
        <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">注册</el-button>
      </el-form>
      <p class="auth-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
    </el-card>
  </main>
</template>
