<script setup>
import {computed,onBeforeUnmount,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {cancelOrder,getOrder,payOrder} from '../../api/order'
import {applyRefund} from '../../api/refund'
import {useSystemTimeStore} from '../../stores/systemTime'
import {confirmAction} from '../../utils/confirmAction'
import {maskIdCard} from '../../utils/privacy'

const route=useRoute(),systemTime=useSystemTimeStore()
const data=ref(null),loading=ref(false),error=ref(''),paying=ref(false),cancelling=ref(false),refunding=ref(false),refreshing=ref(false)
let timer
const order=computed(()=>data.value?.order||{})
const remaining=computed(()=>Math.max(0,new Date(order.value.expireTime||0).getTime()-systemTime.nowMs))
const countdown=computed(()=>{const s=Math.floor(remaining.value/1000);return `${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`})
const load=async()=>{loading.value=true;error.value='';try{data.value=(await getOrder(route.params.id)).data}catch(e){error.value=e?.message||'加载订单失败，请稍后重试。'}finally{loading.value=false}}
const cancel=async()=>{await confirmAction({title:'取消订单',message:'确认取消该待支付订单吗？',impact:'取消后已锁定的座位将立即释放，该订单无法继续支付。',confirmButtonText:'确认取消',danger:true});cancelling.value=true;try{data.value=(await cancelOrder(route.params.id)).data;ElMessage.success('订单已取消，座位已经释放')}finally{cancelling.value=false}}
const pay=async result=>{paying.value=true;try{const r=(await payOrder(route.params.id,result)).data;data.value=r.orderDetail;if(result==='SUCCESS')ElMessage.success(r.idempotent?'订单已经支付，请勿重复操作':'模拟支付成功，电子票已生成');else ElMessage.warning('模拟支付失败，可在订单有效期内重试')}finally{paying.value=false}}
const refund=async()=>{const {value}=await ElMessageBox.prompt('本系统仅支持整单退票；系统将按比赛时间自动计算100%或50%退款，须在比赛开始24小时前申请。请输入原因：','申请退票',{inputType:'textarea',inputValidator:v=>v?.trim()?true:'请输入退票原因'});refunding.value=true;try{await applyRefund(route.params.id,value.trim());await load();ElMessage.success('退款已自动处理，可在本订单查看退款金额与手续费')}finally{refunding.value=false}}
onMounted(async()=>{await Promise.all([load(),systemTime.sync()]);timer=setInterval(async()=>{systemTime.tick();if(order.value.orderStatus==='PENDING_PAYMENT'&&remaining.value===0&&!refreshing.value){refreshing.value=true;try{await load()}finally{refreshing.value=false}}},1000)})
onBeforeUnmount(()=>clearInterval(timer))
</script>

<template>
  <div class="order-page">
    <PageHeader title="订单详情" subtitle="核对比赛、座位、购票人与支付状态。" :breadcrumb="[{label:'我的订单',to:'/user/orders'},{label:order.orderNo||'订单详情'}]">
      <template #status><StatusTag v-if="order.orderStatus" :value="order.orderStatus"/></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!data" empty-title="订单不存在或无权查看" @retry="load">
      <el-alert v-if="order.orderStatus==='PENDING_PAYMENT'" type="warning" :closable="false" :title="remaining?`支付剩余时间 ${countdown}`:'订单正在关闭，请稍候刷新'"/>
      <el-alert v-if="order.orderStatus==='REFUND_PENDING'" title="历史退款处理中，座位和电子票暂时保留" type="warning" :closable="false"/>
      <section class="order-summary">
        <div class="order-summary__status"><span>订单状态</span><h2>{{$statusLabel(order.orderStatus)}}</h2><small>订单号 {{order.orderNo}}</small></div>
        <div class="order-summary__match"><span>比赛</span><h2>{{order.homeClubName}} <em>VS</em> {{order.awayClubName}}</h2><p>{{$formatDateTime(order.matchTime)}} · {{order.stadiumName}}</p></div>
        <div class="order-summary__amount"><span>订单总额</span><strong class="tabular-nums">{{$formatMoney(order.totalAmount)}}</strong><small>{{order.ticketCount}} 张门票</small></div>
      </section>
      <section class="order-content">
        <div class="order-content__main">
          <div class="content-section"><div class="content-section__heading"><span>01</span><h2>座位与购票人</h2></div><el-table :data="data.items" empty-text="暂无座位明细"><el-table-column prop="passengerName" label="购票人"/><el-table-column label="身份证号" min-width="190"><template #default="{row}">{{maskIdCard(row.passengerIdCard)}}</template></el-table-column><el-table-column prop="rowLabel" label="排"/><el-table-column prop="seatLabel" label="座位"/><el-table-column label="单价" align="right"><template #default="{row}"><span class="tabular-nums">{{$formatMoney(row.unitPrice)}}</span></template></el-table-column><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.itemStatus"/></template></el-table-column></el-table></div>
          <div v-if="data.tickets?.length" class="content-section"><div class="content-section__heading"><span>02</span><h2>电子票</h2></div><div class="ticket-links"><RouterLink v-for="ticket in data.tickets" :key="ticket.ticketId" :to="`/user/tickets/${ticket.ticketId}`" class="ticket-link"><span>{{ticket.passengerName}}</span><b>{{ticket.rowNo}}排 {{ticket.seatNo}}座</b><em>查看电子票</em></RouterLink></div></div>
        </div>
        <aside class="order-meta"><h2>订单信息</h2><dl><div><dt>票种 / 票区</dt><dd>{{order.zoneName}}</dd></div><div v-if="order.paidAt"><dt>支付时间</dt><dd>{{$formatDateTime(order.paidAt)}}</dd></div><div v-if="order.cancelReason"><dt>取消原因</dt><dd>{{$statusLabel(order.cancelReason)}}</dd></div></dl>
          <div v-if="order.orderStatus==='PENDING_PAYMENT'" class="demo-payment"><span>演示支付</span><p>仅用于系统答辩演示，不代表真实支付渠道。</p><el-button type="primary" :loading="paying" :disabled="!remaining||cancelling" @click="pay('SUCCESS')">模拟支付成功</el-button><el-button :loading="paying" :disabled="!remaining||cancelling" @click="pay('FAILED')">模拟支付失败</el-button><el-button type="danger" plain :loading="cancelling" :disabled="paying" @click="cancel">取消订单</el-button></div>
          <el-button v-if="order.orderStatus==='PAID'" class="refund-action" text type="danger" :loading="refunding" @click="refund">申请整单退票</el-button>
        </aside>
      </section>
      <el-descriptions v-if="data.refund" :column="2" border class="details">
        <el-descriptions-item label="退款编号">{{data.refund.refundNo}}</el-descriptions-item><el-descriptions-item label="退款状态"><StatusTag :value="data.refund.refundStatus"/></el-descriptions-item>
        <el-descriptions-item label="退款金额">{{$formatMoney(data.refund.refundAmount)}}</el-descriptions-item><el-descriptions-item label="手续费">{{$formatMoney(data.refund.feeAmount)}}</el-descriptions-item>
        <el-descriptions-item label="退款比例">{{Number(data.refund.refundRate||0)*100}}%</el-descriptions-item><el-descriptions-item label="处理方式">{{data.refund.processingMode==='AUTO'?'系统自动':'历史人工'}}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{$formatDateTime(data.refund.appliedAt)}}</el-descriptions-item><el-descriptions-item label="退款原因">{{data.refund.reason}}</el-descriptions-item>
        <el-descriptions-item v-if="data.refund.auditReason" label="历史审核结果" :span="2">{{data.refund.auditReason}}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="data.payment" :column="2" border class="details"><el-descriptions-item label="支付流水">{{data.payment.paymentNo}}</el-descriptions-item><el-descriptions-item label="支付状态"><StatusTag :value="data.payment.payStatus"/></el-descriptions-item></el-descriptions>
    </DataState>
  </div>
</template>

<style scoped>
.order-summary{display:grid;grid-template-columns:220px 1fr 220px;align-items:center;gap:var(--space-lg);margin-bottom:var(--space-lg);padding:24px;border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface)}.order-summary span,.order-summary small,.order-meta dt{color:var(--text-muted);font-size:12px}.order-summary h2{margin:6px 0;font-size:20px}.order-summary__status{padding-right:var(--space-lg);border-right:1px solid var(--border-color)}.order-summary__match h2 em{margin:0 8px;color:var(--primary);font-size:13px;font-style:normal}.order-summary__match p{margin:8px 0 0;color:var(--text-secondary)}.order-summary__amount{display:flex;align-items:flex-end;flex-direction:column}.order-summary__amount strong{margin:5px 0;font-size:30px}
.order-content{display:grid;grid-template-columns:minmax(0,1fr) 300px;align-items:start;gap:var(--space-lg)}.order-content__main,.order-meta{border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface)}.content-section{padding:22px}.content-section+.content-section{border-top:1px solid var(--border-color)}.content-section__heading{display:flex;align-items:center;gap:10px;margin-bottom:var(--space-md)}.content-section__heading span{color:var(--primary);font-size:11px;font-weight:800}.content-section__heading h2,.order-meta h2{margin:0;font-size:18px}
.order-meta{padding:22px}.order-meta dl{margin:18px 0}.order-meta dl div{padding:12px 0;border-bottom:1px solid var(--border-color)}.order-meta dd{margin:5px 0 0;font-weight:650}.demo-payment{display:flex;flex-direction:column;gap:8px;margin-top:var(--space-lg);padding:16px;border-radius:var(--radius-md);background:var(--surface-muted)}.demo-payment>span{color:var(--warning);font-weight:800}.demo-payment p{margin:0 0 6px;color:var(--text-muted);font-size:12px;line-height:1.5}.refund-action{margin-top:var(--space-md)}
.ticket-links{display:grid;grid-template-columns:repeat(2,1fr);gap:var(--space-sm)}.ticket-link{display:grid;grid-template-columns:1fr auto;gap:5px;padding:14px;border:1px solid var(--border-color);border-radius:var(--radius-md)}.ticket-link em{grid-column:1/-1;color:var(--primary);font-size:12px;font-style:normal}.ticket-link:hover{border-color:var(--primary);background:var(--surface-muted)}.details{margin:var(--space-lg) 0}
</style>
