<script setup>
import {computed,onMounted,ref} from 'vue'
import {getClubSchedules} from '../../api/club'
import {formatDateTime} from '../../utils/format'

const rows=ref([]),loading=ref(false),expanded=ref([])
const groups=computed(()=>[
  {key:'upcoming',title:'未开始',rows:rows.value.filter(r=>!['IN_PROGRESS','FINISHED'].includes(r.matchStatus))},
  {key:'ongoing',title:'进行中',rows:rows.value.filter(r=>r.matchStatus==='IN_PROGRESS')},
  {key:'finished',title:'已结束',rows:rows.value.filter(r=>r.matchStatus==='FINISHED')},
])
const money=value=>`￥${Number(value||0).toFixed(2)}`
const load=async()=>{loading.value=true;try{rows.value=(await getClubSchedules()).data}finally{loading.value=false}}
onMounted(load)
</script>
<template><div v-loading="loading"><div class="page-head"><h2>已确认赛程</h2><el-button @click="load">刷新</el-button></div><el-card v-for="group in groups" :key="group.key" class="section"><template #header><b>{{group.title}} <span class="count">{{group.rows.length}}</span></b></template><div v-if="!group.rows.length" class="empty">暂无比赛</div><article v-for="row in group.rows" :key="row.matchId" class="match"><div class="identity"><el-avatar :size="50" :src="row.opponentLogoUrl||undefined">{{row.opponentClubName?.slice(0,1)}}</el-avatar><div><strong>{{row.home?'主场 vs ':'客场 vs '}}{{row.opponentClubName}}</strong><p>{{row.seasonName}} · 第{{row.roundNo}}轮 · {{formatDateTime(row.matchDateTime)}}</p><p>{{row.stadiumName}}</p></div></div><div v-if="row.matchStatus==='FINISHED'" class="score">{{row.ownScore}} : {{row.opponentScore}}</div><el-button link type="primary" @click="expanded.includes(row.matchId)?expanded=expanded.filter(id=>id!==row.matchId):expanded.push(row.matchId)">票务详情</el-button><router-link :to="`/club/matches/${row.matchId}/tickets`"><el-button link>进入票务</el-button></router-link><div v-if="expanded.includes(row.matchId)" class="ticketing"><span>已售 VIP：<b>{{row.soldVipCount||0}}</b></span><span>已售普通票：<b>{{row.soldNormalCount||0}}</b></span><span v-if="row.home">总营收：<b>{{money(row.totalRevenue)}}</b></span><span>本队收益（{{row.home?'60%':'30%'}}）：<b>{{money(row.clubRevenue)}}</b></span></div></article></el-card></div></template>
<style scoped>.page-head,.match{display:flex;align-items:center}.page-head{justify-content:space-between}.page-head h2{margin:0}.section{margin-top:16px}.count{color:#94a3b8;margin-left:6px}.empty{color:#94a3b8;text-align:center;padding:24px}.match{position:relative;gap:16px;padding:15px 0;border-bottom:1px solid #edf0f4;flex-wrap:wrap}.match:last-child{border-bottom:0}.identity{display:flex;align-items:center;gap:12px;min-width:360px;flex:1}.identity strong{font-size:17px}.identity p{margin:4px 0 0;color:#6b7280}.score{font-size:24px;font-weight:700}.ticketing{flex-basis:100%;display:flex;gap:30px;background:#f8fafc;padding:12px 16px;border-radius:8px}.ticketing span{color:#64748b}.ticketing b{color:#111827}</style>
