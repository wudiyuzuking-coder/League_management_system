<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  roleCode: '',
  username: '',
  realName: '',
  clubName: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const roleOptions = [
  { code: 'USER', label: '普通用户' },
  { code: 'CLUB', label: '俱乐部负责人' },
]

const validateConfirm = (_rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const rules = {
  username: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '昵称长度为2到50个字符', trigger: 'blur' },
  ],
  roleCode: [{ required: true, message: '请选择注册身份', trigger: 'change' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  clubName: [{ required: true, message: '请输入俱乐部名字', trigger: 'blur' }],
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

watch(
  () => form.roleCode,
  () => {
    formRef.value?.clearValidate(['realName', 'clubName'])
  },
)

const submit = async () => {
  if (!form.roleCode) {
    ElMessage.warning('请选择注册身份')
    return
  }
  await formRef.value.validate()
  loading.value = true
  try {
    const payload = {
      roleCode: form.roleCode,
      username: form.username,
      phone: form.phone,
      password: form.password,
    }
    if (form.roleCode === 'USER') payload.realName = form.realName
    else if (form.roleCode === 'CLUB') {
      payload.realName = form.realName
      payload.clubName = form.clubName
    }
    await authStore.register(payload)
    ElMessage.success(form.roleCode === 'USER' ? '注册成功，请登录' : '注册成功，请等待系统管理员启用账号')
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
          <h1>账号注册</h1>
          <p>公开注册仅面向普通用户和俱乐部负责人</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="昵称" prop="username"><el-input v-model="form.username" placeholder="昵称允许与其他用户相同" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
        </div>
        <el-form-item label="注册身份" prop="roleCode" required>
          <el-select v-model="form.roleCode" class="role-select" placeholder="请选择注册身份" clearable>
            <el-option v-for="role in roleOptions" :key="role.code" :label="role.label" :value="role.code" />
          </el-select>
        </el-form-item>
        <div v-if="form.roleCode" class="form-grid">
          <el-form-item v-if="form.roleCode === 'USER'" label="姓名" prop="realName" required><el-input v-model="form.realName" /></el-form-item>
          <template v-else-if="form.roleCode === 'CLUB'">
            <el-form-item label="负责人姓名" prop="realName" required><el-input v-model="form.realName" /></el-form-item>
            <el-form-item label="俱乐部名称" prop="clubName" required><el-input v-model="form.clubName" /></el-form-item>
          </template>
        </div>
        <el-button v-if="form.roleCode" class="auth-submit" type="primary" :loading="loading" @click="submit">注册</el-button>
      </el-form>
      <p class="auth-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
    </el-card>
  </main>
</template>

<style scoped>
.role-select {
  width: 100%;
}
</style>
