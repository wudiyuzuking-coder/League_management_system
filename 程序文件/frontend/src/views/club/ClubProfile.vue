<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getClubProfile, updateClubProfile } from '../../api/club'
import { getStadiums } from '../../api/match'

const loading = ref(false)
const formRef = ref()
const form = reactive({ clubName: '', shortName: '', logoUrl: '', homeCity: '', homeAddress: '', homeStadiumId: null, description: '' })
const stadiums = ref([])
const rules = { clubName: [{ required: true, message: '请输入俱乐部名称' }], homeCity: [{ required: true, message: '请输入主场城市' }] }
const load = async () => { loading.value = true; try { Object.assign(form, (await getClubProfile()).data) } finally { loading.value = false } }
const save = async () => {
  await formRef.value.validate()
  const payload = { ...form, homeStadiumId: form.homeStadiumId || null }
  Object.assign(form, (await updateClubProfile(payload)).data)
  ElMessage.success('俱乐部资料已保存')
}
onMounted(async()=>{stadiums.value=(await getStadiums()).data;await load()})
</script>

<template>
  <el-card v-loading="loading" class="page-card">
    <template #header><h2>俱乐部资料</h2></template>
    <el-alert :closable="false" type="info" class="leader-info">
      当前负责人：{{form.leaderName||'未绑定'}}（{{form.leaderPhone||'—'}}）
    </el-alert>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="edit-form">
      <el-form-item label="俱乐部名称" prop="clubName"><el-input v-model="form.clubName" /></el-form-item>
      <el-form-item label="简称"><el-input v-model="form.shortName" /></el-form-item>
      <el-form-item label="主场城市" prop="homeCity"><el-input v-model="form.homeCity" /></el-form-item>
      <el-form-item label="主场地址"><el-input v-model="form.homeAddress" /></el-form-item>
      <el-form-item label="主场场馆"><el-select v-model="form.homeStadiumId" clearable filterable placeholder="请选择已有场馆"><el-option v-for="s in stadiums" :key="s.stadiumId" :label="`${s.stadiumName}（${s.city}）`" :value="s.stadiumId" :disabled="s.stadiumStatus!=='ACTIVE'"/></el-select></el-form-item>
      <el-form-item label="队徽URL"><el-input v-model="form.logoUrl" /></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="5" /></el-form-item>
      <el-form-item><el-button type="primary" @click="save">保存</el-button></el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>.page-card h2{margin:0}.leader-info{margin-bottom:18px}.edit-form{max-width:720px}</style>
