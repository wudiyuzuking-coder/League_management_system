<script setup>
import {computed,onMounted,reactive,ref,watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {getTicketZones,previewSeatAllocation,previewTicketTypeAllocation} from '../api/ticket'
import {createOrder} from '../api/order'
import {addPrefilledPassenger,getPrefilledPassengers} from '../api/passenger'
import {useSystemTimeStore} from '../stores/systemTime'
import {maskIdCard} from '../utils/privacy'

const props=defineProps({matchId:{type:[Number,String],required:true},match:{type:Object,default:()=>({})}})
const zones=ref([]),loading=ref(false),error=ref(''),counts=reactive({}),results=reactive({}),previewKey=ref('')
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
const previewOffering=computed(()=>offerings.value.find(item=>item.key===previewKey.value))
const previewResult=computed(()=>previewKey.value?results[previewKey.value]:null)
const previewTotal=computed(()=>Number(previewOffering.value?.price||0)*Number(counts[previewKey.value]||0))
const checkoutPreview=computed(()=>previewKey.value===currentOffering.value?.key?previewResult.value:null)
const checkoutTotal=computed(()=>Number(currentOffering.value?.price||0)*Number(counts[currentOffering.value?.key]||0))
const maskName=value=>{const text=String(value||'').trim();return text?`${text.slice(0,1)}${'*'.repeat(Math.max(1,text.length-1))}`:'—'}
const load=async()=>{if(!props.matchId)return;loading.value=true;error.value='';try{zones.value=(await getTicketZones(props.matchId)).data;offerings.value.forEach(z=>{const max=Math.max(1,z.maxPurchasableCount);if(!counts[z.key]||counts[z.key]>max)counts[z.key]=1})}catch(e){error.value=e?.message||'加载票务失败，请稍后重试。'}finally{loading.value=false}}
const check=async z=>{delete results[z.key];previewKey.value='';results[z.key]=(await (z.standard?previewTicketTypeAllocation(props.matchId,z.ticketType,counts[z.key]):previewSeatAllocation(z.matchZoneId,counts[z.key]))).data;previewKey.value=z.key}
const openPassengers=async z=>{currentOffering.value=z;selectedPassengers.value=[];passengers.value=(await getPrefilledPassengers()).data;passengerVisible.value=true}
const passengerSelection=rows=>{selectedPassengers.value=rows}
const addPassenger=async()=>{if(!passengerForm.passengerName.trim()||!/^\d{17}[\dXx]$/.test(passengerForm.idCardNo))return ElMessage.error('请填写姓名和正确的18位身份证号');addingPassenger.value=true;try{await addPrefilledPassenger(passengerForm);passengerForm.passengerName='';passengerForm.idCardNo='';passengers.value=(await getPrefilledPassengers()).data;ElMessage.success('购票人已添加')}finally{addingPassenger.value=false}}
const buy=async()=>{const z=currentOffering.value,count=counts[z.key];if(selectedPassengers.value.length!==count)return ElMessage.error(`购买${count}张票必须选择${count}名购票人`);await ElMessageBox.confirm(`确认购买${count}张“${z.label}”门票？系统将自动分配实际票区和座位。`,'确认购票',{type:'warning'});buying.value=z.key;try{const base={ticketCount:count,passengerIds:selectedPassengers.value.map(v=>v.prefilledPassengerId)};const selection=z.standard?{...base,matchId:Number(props.matchId),ticketType:z.ticketType}:{...base,matchZoneId:z.matchZoneId};const data=(await createOrder(selection)).data;passengerVisible.value=false;ElMessage.success('订单创建成功，座位已锁定');router.push(`/user/orders/${data.order.orderId}`)}finally{buying.value=null}}
const saleStateLabel=z=>({NOT_ENABLED:'未启用销售',NOT_STARTED:'未开售',AVAILABLE:'销售中',PAUSED:'暂停销售',CLOSED:'已关闭',ENDED:'已停售',SOLD_OUT:'已售罄',MATCH_UNAVAILABLE:'比赛暂不可售'}[z.saleState]||'暂不可购买')
watch(()=>props.matchId,load);watch(()=>systemTime.revision,load);onMounted(load)
</script>

<template>
  <section class="tickets">
    <div class="section-heading"><div><h2>选择票种与数量</h2><p>每单最多 4 张，实际方向票区由系统分配。</p></div></div>
    <DataState :loading="loading" :error="error" :empty="!offerings.length" empty-title="本场比赛暂未配置票务" empty-description="票务启用后可在这里选择票种。" @retry="load">
      <div class="ticket-offers">
        <article v-for="z in offerings" :key="z.key" class="ticket-offer" :class="{'ticket-offer--vip':z.ticketType==='VIP'||z.label==='VIP'}">
          <div class="ticket-offer__top"><div><span>赛事门票</span><h3>{{z.label}}</h3></div><StatusTag :value="z.saleState" :label="z.saleState==='AVAILABLE'?'售票中':undefined"/></div>
          <div class="ticket-offer__price tabular-nums"><strong>{{$formatMoney(z.price)}}</strong><span>/ 张</span></div>
          <div class="ticket-offer__inventory"><span>剩余</span><b class="tabular-nums">{{z.availableSeatCount}}</b><span>张</span></div>
          <p>东 / 西 / 南 / 北实际票区由系统按库存自动分配。</p>
          <el-alert v-if="z.saleState==='NOT_STARTED'" class="sale-time" type="warning" :closable="false" :title="`本场比赛将于 ${$formatDateTime(z.saleStartTime)} 开售`"/>
          <div class="ticket-offer__controls">
            <label :for="`ticket-count-${z.key}`">数量</label>
            <el-select :id="`ticket-count-${z.key}`" v-model="counts[z.key]" :disabled="z.maxPurchasableCount<1" aria-label="购票数量"><el-option v-for="n in z.maxPurchasableCount" :key="n" :label="`${n} 张`" :value="n"/></el-select>
            <el-button :disabled="!z.saleAvailable||z.maxPurchasableCount<1" @click="check(z)">预览座位</el-button>
            <el-button type="primary" :disabled="z.maxPurchasableCount<1||(!z.saleAvailable&&z.saleState!=='NOT_STARTED')" @click="openPassengers(z)">{{z.saleState==='NOT_STARTED'?'准备购票人':'选择购票人'}}</el-button>
          </div>
          <p v-if="counts[z.key]>z.maxContinuousCount" class="ticket-offer__warning" role="status">当前数量可能无法完全连坐，系统会尽量连续分配。</p>
        </article>
      </div>
      <section v-if="previewResult" class="seat-preview" aria-live="polite">
        <div class="section-heading"><div><h2>座位预览</h2><p>确认订单前核对票种、票区与座位。</p></div></div>
        <div class="seat-preview__body">
          <dl><div><dt>票种</dt><dd>{{previewOffering.label}}</dd></div><div><dt>实际票区</dt><dd>{{previewResult.zoneName}}</dd></div><div><dt>座位</dt><dd>{{previewResult.rowLabel}}，{{previewResult.seatLabels.join('、')}}</dd></div><div><dt>数量</dt><dd>{{counts[previewKey]}} 张</dd></div></dl>
          <div class="seat-preview__total"><span>合计</span><strong>{{$formatMoney(previewTotal)}}</strong><small>预览不会锁座，最终以创建订单时为准。</small></div>
        </div>
        <div class="seat-preview__actions"><el-button @click="previewKey=''">重新选择</el-button><el-button type="primary" @click="openPassengers(previewOffering)">选择购票人</el-button></div>
      </section>
    </DataState>
    <el-dialog v-model="passengerVisible" class="app-dialog" :title="`${currentOffering?.label||''} · 选择购票人`" width="680px">
      <CardShell title="订单确认" subtitle="创建订单前最后核对票种、座位和购票人。" variant="action" compact class="checkout-preview">
        <dl><div v-if="match.homeClubName"><dt>比赛</dt><dd>{{match.homeClubName}} VS {{match.awayClubName}}</dd></div><div><dt>票种</dt><dd>{{currentOffering?.label||'—'}}</dd></div><div v-if="checkoutPreview"><dt>实际票区</dt><dd>{{checkoutPreview.zoneName}}</dd></div><div v-if="checkoutPreview"><dt>座位</dt><dd>{{checkoutPreview.rowLabel}}，{{checkoutPreview.seatLabels.join('、')}}</dd></div><div><dt>购票人</dt><dd>{{selectedPassengers.length?selectedPassengers.map(item=>maskName(item.passengerName)).join('、'):'请选择购票人'}}</dd></div></dl>
        <div class="checkout-total"><span>{{counts[currentOffering?.key]||0}} 张</span><strong>{{$formatMoney(checkoutTotal)}}</strong></div>
      </CardShell>
      <p class="dialog-instruction">请选择 {{counts[currentOffering?.key]||0}} 名购票人，人数必须与门票数量一致。</p>
      <el-alert v-if="currentOffering?.saleState==='NOT_STARTED'" :title="`门票尚未开售，您仍可提前维护购票人；开售时间：${$formatDateTime(currentOffering.saleStartTime)}`" type="warning" :closable="false"/>
      <el-table :data="passengers" empty-text="暂无预填购票人" @selection-change="passengerSelection"><el-table-column type="selection" width="48"/><el-table-column prop="passengerName" label="姓名"/><el-table-column label="身份证号"><template #default="{row}">{{maskIdCard(row.idCardNo)}}</template></el-table-column></el-table>
      <el-divider content-position="left">直接新增购票人</el-divider>
      <div class="passenger-add"><el-input v-model="passengerForm.passengerName" name="passenger-name" autocomplete="off" placeholder="例如：张三…" aria-label="购票人姓名"/><el-input v-model="passengerForm.idCardNo" name="passenger-id-card" autocomplete="off" inputmode="text" maxlength="18" placeholder="18 位身份证号…" aria-label="购票人身份证号"/><el-button :disabled="passengers.length>=4" :loading="addingPassenger" @click="addPassenger">新增购票人</el-button></div>
      <template #footer><el-button @click="router.push('/user/passengers')">管理购票人</el-button><el-button @click="passengerVisible=false">取消</el-button><el-button v-if="currentOffering?.saleAvailable" type="primary" :loading="buying===currentOffering?.key" @click="buy">确认创建订单</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.tickets{margin-top:var(--space-xl)}.section-heading{display:flex;align-items:flex-start;gap:12px;margin-bottom:var(--space-md)}.section-heading h2{margin:0;font-size:22px}.section-heading p{margin:5px 0 0;color:var(--text-secondary)}
.ticket-offers{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-md)}.ticket-offer{padding:22px;border:1px solid var(--border-color);border-top:4px solid var(--color-pitch);border-radius:var(--radius-lg);background:var(--surface)}.ticket-offer--vip{border-top-color:var(--color-warning)}.ticket-offer__top{display:flex;align-items:flex-start;justify-content:space-between}.ticket-offer__top span{color:var(--text-muted);font-size:var(--font-size-xs)}.ticket-offer h3{margin:5px 0 0;font-size:22px}.ticket-offer__price{display:flex;align-items:baseline;gap:5px;margin:22px 0 10px}.ticket-offer__price strong{font-size:34px}.ticket-offer__price span,.ticket-offer>p{color:var(--text-muted)}.ticket-offer__inventory{display:flex;align-items:baseline;gap:6px}.ticket-offer__inventory b{font-size:20px}.sale-time{margin-top:var(--space-md)}.ticket-offer__controls{display:grid;grid-template-columns:auto 90px 1fr 1fr;align-items:center;gap:var(--space-sm);margin-top:var(--space-lg);padding-top:var(--space-md);border-top:1px solid var(--border-color)}.ticket-offer__warning{margin-bottom:0;color:var(--warning)!important;font-size:13px}
.seat-preview{margin-top:var(--space-lg);padding:22px;border:1px solid var(--color-success-border);border-radius:var(--radius-lg);background:var(--color-success-soft)}.seat-preview__body{display:grid;grid-template-columns:1fr 230px;gap:var(--space-lg)}.seat-preview dl{display:grid;grid-template-columns:repeat(2,1fr);gap:var(--space-md);margin:0}.seat-preview dl div{padding:12px;border-radius:var(--radius-sm);background:var(--surface)}.seat-preview dt{color:var(--text-muted);font-size:12px}.seat-preview dd{margin:5px 0 0;font-weight:700}.seat-preview__total{display:flex;align-items:flex-end;flex-direction:column;justify-content:center}.seat-preview__total span,.seat-preview__total small{color:var(--text-muted)}.seat-preview__total strong{margin:5px 0;font-size:30px}.seat-preview__actions{display:flex;justify-content:flex-end;gap:var(--space-sm);margin-top:var(--space-md)}
.checkout-preview{margin-bottom:var(--space-md)}.checkout-preview dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-3);margin:0}.checkout-preview dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.checkout-preview dd{margin:var(--space-1) 0 0;font-weight:var(--font-weight-semibold)}.checkout-total{display:flex;align-items:baseline;justify-content:flex-end;gap:var(--space-3);margin-top:var(--space-4);padding-top:var(--space-3);border-top:1px solid var(--color-line)}.checkout-total span{color:var(--color-text-muted)}.checkout-total strong{font-size:var(--font-size-2xl)}.dialog-instruction{color:var(--color-text-secondary);font-size:var(--font-size-sm)}.passenger-add{display:grid;grid-template-columns:1fr 1.8fr auto;gap:10px}
@media(max-width:760px){.ticket-offers{grid-template-columns:1fr}.ticket-offer__controls{grid-template-columns:auto 90px 1fr}.ticket-offer__controls .el-button--primary{grid-column:1/-1}.seat-preview__body{grid-template-columns:1fr}.checkout-preview dl{grid-template-columns:1fr}.passenger-add{grid-template-columns:1fr}}
</style>
