<script setup>
import {onMounted,ref} from 'vue'
import {getOrders} from '../../api/order'
const loading=ref(false),error=ref(''),records=ref([]),total=ref(0),status=ref('PAID'),page=ref(1)
const load=async()=>{loading.value=true;error.value='';try{const data=(await getOrders({orderStatus:status.value||undefined,page:page.value,size:10})).data;records.value=data.records;total.value=data.total}catch(e){error.value=e?.message||'加载订单失败，请稍后重试。'}finally{loading.value=false}}
const changeStatus=()=>{page.value=1;load()}
onMounted(load)
</script>

<template><div><PageHeader title="我的订单" subtitle="查看待支付、已支付和退款订单。"/><section class="app-surface"><div class="order-tabs"><el-radio-group v-model="status" @change="changeStatus"><el-radio-button value="">全部</el-radio-button><el-radio-button value="PENDING_PAYMENT">待支付</el-radio-button><el-radio-button value="PAID">已支付</el-radio-button><el-radio-button value="CANCELLED">已取消</el-radio-button><el-radio-button value="REFUND_PENDING">退票中</el-radio-button><el-radio-button value="REFUNDED">已退票</el-radio-button></el-radio-group></div><DataState :loading="loading" :error="error" :empty="!records.length" empty-title="当前分类暂无订单" empty-description="完成购票后，订单会显示在这里。" @retry="load"><el-table :data="records"><el-table-column prop="orderNo" label="订单号" min-width="220"/><el-table-column label="比赛" min-width="190"><template #default="{row}">{{row.homeClubName}} VS {{row.awayClubName}}</template></el-table-column><el-table-column prop="zoneName" label="比赛票区"/><el-table-column prop="ticketCount" label="数量" width="70" align="right"/><el-table-column label="金额" width="120" align="right"><template #default="{row}"><span class="tabular-nums">{{$formatMoney(row.totalAmount)}}</span></template></el-table-column><el-table-column label="状态" width="120"><template #default="{row}"><StatusTag :value="row.orderStatus"/></template></el-table-column><el-table-column label="操作" width="90"><template #default="{row}"><RouterLink :to="`/user/orders/${row.orderId}`" class="el-button el-button--primary is-link">详情</RouterLink></template></el-table-column></el-table><el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load"/></DataState></section></div></template>

<style scoped>.order-tabs{display:flex;justify-content:flex-end;margin-bottom:var(--space-md)}</style>
