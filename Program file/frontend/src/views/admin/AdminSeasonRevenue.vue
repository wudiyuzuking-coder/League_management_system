<script setup>
import {onMounted,ref} from 'vue';import {getAdminSeasons} from '../../api/league';import {getSeasonRevenue} from '../../api/statistics'
const seasons=ref([]),seasonId=ref(),data=ref({}),loading=ref(false),money=v=>`￥${Number(v||0).toFixed(2)}`
const load=async()=>{if(!seasonId.value)return;loading.value=true;try{data.value=(await getSeasonRevenue(seasonId.value)).data}finally{loading.value=false}}
onMounted(async()=>{seasons.value=(await getAdminSeasons()).data;if(seasons.value.length){seasonId.value=seasons.value[0].seasonId;await load()}})
</script>
<template><el-card v-loading="loading"><template #header><div class="head"><b>赛季营收</b><el-select v-model="seasonId" style="width:240px" @change="load"><el-option v-for="s in seasons" :key="s.seasonId" :label="s.seasonName" :value="s.seasonId"/></el-select></div></template><el-descriptions :column="2" border><el-descriptions-item label="赛季总有效票务营收">{{money(data.effectiveRevenue)}}</el-descriptions-item><el-descriptions-item label="平台分成（10%）">{{money(data.platformShare)}}</el-descriptions-item></el-descriptions><el-alert class="tip" title="仅统计当前仍为 PAID 的订单明细；已退款明细不计入。" type="info" :closable="false"/></el-card></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}.tip{margin-top:16px}</style>
