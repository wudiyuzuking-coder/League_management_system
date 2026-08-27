<script setup>
import {onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {getRounds,getSeason} from '../../api/league'
const route=useRoute(),router=useRouter(),season=ref({}),rows=ref([]),loading=ref(false)
onMounted(async()=>{loading.value=true;try{const [s,r]=await Promise.all([getSeason(route.params.id),getRounds(route.params.id)]);season.value=s.data;rows.value=r.data}finally{loading.value=false}})
</script>
<template><el-card v-loading="loading"><template #header><div class="head"><el-page-header @back="router.push('/user/seasons')"><template #content>{{season.seasonName}} · 轮次</template></el-page-header><el-button @click="router.push(`/user/seasons/${route.params.id}/standings`)">积分榜</el-button></div></template><el-table :data="rows" empty-text="暂无轮次"><el-table-column prop="roundNo" label="轮次" width="90"/><el-table-column prop="roundName" label="名称"/><el-table-column label="开始日期"><template #default="{row}">{{$formatDate(row.startDate)}}</template></el-table-column><el-table-column label="结束日期"><template #default="{row}">{{$formatDate(row.endDate)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.roundStatus"/></template></el-table-column></el-table></el-card></template>
<style scoped>.head{display:flex;align-items:center;justify-content:space-between}</style>
