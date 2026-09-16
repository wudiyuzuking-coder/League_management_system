<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {addPrefilledPassenger,deletePrefilledPassenger,getPrefilledPassengers} from '../../api/passenger'
import {maskIdCard} from '../../utils/privacy'

const rows=ref([]),loading=ref(false),error=ref(''),saving=ref(false),formRef=ref(),confirmVisible=ref(false),deleting=ref(false),pending=ref(null)
const form=reactive({passengerName:'',idCardNo:''})
const rules={passengerName:[{required:true,message:'请输入姓名'}],idCardNo:[{required:true,message:'请输入身份证号'},{pattern:/^\d{17}[\dXx]$/,message:'请输入正确的18位身份证号'}]}
const capacityText=computed(()=>`${rows.value.length} / 4`)
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getPrefilledPassengers()).data}catch(e){error.value=e?.message||'加载购票人失败，请稍后重试。'}finally{loading.value=false}}
const add=async()=>{await formRef.value.validate();saving.value=true;try{await addPrefilledPassenger(form);form.passengerName='';form.idCardNo='';formRef.value.clearValidate();ElMessage.success('购票人已添加');await load()}finally{saving.value=false}}
const requestRemove=row=>{pending.value=row;confirmVisible.value=true}
const remove=async()=>{if(!pending.value)return;deleting.value=true;try{await deletePrefilledPassenger(pending.value.prefilledPassengerId);ElMessage.success('购票人已删除，名额已释放');confirmVisible.value=false;pending.value=null;await load()}finally{deleting.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="预填购票人" subtitle="提前保存常用购票人，创建订单时可直接选择。">
      <template #status><StatusTag value="PASSENGER_LIMIT" :label="`${capacityText} 人`" type="info"/></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
      <CardShell title="新增购票人" subtitle="每位用户最多维护 4 人；姓名和身份证号均为必填。" variant="action">
        <el-form ref="formRef" :model="form" :rules="rules" class="passenger-form"><el-form-item label="姓名" prop="passengerName"><el-input v-model="form.passengerName" name="passenger-name" autocomplete="off" maxlength="80" placeholder="例如：张三…"/></el-form-item><el-form-item label="身份证号" prop="idCardNo"><el-input v-model="form.idCardNo" name="passenger-id-card" autocomplete="off" maxlength="18" placeholder="18 位身份证号…"/></el-form-item><el-button type="primary" :disabled="rows.length>=4" :loading="saving" @click="add">新增购票人</el-button></el-form>
      </CardShell>
      <TableWrapper class="passenger-list" title="已保存购票人" description="身份证号默认脱敏显示。" label="预填购票人列表">
        <el-table :data="rows" row-key="prefilledPassengerId"><el-table-column prop="passengerName" label="姓名" min-width="160"/><el-table-column label="身份证号" min-width="220"><template #default="{row}"><span class="tabular-nums">{{maskIdCard(row.idCardNo)}}</span></template></el-table-column><el-table-column label="状态" width="100"><template #default><StatusTag value="ENABLED" label="可选择" type="success"/></template></el-table-column><el-table-column label="操作" width="100" fixed="right"><template #default="{row}"><el-button link type="danger" @click="requestRemove(row)">删除</el-button></template></el-table-column></el-table>
      </TableWrapper>
    </DataState>
    <ConfirmDialog v-model="confirmVisible" title="删除购票人" :message="`确认删除购票人「${pending?.passengerName||''}」？`" impact="删除后将释放一个预填名额；历史订单和电子票快照不受影响。" confirm-text="确认删除" danger :loading="deleting" @confirm="remove" />
  </div>
</template>

<style scoped>.passenger-form{display:grid;grid-template-columns:1fr 1.5fr auto;align-items:start;gap:var(--space-3)}.passenger-form :deep(.el-form-item){margin-bottom:0}.passenger-list{margin-top:var(--space-4)}@media(max-width:760px){.passenger-form{grid-template-columns:1fr}.passenger-form :deep(.el-form-item){margin-bottom:var(--space-2)}}</style>
