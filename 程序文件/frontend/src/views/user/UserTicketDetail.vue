<script setup>
import {onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getTicket} from '../../api/eTicket'
const route=useRoute(),router=useRouter(),ticket=ref({}),loading=ref(false)
onMounted(async()=>{loading.value=true;try{ticket.value=(await getTicket(route.params.id)).data}finally{loading.value=false}})
</script>
<template><el-card v-loading="loading"><template #header><el-page-header @back="router.push('/user/tickets')"><template #content>电子票详情</template></el-page-header></template><div v-if="ticket.ticketId" class="ticket"><el-alert v-if="ticket.ticketStatus==='USED'" :title="`已入场，入场时间：${$formatDateTime(ticket.enterTime)}`" type="success" :closable="false"/><el-alert v-else-if="ticket.ticketStatus==='REFUNDED'" title="该票已退票，不可入场" type="error" :closable="false"/><el-alert v-else-if="ticket.ticketStatus==='VOID'" title="票据已作废，不可入场" type="error" :closable="false"/><StatusTag :value="ticket.ticketStatus"/><h2>{{ticket.homeClubName}} vs {{ticket.awayClubName}}</h2><p>{{$formatDateTime(ticket.matchTime)}} · {{ticket.stadiumName}}</p><div class="seat">{{ticket.zoneName}}　{{ticket.rowNo}}排 {{ticket.seatNo}}座</div><div class="code">{{ticket.ticketCode}}</div><p class="hint">请妥善保管票码，入场时由检票员核验。</p></div><el-empty v-else-if="!loading" description="电子票不存在或无权查看"/></el-card></template>
<style scoped>.ticket{text-align:center;padding:28px}.seat{font-size:24px;font-weight:700;margin:24px}.code{display:inline-block;padding:18px 28px;border:2px dashed #166534;border-radius:8px;font-family:monospace;font-size:18px;letter-spacing:1px}.hint{color:#6b7280}</style>
