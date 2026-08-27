<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {getMatch} from '../../api/match'
import {getZones} from '../../api/stadium'
import {createTicketZone,debugSeatAllocation,generateInventory,getAdminTicketZones,getInventoryLayout,updateInventoryStatus,updateTicketZone,updateTicketZoneStatus} from '../../api/ticket'

const route=useRoute(),router=useRouter(),matchId=Number(route.params.id)
const match=ref({}),staticZones=ref([]),zones=ref([]),selected=ref(null),layout=ref([]),loading=ref(false),saving=ref(false),operating=ref('')
const visible=ref(false),editingId=ref(null),formRef=ref(),debugCount=ref(2),debugResult=ref(null),debugging=ref(false)
const blank=()=>({stadiumZoneId:null,price:0,saleStartTime:'',saleEndTime:''}),form=reactive(blank())
const rules={stadiumZoneId:[{required:true,message:'请选择静态票区'}],price:[{required:true,type:'number',min:0,message:'票价不能小于0'}],saleStartTime:[{required:true,message:'请选择开售时间'}],saleEndTime:[{required:true,message:'请选择停售时间'}]}

const load=async()=>{loading.value=true;try{const [m,z]=await Promise.all([getMatch(matchId),getAdminTicketZones(matchId)]);match.value=m.data;zones.value=z.data;staticZones.value=(await getZones(match.value.stadiumId)).data;if(selected.value){selected.value=zones.value.find(v=>v.matchZoneId===selected.value.matchZoneId)||null;if(selected.value)await loadLayout()}}finally{loading.value=false}}
const open=row=>{editingId.value=row?.matchZoneId||null;Object.assign(form,blank(),row?{stadiumZoneId:row.stadiumZoneId,price:Number(row.price),saleStartTime:row.saleStartTime,saleEndTime:row.saleEndTime}:{});visible.value=true}
const save=async()=>{await formRef.value.validate();if(form.saleEndTime<=form.saleStartTime)return ElMessage.error('停售时间必须晚于开售时间');saving.value=true;try{editingId.value?await updateTicketZone(editingId.value,form):await createTicketZone(matchId,form);visible.value=false;ElMessage.success('比赛票区已保存');await load()}finally{saving.value=false}}
const nextActions=z=>({DRAFT:[['ON_SALE','开售'],['CLOSED','关闭']],ON_SALE:[['PAUSED','暂停'],['CLOSED','关闭']],PAUSED:[['ON_SALE','恢复'],['CLOSED','关闭']]}[z.zoneStatus]||[])
const transition=async(z,status,label)=>{await ElMessageBox.confirm(`确认${label}“${z.zoneName}”？`,'销售状态');operating.value=`status-${z.matchZoneId}`;try{await updateTicketZoneStatus(z.matchZoneId,status);ElMessage.success('销售状态已更新');await load()}finally{operating.value=''}}
const generate=async z=>{await ElMessageBox.confirm('库存将从当前ACTIVE物理座位生成，生成后不可重复生成。','生成库存');operating.value=`inventory-${z.matchZoneId}`;try{const r=await generateInventory(z.matchZoneId);ElMessage.success(`已生成 ${r.data} 个比赛座位库存`);await load()}finally{operating.value=''}}
const selectZone=async z=>{selected.value=z;debugResult.value=null;await loadLayout()}
const loadLayout=async()=>{layout.value=(await getInventoryLayout(selected.value.matchZoneId)).data}
const toggleSeat=async seat=>{if(!['AVAILABLE','DISABLED'].includes(seat.inventoryStatus))return;await updateInventoryStatus(seat.inventoryId,seat.inventoryStatus==='AVAILABLE'?'DISABLED':'AVAILABLE');await Promise.all([loadLayout(),load()]);debugResult.value=null;ElMessage.success('比赛座位状态已更新')}
const seatLetter=s=>({AVAILABLE:'O',LOCKED:'L',SOLD:'S',DISABLED:'X'}[s.inventoryStatus]||'?')
const seatType=s=>({AVAILABLE:'primary',LOCKED:'warning',SOLD:'success',DISABLED:'info'}[s.inventoryStatus]||'info')
const runDebug=async()=>{debugging.value=true;try{debugResult.value=(await debugSeatAllocation(selected.value.matchZoneId,debugCount.value)).data}finally{debugging.value=false}}
onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-page-header @back="router.push(`/admin/matches/${matchId}`)"><template #content>比赛票务配置</template></el-page-header>
    <el-alert class="notice" title="比赛库存是物理座位在本场比赛中的独立快照；连坐预览和算法调试均不会修改库存。" type="info" :closable="false"/>
    <el-card>
      <template #header><div class="head"><b>{{match.homeClubName}} vs {{match.awayClubName}}</b><el-button type="primary" :disabled="!['DRAFT','PUBLISHED'].includes(match.matchStatus)" @click="open()">新增比赛票区</el-button></div></template>
      <el-table :data="zones" highlight-current-row @current-change="selectZone">
        <el-table-column label="票区"><template #default="{row}">{{row.zoneName}}（{{row.zoneCode}}）</template></el-table-column>
        <el-table-column label="票价"><template #default="{row}">{{$formatMoney(row.price)}}</template></el-table-column>
        <el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.zoneStatus"/></template></el-table-column><el-table-column prop="availableSeatCount" label="可售"/><el-table-column prop="lockedSeatCount" label="锁定"/><el-table-column prop="soldSeatCount" label="已售"/><el-table-column prop="disabledSeatCount" label="停用"/><el-table-column prop="maxContinuousCount" label="最大连坐"/>
        <el-table-column label="操作" width="300"><template #default="{row}"><el-button v-if="row.zoneStatus==='DRAFT'" link type="primary" @click.stop="open(row)">编辑</el-button><el-button v-if="row.zoneStatus==='DRAFT'&&row.totalSeatCount===0" link type="success" :loading="operating===`inventory-${row.matchZoneId}`" @click.stop="generate(row)">生成库存</el-button><el-button v-for="a in nextActions(row)" :key="a[0]" link :type="a[0]==='CLOSED'?'danger':'primary'" :loading="operating===`status-${row.matchZoneId}`" @click.stop="transition(row,a[0],a[1])">{{a[1]}}</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-card v-if="selected" class="inventory">
      <template #header><div class="head"><b>{{selected.zoneName}} · 比赛库存预览</b><span>O=可售，L=锁定，S=已售，X=停用</span></div></template>
      <el-empty v-if="!layout.length" description="尚未生成库存"/>
      <div v-for="row in layout" :key="row.rowNo" class="seat-row"><b>{{row.rowLabel}}</b><el-button v-for="s in row.seats" :key="s.inventoryId" size="small" :type="seatType(s)" :plain="s.inventoryStatus==='AVAILABLE'" :disabled="['LOCKED','SOLD'].includes(s.inventoryStatus)" @click="toggleSeat(s)">{{seatLetter(s)}} {{s.seatLabel}}</el-button></div>
    </el-card>
    <el-card v-if="selected" class="debug">
      <template #header><b>连坐算法测试</b></template>
      <el-form inline><el-form-item label="购票张数"><el-select v-model="debugCount" style="width:100px"><el-option v-for="n in 4" :key="n" :label="`${n}张`" :value="n"/></el-select></el-form-item><el-button type="primary" :loading="debugging" @click="runDebug">计算候选</el-button></el-form>
      <el-alert v-if="debugResult?.best" type="success" :closable="false" :title="`最优：${debugResult.best.rowLabel}，${debugResult.best.seatLabels.join('、')}`"/>
      <el-alert v-else-if="debugResult" type="warning" :closable="false" :title="`无可用候选，当前最大连续数 ${debugResult.maxContinuousCount}`"/>
      <el-table v-if="debugResult" :data="debugResult.candidates" size="small"><el-table-column type="index" label="排名" width="65"/><el-table-column prop="rowLabel" label="排"/><el-table-column label="座位"><template #default="{row}">{{row.startSeatNo}} - {{row.endSeatNo}}</template></el-table-column><el-table-column prop="centerDistance" label="中线距离"/><el-table-column prop="remainingFragmentCount" label="剩余碎片"/><el-table-column prop="maxRemainingContinuousLength" label="最大剩余连续"/></el-table>
    </el-card>
  </div>
  <el-dialog v-model="visible" :title="editingId?'编辑比赛票区':'新增比赛票区'" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px"><el-form-item label="静态票区" prop="stadiumZoneId"><el-select v-model="form.stadiumZoneId" :disabled="editingId&&zones.find(v=>v.matchZoneId===editingId)?.totalSeatCount>0"><el-option v-for="z in staticZones" :key="z.stadiumZoneId" :label="`${z.zoneName}（${z.zoneCode}）`" :value="z.stadiumZoneId" :disabled="z.zoneStatus!=='ACTIVE'"/></el-select></el-form-item><el-form-item label="票价" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2"/></el-form-item><el-form-item label="开售时间" prop="saleStartTime"><el-date-picker v-model="form.saleStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"/></el-form-item><el-form-item label="停售时间" prop="saleEndTime"><el-date-picker v-model="form.saleEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"/></el-form-item></el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>.notice,.inventory,.debug{margin-top:16px}.head{display:flex;justify-content:space-between;align-items:center}.seat-row{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin:12px 0}.seat-row>b{width:55px}</style>
