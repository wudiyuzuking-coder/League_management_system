<script setup>
import {onMounted,reactive,ref,watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {getTicketZones,previewSeatAllocation} from '../api/ticket'
import {createOrder} from '../api/order'

const props=defineProps({matchId:{type:[Number,String],required:true}})
const zones=ref([]),loading=ref(false),counts=reactive({}),results=reactive({})
const router=useRouter(),buying=ref(null)
const load=async()=>{if(!props.matchId)return;loading.value=true;try{zones.value=(await getTicketZones(props.matchId)).data;zones.value.forEach(z=>{if(!counts[z.matchZoneId])counts[z.matchZoneId]=1})}finally{loading.value=false}}
const check=async z=>{delete results[z.matchZoneId];results[z.matchZoneId]=(await previewSeatAllocation(z.matchZoneId,counts[z.matchZoneId])).data}
const buy=async z=>{await ElMessageBox.confirm(`确认购买${counts[z.matchZoneId]}张“${z.zoneName}”门票？座位将由系统重新分配。`,'确认购票',{type:'warning'});buying.value=z.matchZoneId;try{const data=(await createOrder(z.matchZoneId,counts[z.matchZoneId])).data;ElMessage.success('订单创建成功，座位已锁定');router.push(`/user/orders/${data.order.orderId}`)}finally{buying.value=null}}
watch(()=>props.matchId,load);onMounted(load)
</script>

<template>
  <section class="tickets" v-loading="loading">
    <h3>票务信息</h3>
    <el-empty v-if="!zones.length" description="本场比赛暂未配置票务"/>
    <el-row v-else :gutter="14">
      <el-col v-for="z in zones" :key="z.matchZoneId" :md="12" :lg="8">
        <el-card class="zone">
          <template #header><div class="head"><b>{{z.zoneName}}（{{z.zoneCode}}）</b><el-tag :type="z.saleAvailable?'success':'info'">{{z.zoneStatus}}</el-tag></div></template>
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="票价">￥{{Number(z.price).toFixed(2)}}</el-descriptions-item>
            <el-descriptions-item label="销售时间">{{z.saleStartTime}} 至 {{z.saleEndTime}}</el-descriptions-item>
            <el-descriptions-item label="余票">{{z.availableSeatCount}} / {{z.totalSeatCount}}</el-descriptions-item>
            <el-descriptions-item label="最大连坐数">{{z.maxContinuousCount}}</el-descriptions-item>
          </el-descriptions>
          <div class="check-row">
            <el-select v-model="counts[z.matchZoneId]" style="width:90px"><el-option v-for="n in 4" :key="n" :label="`${n}张`" :value="n"/></el-select>
            <el-button :disabled="!z.saleAvailable" @click="check(z)">检查连坐</el-button>
            <el-button type="primary" :loading="buying===z.matchZoneId" :disabled="!z.saleAvailable" @click="buy(z)">立即购票</el-button>
          </div>
          <el-alert v-if="results[z.matchZoneId]" class="result" type="success" :closable="false">
            <template #title>当前可满足{{results[z.matchZoneId].ticketCount}}张连坐：{{results[z.matchZoneId].rowLabel}}，{{results[z.matchZoneId].seatLabels.join('、')}}</template>
          </el-alert>
          <p class="hint">预览不会锁座；实际座位由创建订单时重新运行算法并正式锁定。</p>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>.tickets{margin-top:22px}.head{display:flex;justify-content:space-between;align-items:center}.zone{margin-bottom:14px}.check-row{display:flex;gap:10px;margin-top:12px}.check-row .el-button{flex:1}.result{margin-top:12px}.hint{font-size:12px;color:#909399;margin:10px 0 0}</style>
