<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createClubPlayer, getClubPlayers, updateClubPlayer, updateClubPlayerStatus } from '../../api/club'

const rows = ref([]), loading = ref(false), visible = ref(false), formRef = ref(), editingId = ref(null)
const empty = () => ({ playerName: '', shirtNo: 1, position: 'FORWARD', nationality: '', birthDate: null })
const form = reactive(empty())
const rules = { playerName: [{ required: true, message: '请输入球员姓名' }], shirtNo: [{ required: true, type: 'number', min: 1, max: 99, message: '球衣号为1～99' }], position: [{ required: true, message: '请选择位置' }] }
const positions = [['GOALKEEPER','守门员'],['DEFENDER','后卫'],['MIDFIELDER','中场'],['FORWARD','前锋']]
const load = async () => { loading.value = true; try { rows.value = (await getClubPlayers()).data } finally { loading.value = false } }
const open = (row) => { editingId.value = row?.playerId || null; Object.assign(form, empty(), row || {}); visible.value = true }
const save = async () => { await formRef.value.validate(); editingId.value ? await updateClubPlayer(editingId.value, form) : await createClubPlayer(form); visible.value = false; ElMessage.success('保存成功'); await load() }
const toggle = async (row) => { await updateClubPlayerStatus(row.playerId, row.playerStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'); ElMessage.success('状态已更新'); await load() }
onMounted(load)
</script>

<template>
  <el-card><template #header><div class="head"><h2>球员管理</h2><el-button type="primary" @click="open()">新增球员</el-button></div></template>
    <el-table v-loading="loading" :data="rows"><el-table-column prop="shirtNo" label="号码" width="80"/><el-table-column prop="playerName" label="姓名"/><el-table-column prop="position" label="位置"/><el-table-column prop="nationality" label="国籍"/><el-table-column prop="playerStatus" label="状态"/><el-table-column label="操作" width="180"><template #default="{row}"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link :type="row.playerStatus==='ACTIVE'?'danger':'success'" @click="toggle(row)">{{ row.playerStatus==='ACTIVE'?'停用':'启用' }}</el-button></template></el-table-column></el-table>
  </el-card>
  <el-dialog v-model="visible" :title="editingId?'编辑球员':'新增球员'" width="520px"><el-form ref="formRef" :model="form" :rules="rules" label-width="80px"><el-form-item label="姓名" prop="playerName"><el-input v-model="form.playerName"/></el-form-item><el-form-item label="球衣号" prop="shirtNo"><el-input-number v-model="form.shirtNo" :min="1" :max="99"/></el-form-item><el-form-item label="位置" prop="position"><el-select v-model="form.position"><el-option v-for="p in positions" :key="p[0]" :label="p[1]" :value="p[0]"/></el-select></el-form-item><el-form-item label="国籍"><el-input v-model="form.nationality"/></el-form-item><el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" value-format="YYYY-MM-DD"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
</template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0}</style>
