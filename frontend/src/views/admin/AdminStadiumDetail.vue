<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {batchCreateSeats,createSeat,createZone,getAdminStadium,getCapacitySummary,getLayout,getZones,updateSeat,updateSeatStatus,updateZone,updateZoneStatus} from '../../api/stadium'

const route=useRoute(),router=useRouter(),stadiumId=Number(route.params.id)
const stadium=ref({}),capacity=ref({}),zones=ref([]),selectedZone=ref(null),layout=ref([]),loading=ref(false),saving=ref(false)
const zoneVisible=ref(false),zoneId=ref(null),zoneRef=ref(),seatVisible=ref(false),seatId=ref(null),seatRef=ref(),batchVisible=ref(false),batchRef=ref()
const blankZone=()=>({zoneCode:'',zoneName:'',sortNo:0,description:''}),zone=reactive(blankZone())
const blankSeat=()=>({rowNo:1,rowLabel:'A',seatNo:1,seatLabel:'1'}),seat=reactive(blankSeat())
const batch=reactive({rows:[{rowNo:1,rowLabel:'A',startSeatNo:1,seatCount:10}]})
const zoneRules={zoneCode:[{required:true,message:'请输入票区编码'}],zoneName:[{required:true,message:'请输入票区名称'}],sortNo:[{required:true,type:'number',min:0,message:'排序号不能小于0'}]}
const seatRules={rowNo:[{required:true,type:'number',min:1,message:'排序号必须大于0'}],rowLabel:[{required:true,message:'请输入排标签'}],seatNo:[{required:true,type:'number',min:1,message:'座位序号必须大于0'}],seatLabel:[{required:true,message:'请输入座位标签'}]}
const loadBase=async()=>{loading.value=true;try{const [s,c,z]=await Promise.all([getAdminStadium(stadiumId),getCapacitySummary(stadiumId),getZones(stadiumId)]);stadium.value=s.data;capacity.value=c.data;zones.value=z.data;if(selectedZone.value){selectedZone.value=zones.value.find(v=>v.stadiumZoneId===selectedZone.value.stadiumZoneId)||null;if(selectedZone.value)await loadLayout()}}finally{loading.value=false}}
const chooseZone=async row=>{selectedZone.value=row;await loadLayout()}
const loadLayout=async()=>{layout.value=selectedZone.value?(await getLayout(selectedZone.value.stadiumZoneId)).data:[]}
const openZone=row=>{zoneId.value=row?.stadiumZoneId||null;Object.assign(zone,blankZone(),row||{});zoneVisible.value=true}
const saveZone=async()=>{if(saving.value)return;await zoneRef.value.validate();saving.value=true;try{zoneId.value?await updateZone(zoneId.value,zone):await createZone(stadiumId,zone);zoneVisible.value=false;ElMessage.success('票区已保存');await loadBase()}finally{saving.value=false}}
const toggleZone=async row=>{await updateZoneStatus(row.stadiumZoneId,row.zoneStatus==='ACTIVE'?'DISABLED':'ACTIVE');ElMessage.success('票区状态已更新');await loadBase()}
const openSeat=value=>{seatId.value=value?.stadiumSeatId||null;Object.assign(seat,blankSeat(),value||{});seatVisible.value=true}
const saveSeat=async()=>{if(saving.value)return;await seatRef.value.validate();saving.value=true;try{seatId.value?await updateSeat(seatId.value,seat):await createSeat(selectedZone.value.stadiumZoneId,seat);seatVisible.value=false;ElMessage.success('座位已保存');await Promise.all([loadLayout(),refreshCapacity()])}finally{saving.value=false}}
const toggleSeat=async value=>{await updateSeatStatus(value.stadiumSeatId,value.seatStatus==='ACTIVE'?'DISABLED':'ACTIVE');ElMessage.success('座位状态已更新');await Promise.all([loadLayout(),refreshCapacity()])}
const refreshCapacity=async()=>{capacity.value=(await getCapacitySummary(stadiumId)).data}
const addBatchRow=()=>batch.rows.push({rowNo:batch.rows.length+1,rowLabel:String.fromCharCode(65+batch.rows.length),startSeatNo:1,seatCount:10})
const openBatch=()=>{batch.rows.splice(0,batch.rows.length,{rowNo:1,rowLabel:'A',startSeatNo:1,seatCount:10});batchVisible.value=true}
const saveBatch=async()=>{if(saving.value)return;await batchRef.value.validate();saving.value=true;try{const r=await batchCreateSeats(selectedZone.value.stadiumZoneId,batch);batchVisible.value=false;ElMessage.success(`已创建 ${r.data} 个座位`);await Promise.all([loadLayout(),refreshCapacity()])}finally{saving.value=false}}
onMounted(loadBase)
</script>

<template>
  <div v-loading="loading">
    <el-page-header @back="router.push('/admin/stadiums')"><template #content>{{stadium.stadiumName||'场馆详情'}}</template></el-page-header>
    <el-row :gutter="12" class="summary">
      <el-col :span="6"><el-statistic title="申报容量" :value="capacity.declaredCapacity||0"/></el-col>
      <el-col :span="6"><el-statistic title="已建座位" :value="capacity.totalSeatCount||0"/></el-col>
      <el-col :span="6"><el-statistic title="启用座位" :value="capacity.activeSeatCount||0"/></el-col>
      <el-col :span="6"><el-statistic title="停用座位" :value="capacity.disabledSeatCount||0"/></el-col>
    </el-row>
    <el-alert title="申报容量用于场馆资料展示；已建座位数由静态物理座位实时统计，两者不强制相等。" type="info" :closable="false"/>
    <el-card class="section">
      <template #header><div class="head"><b>静态票区</b><el-button type="primary" @click="openZone()">新增票区</el-button></div></template>
      <el-table :data="zones" highlight-current-row @current-change="chooseZone">
        <el-table-column prop="zoneCode" label="编码"/><el-table-column prop="zoneName" label="名称"/><el-table-column prop="sortNo" label="排序"/>
        <el-table-column prop="description" label="说明" min-width="180"/>
        <el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.zoneStatus"/></template></el-table-column>
        <el-table-column label="操作" width="150"><template #default="{row}"><el-button link type="primary" @click.stop="openZone(row)">编辑</el-button><el-button link :type="row.zoneStatus==='ACTIVE'?'warning':'success'" @click.stop="toggleZone(row)">{{row.zoneStatus==='ACTIVE'?'停用':'启用'}}</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-card v-if="selectedZone" class="section">
      <template #header><div class="head"><b>{{selectedZone.zoneName}} · 物理座位布局</b><div><el-button @click="openSeat()">单个新增</el-button><el-button type="primary" @click="openBatch">按排批量生成</el-button></div></div></template>
      <el-empty v-if="!layout.length" description="该票区尚未创建座位"/>
      <div v-for="row in layout" :key="row.rowNo" class="seat-row"><span class="row-label">{{row.rowLabel}} 排</span><el-button v-for="item in row.seats" :key="item.stadiumSeatId" size="small" :type="item.seatStatus==='ACTIVE'?'primary':'info'" :plain="item.seatStatus==='ACTIVE'" @click="openSeat(item)">{{item.seatLabel}}</el-button></div>
      <p v-if="layout.length" class="tip">点击座位可编辑排号和座号；座位启停在编辑窗口中操作。</p>
    </el-card>
  </div>

  <el-dialog v-model="zoneVisible" :title="zoneId?'编辑票区':'新增票区'" width="520px"><el-form ref="zoneRef" :model="zone" :rules="zoneRules" label-width="90px"><el-form-item label="票区编码" prop="zoneCode"><el-input v-model="zone.zoneCode"/></el-form-item><el-form-item label="票区名称" prop="zoneName"><el-input v-model="zone.zoneName"/></el-form-item><el-form-item label="排序号" prop="sortNo"><el-input-number v-model="zone.sortNo" :min="0"/></el-form-item><el-form-item label="说明"><el-input v-model="zone.description" type="textarea"/></el-form-item></el-form><template #footer><el-button @click="zoneVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveZone">保存</el-button></template></el-dialog>
  <el-dialog v-model="seatVisible" :title="seatId?'编辑座位':'新增座位'" width="500px"><el-form ref="seatRef" :model="seat" :rules="seatRules" label-width="100px"><el-form-item label="排排序号" prop="rowNo"><el-input-number v-model="seat.rowNo" :min="1"/></el-form-item><el-form-item label="排标签" prop="rowLabel"><el-input v-model="seat.rowLabel"/></el-form-item><el-form-item label="座位排序号" prop="seatNo"><el-input-number v-model="seat.seatNo" :min="1"/></el-form-item><el-form-item label="座位标签" prop="seatLabel"><el-input v-model="seat.seatLabel"/></el-form-item></el-form><template #footer><el-button v-if="seatId" :type="seat.seatStatus==='ACTIVE'?'warning':'success'" @click="toggleSeat(seat);seatVisible=false">{{seat.seatStatus==='ACTIVE'?'停用':'启用'}}</el-button><el-button @click="seatVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveSeat">保存</el-button></template></el-dialog>
  <el-dialog v-model="batchVisible" title="按排批量生成座位" width="720px"><el-alert title="每排可设置不同座位数。提交前会整体校验；任一座位冲突则整批不写入。" type="info" :closable="false"/><el-form ref="batchRef" :model="batch"><div v-for="(row,index) in batch.rows" :key="index" class="batch-row"><el-form-item :prop="`rows.${index}.rowNo`" :rules="{required:true,type:'number',min:1,message:'必填'}"><el-input-number v-model="row.rowNo" :min="1" controls-position="right"/><small>排序号</small></el-form-item><el-form-item :prop="`rows.${index}.rowLabel`" :rules="{required:true,message:'必填'}"><el-input v-model="row.rowLabel"/><small>排标签</small></el-form-item><el-form-item :prop="`rows.${index}.startSeatNo`" :rules="{required:true,type:'number',min:1,message:'必填'}"><el-input-number v-model="row.startSeatNo" :min="1" controls-position="right"/><small>起始座号</small></el-form-item><el-form-item :prop="`rows.${index}.seatCount`" :rules="{required:true,type:'number',min:1,message:'必填'}"><el-input-number v-model="row.seatCount" :min="1" controls-position="right"/><small>座位数</small></el-form-item><el-button v-if="batch.rows.length>1" link type="danger" @click="batch.rows.splice(index,1)">移除</el-button></div></el-form><el-button @click="addBatchRow">增加一排</el-button><template #footer><el-button @click="batchVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveBatch">生成</el-button></template></el-dialog>
</template>

<style scoped>.summary{margin:20px 0}.summary .el-col{padding:16px;background:#fff;border-right:1px solid #eee}.section{margin-top:16px}.head{display:flex;justify-content:space-between;align-items:center}.seat-row{display:flex;gap:8px;align-items:center;margin:12px 0;flex-wrap:wrap}.row-label{width:60px;font-weight:600}.tip{color:#909399;font-size:13px}.batch-row{display:grid;grid-template-columns:1fr 1fr 1fr 1fr auto;gap:10px;align-items:flex-start;margin-top:16px}.batch-row small{display:block;color:#909399}.batch-row .el-form-item{margin-bottom:16px}</style>
