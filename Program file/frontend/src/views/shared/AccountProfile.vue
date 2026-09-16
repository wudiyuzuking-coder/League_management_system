<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { removeAvatar, updateProfile, uploadAvatar } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import { maskPhone } from '../../utils/privacy'

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
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为2到50个字符', trigger: 'blur' },
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
    const payload=authStore.user?.roleCode==='USER'?{username:form.username}:{...form}
    await updateProfile(payload)
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
  <div v-if="authStore.user?.roleCode==='USER'" class="user-profile">
    <PageHeader title="用户资料" subtitle="维护赛事账户头像和显示名称。" />
    <CardShell title="个人资料" subtitle="手机号是唯一登录凭证且不可修改。" variant="action">
      <section class="avatar-section">
        <el-avatar :size="96" :src="authStore.user?.avatarUrl || undefined" :icon="UserFilled" />
        <div class="avatar-actions"><strong>{{form.username}}</strong><span>{{maskPhone(form.phone)}}</span><div><el-upload accept="image/jpeg,image/png" :show-file-list="false" :before-upload="beforeAvatarUpload" :http-request="uploadAvatarFile" :disabled="avatarBusy"><el-button type="primary" :loading="avatarBusy">上传或更换头像</el-button></el-upload><el-button v-if="authStore.user?.avatarUrl" :disabled="avatarBusy" @click="clearAvatar">移除头像</el-button></div><small>支持 JPEG、PNG，最大 2MB</small></div>
      </section>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="user-profile__form"><el-form-item label="用户名" prop="username"><el-input v-model="form.username" name="username" autocomplete="username" /></el-form-item><el-form-item label="手机号"><el-input :model-value="maskPhone(form.phone)" disabled /></el-form-item><el-form-item><el-button type="primary" :loading="saving" @click="save">保存资料</el-button></el-form-item></el-form>
    </CardShell>
  </div>
  <div v-else class="management-profile">
    <PageHeader :breadcrumb="[{label:'账号设置'},{label:'账号资料'}]" title="账号资料" :subtitle="`维护${roleLabel}的身份与联系方式。`"><template #status><StatusTag v-if="authStore.user?.userStatus" :value="authStore.user.userStatus" /></template></PageHeader>
    <CardShell title="基本资料" subtitle="头像、用户名、手机号和真实姓名用于账号识别。">
      <section class="avatar-section">
        <el-avatar :size="96" :src="authStore.user?.avatarUrl || undefined" :icon="UserFilled" :alt="`${form.username||roleLabel}头像`" />
        <div class="avatar-actions">
          <strong>{{form.username||roleLabel}}</strong>
          <span>{{maskPhone(form.phone)}}</span>
          <div><el-upload accept="image/jpeg,image/png" :show-file-list="false" :before-upload="beforeAvatarUpload" :http-request="uploadAvatarFile" :disabled="avatarBusy"><el-button type="primary" :loading="avatarBusy">上传或更换头像</el-button></el-upload><el-button v-if="authStore.user?.avatarUrl" :disabled="avatarBusy" @click="clearAvatar">移除头像</el-button></div>
          <small>支持 JPEG、PNG，最大 2MB</small>
        </div>
      </section>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="management-profile__form">
        <el-form-item label="角色"><el-input :model-value="roleLabel" disabled /></el-form-item>
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" name="management-username" autocomplete="username" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" name="management-phone" type="tel" inputmode="tel" autocomplete="tel" /></el-form-item>
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="form.realName" name="management-real-name" autocomplete="name" /></el-form-item>
      </el-form>
    </CardShell>
    <CardShell class="security-card" title="安全与账号状态" subtitle="手机号是唯一登录凭证；账号注销继续由全局账号菜单处理。">
      <dl class="security-list"><div><dt>角色</dt><dd>{{roleLabel}}</dd></div><div><dt>账号状态</dt><dd><StatusTag v-if="authStore.user?.userStatus" :value="authStore.user.userStatus" /><span v-else>以当前登录状态为准</span></dd></div><div v-if="authStore.user?.employeeNo"><dt>管理工号</dt><dd class="score-nums">{{authStore.user.employeeNo}}</dd></div><div v-if="authStore.user?.roleCode==='CLUB'"><dt>绑定俱乐部</dt><dd>{{authStore.user?.clubId||'尚未绑定'}}</dd></div></dl>
    </CardShell>
    <ActionToolbar class="profile-actions" title="保存账号资料" description="保存后会刷新当前账号信息。"><template #actions><el-button type="primary" :loading="saving" @click="save">保存资料</el-button></template></ActionToolbar>
  </div>
</template>

<style scoped>
.el-alert { margin-bottom: 20px; }
.avatar-section { display: flex; align-items: center; gap: 20px; margin: 0 0 24px; }
.avatar-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.avatar-actions small { width: 100%; color: var(--color-text-muted); }
.user-profile{max-width:820px}.user-profile .avatar-section{padding-bottom:var(--space-6);border-bottom:1px solid var(--color-line)}.user-profile .avatar-actions{align-items:flex-start;flex-direction:column}.user-profile .avatar-actions>strong{font-size:var(--font-size-xl)}.user-profile .avatar-actions>span{color:var(--color-text-muted)}.user-profile .avatar-actions>div{display:flex;gap:var(--space-2)}.user-profile__form{max-width:560px;margin-top:var(--space-6)}
.management-profile{max-width:900px}.management-profile .avatar-section{padding-bottom:var(--space-6);border-bottom:1px solid var(--color-line)}.management-profile .avatar-actions{align-items:flex-start;flex-direction:column}.management-profile .avatar-actions>strong{font-size:var(--font-size-xl)}.management-profile .avatar-actions>span{color:var(--color-text-muted)}.management-profile .avatar-actions>div{display:flex;gap:var(--space-2)}.management-profile__form{max-width:620px;margin-top:var(--space-6)}.security-card,.profile-actions{margin-top:var(--space-4)}.security-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4);margin:0}.security-list dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.security-list dd{margin:var(--space-1) 0 0;font-weight:var(--font-weight-semibold)}
</style>
