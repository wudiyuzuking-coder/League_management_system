<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入11位手机号', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const home = await authStore.login(form)
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
          <p>使用手机号登录</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" autocomplete="tel" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" />
        </el-form-item>
        <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <p class="auth-switch">还没有普通用户账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </el-card>
  </main>
</template>
