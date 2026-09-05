<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { removeAvatar, updateProfile, uploadAvatar } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const formRef = ref()
const saving = ref(false)
const avatarBusy = ref(false)
const form = reactive({
  username: authStore.user?.username || '',
  phone: authStore.user?.phone || '',
  realName: authStore.user?.realName || '',
})
const rules = {
  username: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '昵称长度为2到50个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入11位手机号', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}
const roleLabels = { USER: '普通用户', CLUB: '俱乐部负责人', EVENT_ADMIN: '赛事管理员', ADMIN: '系统管理员' }
const roleLabel = computed(() => roleLabels[authStore.user?.roleCode] || authStore.user?.roleCode || '—')
watch(() => authStore.user, (user) => {
  if (!user) return
  Object.assign(form, { username: user.username || '', phone: user.phone || '', realName: user.realName || '' })
}, { deep: true })
const save = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await updateProfile(form)
    await authStore.fetchMe()
    Object.assign(form, {
      username: authStore.user.username,
      phone: authStore.user.phone,
      realName: authStore.user.realName,
    })
    ElMessage.success('账号资料已更新')
  } finally {
    saving.value = false
  }
}
const beforeAvatarUpload = (file) => {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.error('头像仅支持JPEG或PNG格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件不能超过2MB')
    return false
  }
  return true
}
const uploadAvatarFile = async ({ file }) => {
  avatarBusy.value = true
  try {
    await uploadAvatar(file)
    await authStore.fetchMe()
    ElMessage.success('头像已更新')
  } finally {
    avatarBusy.value = false
  }
}
const clearAvatar = async () => {
  await ElMessageBox.confirm('确认移除当前头像？', '移除头像', { type: 'warning' })
  avatarBusy.value = true
  try {
    await removeAvatar()
    await authStore.fetchMe()
    ElMessage.success('头像已移除')
  } finally {
    avatarBusy.value = false
  }
}
</script>

<template>
  <el-card class="profile-card">
    <template #header><h2>账号资料</h2></template>
    <el-alert title="手机号是唯一登录凭证；昵称允许重复，修改昵称或手机号后当前登录仍然有效。" type="info" :closable="false" />
    <section class="avatar-section">
      <el-avatar :size="96" :src="authStore.user?.avatarUrl || undefined" :icon="UserFilled" />
      <div class="avatar-actions">
        <el-upload accept="image/jpeg,image/png" :show-file-list="false" :before-upload="beforeAvatarUpload" :http-request="uploadAvatarFile" :disabled="avatarBusy">
          <el-button type="primary" :loading="avatarBusy">上传或更换头像</el-button>
        </el-upload>
        <el-button v-if="authStore.user?.avatarUrl" :disabled="avatarBusy" @click="clearAvatar">移除头像</el-button>
        <small>支持JPEG、PNG，最大2MB</small>
      </div>
    </section>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="用户ID"><el-input :model-value="authStore.user?.userId" disabled /></el-form-item>
      <el-form-item label="角色"><el-input :model-value="roleLabel" disabled /></el-form-item>
      <el-form-item label="昵称" prop="username"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="真实姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item v-if="authStore.user?.employeeNo" label="管理工号"><el-input :model-value="authStore.user.employeeNo" disabled /></el-form-item>
      <el-form-item v-if="authStore.user?.roleCode === 'CLUB'" label="绑定俱乐部"><el-input :model-value="authStore.user?.clubId || '尚未绑定'" disabled /></el-form-item>
      <el-form-item><el-button type="primary" :loading="saving" @click="save">保存资料</el-button></el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.profile-card { max-width: 720px; }
.profile-card h2 { margin: 0; }
.el-alert { margin-bottom: 20px; }
.avatar-section { display: flex; align-items: center; gap: 20px; margin: 0 0 24px; }
.avatar-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.avatar-actions small { width: 100%; color: #6b7280; }
</style>
