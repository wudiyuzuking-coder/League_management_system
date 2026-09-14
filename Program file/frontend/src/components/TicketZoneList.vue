<script setup>
import {computed,onMounted,reactive,ref,watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {getTicketZones,previewSeatAllocation,previewTicketTypeAllocation} from '../api/ticket'
import {createOrder} from '../api/order'
import {addPrefilledPassenger,getPrefilledPassengers} from '../api/passenger'
import {useSystemTimeStore} from '../stores/systemTime'

const props=defineProps({matchId:{type:[Number,String],required:true}})
const zones=ref([]),loading=ref(false),counts=reactive({}),results=reactive({})
const router=useRouter(),buying=ref(null),systemTime=useSystemTimeStore()
const passengerVisible=ref(false),passengers=ref([]),selectedPassengers=ref([]),currentOffering=ref(null),addingPassenger=ref(false)
const passengerForm=reactive({passengerName:'',idCardNo:''})
const standardMode=computed(()=>zones.value.length>0&&zones.value.every(z=>z.ticketType&&z.zoneDirection))
const offerings=computed(()=>{
  if(!standardMode.value)return zones.value.map(z=>({...z,key:String(z.matchZoneId),label:z.zoneName,maxPurchasableCount:Math.min(4,Number(z.availableSeatCount||0))}))
  return ['VIP','NORMAL'].map(type=>{
    const actual=zones.value.filter(z=>z.ticketType===type)
    if(!actual.length)return null
    const available=actual.reduce((sum,z)=>sum+Number(z.availableSeatCount||0),0)
    // STANDARD_8 orders remain in a single actual direction zone.  A longest
    // contiguous run is informational only: the backend will allocate the best
    // available arrangement if a full run is unavailable.
    const carrying=actual.map(z=>Math.min(Number(z.availableSeatCount||0),4))
    const sale=actual.find(z=>z.saleAvailable)||actual.find(z=>z.saleState==='NOT_STARTED')||actual[0]
    return {...sale,key:type,label:type==='VIP'?'VIP':'普通',availableSeatCount:available,maxContinuousCount:Math.max(0,...actual.map(z=>Number(z.maxContinuousCount||0))),maxPurchasableCount:Math.max(0,...carrying),standard:true,ticketType:type}
  }).filter(Boolean)
})
const load=async()=>{if(!props.matchId)return;loading.value=true;try{zones.value=(await getTicketZones(props.matchId)).data;offerings.value.forEach(z=>{const max=Math.max(1,z.maxPurchasableCount);if(!counts[z.key]||counts[z.key]>max)counts[z.key]=1})}finally{loading.value=false}}
const check=async z=>{delete results[z.key];results[z.key]=(await (z.standard?previewTicketTypeAllocation(props.matchId,z.ticketType,counts[z.key]):previewSeatAllocation(z.matchZoneId,counts[z.key]))).data}
const openPassengers=async z=>{currentOffering.value=z;selectedPassengers.value=[];passengers.value=(await getPrefilledPassengers()).data;passengerVisible.value=true}
const passengerSelection=rows=>{selectedPassengers.value=rows}
const addPassenger=async()=>{if(!passengerForm.passengerName.trim()||!/^\d{17}[\dXx]$/.test(passengerForm.idCardNo))return ElMessage.error('请填写姓名和正确的18位身份证号');addingPassenger.value=true;try{await addPrefilledPassenger(passengerForm);passengerForm.passengerName='';passengerForm.idCardNo='';passengers.value=(await getPrefilledPassengers()).data;ElMessage.success('购票人已添加')}finally{addingPassenger.value=false}}
const buy=async()=>{const z=currentOffering.value,count=counts[z.key];if(selectedPassengers.value.length!==count)return ElMessage.error(`购买${count}张票必须选择${count}名购票人`);await ElMessageBox.confirm(`确认购买${count}张“${z.label}”门票？系统将自动分配实际票区和座位。`,'确认购票',{type:'warning'});buying.value=z.key;try{const base={ticketCount:count,passengerIds:selectedPassengers.value.map(v=>v.prefilledPassengerId)};const selection=z.standard?{...base,matchId:Number(props.matchId),ticketType:z.ticketType}:{...base,matchZoneId:z.matchZoneId};const data=(await createOrder(selection)).data;passengerVisible.value=false;ElMessage.success('订单创建成功，座位已锁定');router.push(`/user/orders/${data.order.orderId}`)}finally{buying.value=null}}
const saleStateLabel=z=>({NOT_ENABLED:'未启用销售',NOT_STARTED:'未开售',AVAILABLE:'销售中',PAUSED:'暂停销售',CLOSED:'已关闭',ENDED:'已停售',SOLD_OUT:'已售罄',MATCH_UNAVAILABLE:'比赛暂不可售'}[z.saleState]||'暂不可购买')
watch(()=>props.matchId,load);watch(()=>systemTime.revision,load);onMounted(load)
</script>

<template>
  <section class="tickets" v-loading="loading">
    <h3>票务信息</h3>
    <el-empty v-if="!offerings.length" description="本场比赛暂未配置票务"/>
    <el-row v-else :gutter="16">
      <el-col v-for="z in offerings" :key="z.key" :md="12">
        <el-card class="zone">
          <template #header><div class="head"><b>{{z.label}}</b><el-tag :type="z.saleAvailable?'success':'info'">{{saleStateLabel(z)}}</el-tag></div></template>
          <div class="price">￥{{Number(z.price).toFixed(2)}}</div>
          <div class="remaining">余票 <strong>{{z.availableSeatCount}}</strong></div>
          <el-alert v-if="z.saleState==='NOT_STARTED'" class="sale-time" type="warning" :closable="false" :title="`本场比赛将于 ${z.saleStartTime} 开售`"/>
          <div class="check-row">
            <el-select v-model="counts[z.key]" :disabled="z.maxPurchasableCount<1" style="width:92px"><el-option v-for="n in z.maxPurchasableCount" :key="n" :label="`${n}张`" :value="n"/></el-select>
            <el-button :disabled="!z.saleAvailable||z.maxPurchasableCount<1" @click="check(z)">预览座位</el-button>
            <el-button type="primary" :disabled="z.maxPurchasableCount<1||(!z.saleAvailable&&z.saleState!=='NOT_STARTED')" @click="openPassengers(z)">{{z.saleState==='NOT_STARTED'?'准备购票人':'选择购票人'}}</el-button>
          </div>
          <el-alert v-if="counts[z.key]>z.maxContinuousCount" class="result" type="warning" :closable="false" title="系统无法满足连坐需求，将为您尽量分配连坐座位"/>
          <el-alert v-else-if="results[z.key]" class="result" type="success" :closable="false" :title="`${results[z.key].zoneName}：${results[z.key].rowLabel}，${results[z.key].seatLabels.join('、')}`"/>
          <p class="hint">预览不会锁座；实际票区和座位以创建订单时为准。</p>
        </el-card>
      </el-col>
    </el-row>
    <el-dialog v-model="passengerVisible" :title="`${currentOffering?.label||''} · 选择购票人`" width="680px"><el-alert v-if="currentOffering?.saleState==='NOT_STARTED'" :title="`门票尚未开售，您仍可提前维护购票人；开售时间：${currentOffering.saleStartTime}`" type="warning" :closable="false"/><p>本次购买 {{counts[currentOffering?.key]||0}} 张，必须选择相同数量的购票人。</p><el-table :data="passengers" @selection-change="passengerSelection"><el-table-column type="selection" width="48"/><el-table-column prop="passengerName" label="姓名"/><el-table-column prop="idCardNo" label="身份证号"/></el-table><el-divider>直接新增购票人</el-divider><div class="passenger-add"><el-input v-model="passengerForm.passengerName" placeholder="姓名"/><el-input v-model="passengerForm.idCardNo" maxlength="18" placeholder="身份证号"/><el-button :disabled="passengers.length>=4" :loading="addingPassenger" @click="addPassenger">新增</el-button></div><template #footer><el-button @click="router.push('/user/passengers')">管理购票人</el-button><el-button @click="passengerVisible=false">关闭</el-button><el-button v-if="currentOffering?.saleAvailable" type="primary" :loading="buying===currentOffering?.key" @click="buy">确认创建订单</el-button></template></el-dialog>
  </section>
</template>

<style scoped>.tickets{margin-top:22px}.head{display:flex;justify-content:space-between;align-items:center}.zone{margin-bottom:16px}.price{font-size:32px;font-weight:700;color:#e65d2f;margin:8px 0}.remaining{font-size:17px;margin-bottom:14px}.sale-time{margin-bottom:14px}.check-row{display:flex;gap:10px}.check-row .el-button{flex:1}.result{margin-top:12px}.hint{font-size:12px;color:#909399;margin:10px 0 0}.passenger-add{display:grid;grid-template-columns:1fr 2fr auto;gap:10px}</style>
