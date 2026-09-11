<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {cleanupClubPlayer,createClubPlayer,getClubPlayers,returnClubPlayer,updateClubPlayer,updateClubPlayerStatus} from '../../api/club'

const rows=ref([]),loading=ref(false),visible=ref(false),editingId=ref(null),returning=ref(false),organized=ref(false),formRef=ref()
const positions=[['GOALKEEPER','门将'],['DEFENDER','后卫'],['MIDFIELDER','中场'],['FORWARD','前锋']]
const positionLabel=Object.fromEntries(positions),roleLabel={STARTER:'首发',SUBSTITUTE:'替补'}
const empty=()=>({playerName:'',birthYear:null,nationality:'',shirtNo:'',position:'FORWARD',lineupRole:'SUBSTITUTE'})
const form=reactive(empty())
const shirtValidator=(_r,value,done)=>{const number=Number(value);Number.isInteger(number)&&number>=1&&number<=99?done():done(new Error('球衣号为1～99'))}
const rules={playerName:[{required:true,message:'请输入球员姓名'}],birthYear:[{required:true,message:'请输入出生年份'}],nationality:[{required:true,message:'请输入国籍'}],shirtNo:[{validator:shirtValidator,trigger:'blur'}],position:[{required:true,message:'请选择位置'}],lineupRole:[{required:true,message:'请选择首发或替补'}]}
const displayed=computed(()=>{if(!organized.value)return rows.value;const order={STARTER:0,SUBSTITUTE:1};return [...rows.value].sort((a,b)=>{const left=a.playerStatus==='TRANSFERRED'?2:(order[a.lineupRole]??2),right=b.playerStatus==='TRANSFERRED'?2:(order[b.lineupRole]??2);return left-right||(a.shirtNo??999)-(b.shirtNo??999)||a.playerId-b.playerId})})
const load=async()=>{loading.value=true;try{rows.value=(await getClubPlayers()).data}finally{loading.value=false}}
const openCreate=()=>{editingId.value=null;returning.value=false;Object.assign(form,empty());visible.value=true}
const openAdjust=row=>{editingId.value=row.playerId;returning.value=false;Object.assign(form,empty(),row,{shirtNo:String(row.shirtNo??'')});visible.value=true}
const openReturn=row=>{editingId.value=row.playerId;returning.value=true;Object.assign(form,empty(),row,{shirtNo:''});visible.value=true}
const mutablePayload=()=>({shirtNo:Number(form.shirtNo),position:form.position,lineupRole:form.lineupRole})
const save=async()=>{await formRef.value.validate();if(!editingId.value){await ElMessageBox.confirm('球员姓名、出生年份和国籍添加后无法修改，请确认后再添加。','确认新增',{type:'warning'});await createClubPlayer({...mutablePayload(),playerName:form.playerName,birthYear:Number(form.birthYear),nationality:form.nationality,birthDate:null})}else if(returning.value){await returnClubPlayer(editingId.value,mutablePayload())}else{await updateClubPlayer(editingId.value,mutablePayload())}visible.value=false;ElMessage.success('保存成功');await load()}
const leave=async row=>{await ElMessageBox.confirm(`确认将「${row.playerName}」设为离队？其球衣号将立即释放。`,'离队确认',{type:'warning'});await updateClubPlayerStatus(row.playerId,'TRANSFERRED');ElMessage.success('已离队');await load()}
const cleanup=async row=>{await ElMessageBox.confirm(`确认清理离队球员「${row.playerName}」？`,'清理确认',{type:'warning'});await cleanupClubPlayer(row.playerId);ElMessage.success('已清理');await load()}
onMounted(load)
</script>

<template>
  <el-card><template #header><div class="head"><h2>球员管理</h2><div><el-button @click="organized=!organized">{{organized?'恢复添加顺序':'整理'}}</el-button><el-button type="primary" @click="openCreate">新增球员</el-button></div></div></template>
    <el-table v-loading="loading" :data="displayed" empty-text="暂无球员"><el-table-column prop="shirtNo" label="号码" width="72"><template #default="{row}">{{row.shirtNo??'-'}}</template></el-table-column><el-table-column prop="playerName" label="姓名"/><el-table-column prop="birthYear" label="出生年份"><template #default="{row}">{{row.birthYear??(row.birthDate?String(row.birthDate).slice(0,4):'-')}}</template></el-table-column><el-table-column prop="nationality" label="国籍"/><el-table-column label="位置"><template #default="{row}">{{positionLabel[row.position]||'-'}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><el-tag :type="row.playerStatus==='TRANSFERRED'?'info':row.lineupRole==='STARTER'?'success':''">{{row.playerStatus==='TRANSFERRED'?'离队':(roleLabel[row.lineupRole]||'待设置')}}</el-tag></template></el-table-column><el-table-column label="操作" width="210"><template #default="{row}"><template v-if="row.playerStatus!=='TRANSFERRED'"><el-button link type="primary" @click="openAdjust(row)">调整</el-button><el-button link type="danger" @click="leave(row)">离队</el-button></template><template v-else><el-button link type="success" @click="openReturn(row)">归队</el-button><el-button link type="danger" @click="cleanup(row)">清理</el-button></template></template></el-table-column></el-table>
  </el-card>
  <el-dialog v-model="visible" :title="!editingId?'新增球员':returning?'球员归队':'调整球员'" width="540px"><el-form ref="formRef" :model="form" :rules="rules" label-width="96px"><el-form-item label="姓名" prop="playerName"><el-input v-model="form.playerName" :disabled="!!editingId"/></el-form-item><el-form-item label="出生年份" prop="birthYear"><el-input v-model="form.birthYear" inputmode="numeric" :disabled="!!editingId"/></el-form-item><el-form-item label="国籍" prop="nationality"><el-input v-model="form.nationality" :disabled="!!editingId"/></el-form-item><el-form-item label="首发/替补" prop="lineupRole"><el-select v-model="form.lineupRole"><el-option label="首发" value="STARTER"/><el-option label="替补" value="SUBSTITUTE"/></el-select></el-form-item><el-form-item label="球衣号" prop="shirtNo"><el-input v-model="form.shirtNo" inputmode="numeric" placeholder="1～99"/></el-form-item><el-form-item label="场上位置" prop="position"><el-select v-model="form.position"><el-option v-for="p in positions" :key="p[0]" :label="p[1]" :value="p[0]"/></el-select></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
</template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.head h2{margin:0}</style>
