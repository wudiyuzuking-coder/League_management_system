<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {cleanupClubPlayer,createClubPlayer,getClubPlayers,returnClubPlayer,updateClubPlayer,updateClubPlayerStatus} from '../../api/club'
import {useSystemTimeStore} from '../../stores/systemTime'

const rows=ref([]),loading=ref(false),error=ref(''),visible=ref(false),editingId=ref(null),returning=ref(false),organized=ref(false),formRef=ref(),saving=ref(false)
const systemTime=useSystemTimeStore()
const confirmVisible=ref(false),confirming=ref(false),pendingAction=ref(null)
const positions=[['GOALKEEPER','门将'],['DEFENDER','后卫'],['MIDFIELDER','中场'],['FORWARD','前锋']]
const positionLabel=Object.fromEntries(positions)
const empty=()=>({playerName:'',birthYear:null,nationality:'',shirtNo:'',position:'FORWARD',lineupRole:'SUBSTITUTE'})
const form=reactive(empty())
const currentYear=computed(()=>new Date(systemTime.nowMs).getFullYear())
const playerAgeError=computed(()=>{
  if(form.birthYear===null||form.birthYear==='')return ''
  const year=Number(form.birthYear),age=currentYear.value-year
  return Number.isInteger(year)&&age>=18&&age<=50?'':'球员年龄必须在18至50岁之间'
})
const shirtValidator=(_r,value,done)=>{const number=Number(value);Number.isInteger(number)&&number>=1&&number<=99?done():done(new Error('球衣号为1～99'))}
const birthYearValidator=(_r,_value,done)=>playerAgeError.value?done(new Error(playerAgeError.value)):done()
const rules={playerName:[{required:true,message:'请输入球员姓名'}],birthYear:[{required:true,message:'请输入出生年份'},{validator:birthYearValidator,trigger:['change','blur']}],nationality:[{required:true,message:'请输入国籍'}],shirtNo:[{validator:shirtValidator,trigger:'blur'}],position:[{required:true,message:'请选择位置'}],lineupRole:[{required:true,message:'请选择首发或替补'}]}
const displayed=computed(()=>{if(!organized.value)return rows.value;const order={STARTER:0,SUBSTITUTE:1};return [...rows.value].sort((a,b)=>{const left=a.playerStatus==='TRANSFERRED'?2:(order[a.lineupRole]??2),right=b.playerStatus==='TRANSFERRED'?2:(order[b.lineupRole]??2);return left-right||(a.shirtNo??999)-(b.shirtNo??999)||a.playerId-b.playerId})})
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getClubPlayers()).data}catch(e){error.value=e?.message||'加载球员失败，请稍后重试。'}finally{loading.value=false}}
const openCreate=()=>{editingId.value=null;returning.value=false;Object.assign(form,empty());visible.value=true}
const openAdjust=row=>{editingId.value=row.playerId;returning.value=false;Object.assign(form,empty(),row,{shirtNo:String(row.shirtNo??'')});visible.value=true}
const openReturn=row=>{editingId.value=row.playerId;returning.value=true;Object.assign(form,empty(),row,{shirtNo:''});visible.value=true}
const mutablePayload=()=>({shirtNo:Number(form.shirtNo),position:form.position,lineupRole:form.lineupRole})
const save=async()=>{if(playerAgeError.value)return;await formRef.value.validate();saving.value=true;try{if(!editingId.value){await ElMessageBox.confirm('球员姓名、出生年份和国籍添加后无法修改，请确认后再添加。','确认新增',{type:'warning'});await createClubPlayer({...mutablePayload(),playerName:form.playerName,birthYear:Number(form.birthYear),nationality:form.nationality,birthDate:null})}else if(returning.value){await returnClubPlayer(editingId.value,mutablePayload())}else{await updateClubPlayer(editingId.value,mutablePayload())}visible.value=false;ElMessage.success('保存成功');await load()}finally{saving.value=false}}
const requestRisk=(row,kind)=>{pendingAction.value={row,kind};confirmVisible.value=true}
const riskCopy=computed(()=>pendingAction.value?.kind==='cleanup'?{title:'清理离队球员',message:`确认清理离队球员「${pendingAction.value?.row.playerName||''}」？`,impact:'该操作用于清理离队记录，既有历史业务引用仍由系统规则处理。',confirm:'确认清理'}:{title:'确认球员离队',message:`确认将「${pendingAction.value?.row.playerName||''}」设为离队？`,impact:'球员将退出当前阵容，球衣号会立即释放；历史比赛和报名快照仍保留。',confirm:'确认离队'})
const executeRisk=async()=>{const action=pendingAction.value;if(!action)return;confirming.value=true;try{action.kind==='cleanup'?await cleanupClubPlayer(action.row.playerId):await updateClubPlayerStatus(action.row.playerId,'TRANSFERRED');ElMessage.success(action.kind==='cleanup'?'已清理':'已离队');confirmVisible.value=false;pendingAction.value=null;await load()}finally{confirming.value=false}}
onMounted(()=>{load();if(!systemTime.synced)systemTime.sync().catch(()=>{})})
</script>

<template>
  <div>
    <PageHeader title="球员管理" subtitle="维护球衣号码、场上位置与首发替补阵容。">
      <template #actions><el-button @click="organized=!organized">{{organized?'恢复添加顺序':'按阵容整理'}}</el-button><el-button type="primary" @click="openCreate">新增球员</el-button></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无球员" empty-description="新增球员后即可开始组建首发与替补阵容。" @retry="load">
      <TableWrapper title="一线队阵容" description="球衣号码、姓名和位置是阵容识别的主要信息。" label="球员列表">
        <el-table :data="displayed" row-key="playerId">
          <el-table-column label="号码" width="82" align="center"><template #default="{row}"><strong class="shirt-number score-nums">{{row.shirtNo??'—'}}</strong></template></el-table-column>
          <el-table-column prop="playerName" label="姓名" min-width="140" />
          <el-table-column label="位置" width="110"><template #default="{row}">{{positionLabel[row.position]||'—'}}</template></el-table-column>
          <el-table-column label="阵容角色" width="110"><template #default="{row}"><StatusTag v-if="row.playerStatus==='TRANSFERRED'" value="TRANSFERRED"/><StatusTag v-else :value="row.lineupRole" /></template></el-table-column>
          <el-table-column label="年龄" width="86"><template #default="{row}">{{row.age??(row.birthYear?currentYear-row.birthYear:'—')}}</template></el-table-column>
          <el-table-column prop="nationality" label="国籍" min-width="110" />
          <el-table-column label="状态" width="100"><template #default="{row}"><StatusTag :value="row.playerStatus==='TRANSFERRED'?'TRANSFERRED':'ACTIVE'" :label="row.playerStatus==='TRANSFERRED'?'已离队':'在队'" /></template></el-table-column>
          <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button v-if="row.playerStatus!=='TRANSFERRED'" link type="primary" @click="openAdjust(row)">调整</el-button><el-button v-else link type="primary" @click="openReturn(row)">归队</el-button><el-dropdown trigger="click"><el-button link>更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-if="row.playerStatus!=='TRANSFERRED'" @click="requestRisk(row,'leave')">离队</el-dropdown-item><el-dropdown-item v-else @click="requestRisk(row,'cleanup')">清理记录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
        </el-table>
      </TableWrapper>
    </DataState>
    <el-dialog v-model="visible" class="app-dialog" :title="!editingId?'新增球员':returning?'球员归队':'调整球员'" width="540px" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px"><el-form-item label="姓名" prop="playerName"><el-input v-model="form.playerName" name="player-name" autocomplete="off" :disabled="!!editingId"/></el-form-item><el-form-item label="出生年份" prop="birthYear" :error="playerAgeError"><el-input v-model="form.birthYear" name="player-birth-year" inputmode="numeric" autocomplete="off" :disabled="!!editingId"/></el-form-item><el-form-item label="国籍" prop="nationality"><el-input v-model="form.nationality" name="player-nationality" autocomplete="off" :disabled="!!editingId"/></el-form-item><el-form-item label="首发/替补" prop="lineupRole"><el-select v-model="form.lineupRole" aria-label="阵容角色"><el-option label="首发" value="STARTER"/><el-option label="替补" value="SUBSTITUTE"/></el-select></el-form-item><el-form-item label="球衣号" prop="shirtNo"><el-input v-model="form.shirtNo" name="player-shirt-number" inputmode="numeric" autocomplete="off" placeholder="1～99…"/></el-form-item><el-form-item label="场上位置" prop="position"><el-select v-model="form.position" aria-label="场上位置"><el-option v-for="p in positions" :key="p[0]" :label="p[1]" :value="p[0]"/></el-select></el-form-item></el-form>
      <template #footer><el-button :disabled="saving" @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="saving||!!playerAgeError" @click="save">保存</el-button></template>
    </el-dialog>
    <ConfirmDialog v-model="confirmVisible" :title="riskCopy.title" :message="riskCopy.message" :impact="riskCopy.impact" :confirm-text="riskCopy.confirm" danger :loading="confirming" @confirm="executeRisk" />
  </div>
</template>

<style scoped>
.shirt-number{display:inline-grid;min-width:34px;height:34px;padding:0 var(--space-2);border:1px solid var(--color-line-strong);border-radius:var(--radius-sm);background:var(--color-surface-subtle);font-size:var(--font-size-lg);place-items:center}
</style>
