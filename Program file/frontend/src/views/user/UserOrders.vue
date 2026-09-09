<script setup>
import {onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {getOrders} from '../../api/order'
const router=useRouter(),loading=ref(false),records=ref([]),total=ref(0),status=ref(''),page=ref(1)
const load=async()=>{loading.value=true;try{const d=(await getOrders({orderStatus:status.value||undefined,page:page.value,size:10})).data;records.value=d.records;total.value=d.total}finally{loading.value=false}}
const changeStatus=()=>{page.value=1;load()}
onMounted(load)
</script>
<template><el-card><template #header><div class="head"><b>我的订单</b><el-radio-group v-model="status" @change="changeStatus"><el-radio-button value="">全部</el-radio-button><el-radio-button value="PENDING_PAYMENT">待支付</el-radio-button><el-radio-button value="PAID">已支付</el-radio-button><el-radio-button value="CANCELLED">已取消</el-radio-button><el-radio-button value="REFUND_PENDING">退票中</el-radio-button><el-radio-button value="REFUNDED">已退票</el-radio-button></el-radio-group></div></template><el-table :data="records" v-loading="loading" empty-text="暂无订单"><el-table-column prop="orderNo" label="订单号" min-width="220"/><el-table-column label="比赛" min-width="190"><template #default="{row}">{{row.homeClubName}} vs {{row.awayClubName}}</template></el-table-column><el-table-column prop="zoneName" label="比赛票区"/><el-table-column prop="ticketCount" label="数量" width="70"/><el-table-column label="金额" width="110"><template #default="{row}">{{$formatMoney(row.totalAmount)}}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.orderStatus"/></template></el-table-column><el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="router.push(`/user/orders/${row.orderId}`)">详情</el-button></template></el-table-column></el-table><el-pagination v-if="total>10" v-model:current-page="page" :total="total" :page-size="10" layout="prev,pager,next" @current-change="load"/></el-card></template>
<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.el-pagination{margin-top:16px;justify-content:flex-end}</style>
