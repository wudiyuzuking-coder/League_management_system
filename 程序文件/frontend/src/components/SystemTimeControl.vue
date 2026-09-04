<script setup>
import {computed,onBeforeUnmount,onMounted,ref} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {useSystemTimeStore} from '../stores/systemTime'

const store=useSystemTimeStore()
const dialogVisible=ref(false),targetTime=ref(''),saving=ref(false)
let timer
const displayTime=computed(()=>new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(new Date(store.nowMs)))
const open=()=>{const d=new Date(store.nowMs),pad=n=>String(n).padStart(2,'0');targetTime.value=`${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;dialogVisible.value=true}
const save=async()=>{if(!targetTime.value){ElMessage.warning('请选择目标系统时间');return}saving.value=true;try{await store.set(targetTime.value);dialogVisible.value=false;ElMessage.success('系统时间已调整，全局立即生效')}finally{saving.value=false}}
const reset=async()=>{await ElMessageBox.confirm('确认恢复为服务器真实时间？','恢复真实时间',{type:'warning'});await store.reset();ElMessage.success('系统时间已恢复为真实时间')}
onMounted(async()=>{await store.sync();timer=setInterval(()=>store.tick(),1000)})
onBeforeUnmount(()=>clearInterval(timer))
</script>

<template>
  <div class="system-time">
    <span>系统时间：{{displayTime}}</span>
    <el-button size="small" @click="open">调整时间</el-button>
    <el-button size="small" @click="reset">恢复真实时间</el-button>
  </div>
  <el-dialog v-model="dialogVisible" title="调整演示系统时间" width="430px">
    <el-alert title="仅用于课程设计演示，调整会影响所有登录角色的业务时间判断。" type="warning" :closable="false"/>
    <el-date-picker v-model="targetTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择目标系统时间" class="picker"/>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确认调整</el-button></template>
  </el-dialog>
</template>

<style scoped>
.system-time{display:flex;align-items:center;gap:8px;font-size:13px;color:#4b5563}.picker{width:100%;margin-top:16px}
</style>
