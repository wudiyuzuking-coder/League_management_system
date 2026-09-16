<script setup>
import {onMounted,ref} from 'vue'
import {getOrders} from '../../api/order'

const loading=ref(false),error=ref(''),records=ref([]),total=ref(0),status=ref('PAID'),page=ref(1)
const load=async()=>{loading.value=true;error.value='';try{const data=(await getOrders({orderStatus:status.value||undefined,page:page.value,size:10})).data;records.value=data.records;total.value=data.total}catch(e){error.value=e?.message||'加载订单失败，请稍后重试。'}finally{loading.value=false}}
const changeStatus=()=>{page.value=1;load()}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="我的订单" subtitle="查看购票订单、支付状态和退款进度。" />
    <ActionToolbar title="订单状态" description="按当前处理状态筛选订单。">
      <template #actions><el-radio-group v-model="status" @change="changeStatus"><el-radio-button value="">全部</el-radio-button><el-radio-button value="PENDING_PAYMENT">待支付</el-radio-button><el-radio-button value="PAID">已支付</el-radio-button><el-radio-button value="CANCELLED">已取消</el-radio-button><el-radio-button value="REFUND_PENDING">退票中</el-radio-button><el-radio-button value="REFUNDED">已退票</el-radio-button></el-radio-group></template>
    </ActionToolbar>
    <DataState class="orders-state" :loading="loading" :error="error" :empty="!records.length" empty-title="暂无订单" empty-description="购买比赛门票后，订单会显示在这里。" @retry="load">
      <div class="order-list">
        <CardShell v-for="row in records" :key="row.orderId" variant="fixture" compact class="order-card">
          <template #header><div class="order-card__header"><span>订单 {{row.orderNo}}</span><StatusTag :value="row.orderStatus"/></div></template>
          <div class="order-card__body"><div><h2>{{row.homeClubName}} <span>VS</span> {{row.awayClubName}}</h2><p>{{$formatDateTime(row.matchTime)}}<template v-if="row.stadiumName"> · {{row.stadiumName}}</template></p><small>{{row.zoneName||'比赛门票'}} · {{row.ticketCount||0}} 张</small></div><strong class="order-card__amount score-nums">{{$formatMoney(row.totalAmount)}}</strong></div>
          <template #footer><div class="order-card__footer"><span>{{$statusLabel(row.orderStatus)}}</span><RouterLink :to="`/user/orders/${row.orderId}`" class="el-button el-button--primary">查看订单</RouterLink></div></template>
        </CardShell>
      </div>
      <el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load"/>
    </DataState>
  </div>
</template>

<style scoped>.orders-state{margin-top:var(--space-4)}.order-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4)}.order-card__header{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3);width:100%;color:var(--color-text-muted);font-size:var(--font-size-xs)}.order-card__body{display:flex;align-items:center;justify-content:space-between;gap:var(--space-5)}.order-card__body h2{margin:0;font-size:var(--font-size-lg)}.order-card__body h2 span{margin:0 var(--space-1);color:var(--role-accent);font-size:var(--font-size-xs)}.order-card__body p{margin:var(--space-2) 0;color:var(--color-text-secondary)}.order-card__body small{color:var(--color-text-muted)}.order-card__amount{flex:none;font-size:var(--font-size-2xl)}.order-card__footer{display:flex;align-items:center;justify-content:space-between;width:100%;color:var(--color-text-muted);font-size:var(--font-size-sm)}@media(max-width:860px){.order-list{grid-template-columns:1fr}}</style>
