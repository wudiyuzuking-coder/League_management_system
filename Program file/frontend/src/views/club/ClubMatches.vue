<script setup>
import {onMounted,reactive,ref} from 'vue'
import {useAuthStore} from '../../stores/auth'
import {getMatches} from '../../api/match'

const auth=useAuthStore(),rows=ref([]),total=ref(0),loading=ref(false),error=ref(''),query=reactive({page:1,size:10,clubId:auth.user?.clubId})
const load=async()=>{loading.value=true;error.value='';try{const r=(await getMatches(query)).data;rows.value=r.records;total.value=r.total}catch(e){error.value=e?.message||'加载比赛票务失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="比赛票务" subtitle="查看本俱乐部参与比赛的票务信息；页面不提供售票配置能力。" />
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无比赛票务" empty-description="已发布的俱乐部比赛会显示在这里。" @retry="load">
      <TableWrapper title="本俱乐部比赛" description="主场与客场比赛均只展示当前俱乐部有权查看的信息。" label="俱乐部比赛票务">
        <el-table :data="rows" row-key="matchId">
          <el-table-column prop="seasonName" label="赛季" min-width="160"/>
          <el-table-column prop="roundName" label="轮次" width="100"/>
          <el-table-column label="对阵" min-width="220"><template #default="{row}"><strong>{{row.homeClubName}}</strong><span class="versus">VS</span><strong>{{row.awayClubName}}</strong></template></el-table-column>
          <el-table-column label="时间" min-width="170"><template #default="{row}">{{$formatDateTime(row.matchTime)}}</template></el-table-column>
          <el-table-column prop="stadiumName" label="场馆" min-width="150" show-overflow-tooltip/>
          <el-table-column label="比分" width="90" align="center"><template #default="{row}"><strong class="score-nums">{{row.homeScore==null?'—':`${row.homeScore} : ${row.awayScore}`}}</strong></template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.matchStatus"/></template></el-table-column>
          <el-table-column label="操作" width="120" fixed="right"><template #default="{row}"><RouterLink :to="`/club/matches/${row.matchId}/tickets`" class="el-button el-button--primary is-link">票务详情</RouterLink></template></el-table-column>
        </el-table>
        <template #footer><el-pagination v-model:current-page="query.page" :total="total" :page-size="query.size" layout="total,prev,pager,next" @current-change="load"/></template>
      </TableWrapper>
    </DataState>
  </div>
</template>

<style scoped>.versus{margin:0 var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-xs)}</style>
