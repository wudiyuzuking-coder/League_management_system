<script setup>
import {computed,onMounted,ref} from 'vue'
import {getClubData} from '../../api/statistics'

const data=ref({revenue:0,performance:{},rankings:[]}),loading=ref(false),error=ref('')
const money=value=>`￥${Number(value||0).toFixed(2)}`
const metrics=computed(()=>{const p=data.value.performance||{};return[
  {label:'比赛',value:p.matchesPlayed||0},
  {label:'积分',value:p.points||0,tone:'success'},
  {label:'胜 / 平 / 负',value:`${p.wins||0} / ${p.draws||0} / ${p.losses||0}`},
  {label:'进球',value:p.goalsFor||0},
  {label:'失球',value:p.goalsAgainst||0},
  {label:'净胜球',value:p.goalDifference||0},
  {label:'收益',value:money(data.value.revenue),meta:'已结束有效比赛'},
]})
const rowClassName=({row})=>String(row.clubId)===String(data.value.performance?.clubId)?'current-club-row':''
const load=async()=>{loading.value=true;error.value='';try{data.value=(await getClubData()).data}catch(e){error.value=e?.message||'加载俱乐部数据失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="俱乐部数据" subtitle="查看球队成绩、积分排名和已结束比赛收益。" />
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
      <MetricStrip :items="metrics" label="俱乐部比赛指标" />
      <TableWrapper class="ranking" title="联赛排行榜" description="按积分与既有排名规则排序；当前俱乐部使用轻量强调。" label="俱乐部排行榜">
        <el-table :data="data.rankings" :row-class-name="rowClassName">
          <el-table-column type="index" label="排名" width="76" align="center"/>
          <el-table-column label="俱乐部" min-width="200"><template #default="{row}"><div class="club"><el-avatar :size="34" :src="row.logoUrl||undefined" :alt="`${row.clubName}队徽`">{{row.clubName?.slice(0,1)}}</el-avatar><strong>{{row.clubName}}</strong><span v-if="String(row.clubId)===String(data.performance?.clubId)">本队</span></div></template></el-table-column>
          <el-table-column prop="points" label="积分" width="90" align="center"><template #default="{row}"><strong class="score-nums">{{row.points}}</strong></template></el-table-column>
          <el-table-column prop="goalDifference" label="净胜球" width="100" align="center"/>
          <el-table-column prop="matchesPlayed" label="比赛" width="82" align="center"/>
          <el-table-column prop="wins" label="胜" width="72" align="center"/>
          <el-table-column prop="draws" label="平" width="72" align="center"/>
          <el-table-column prop="losses" label="负" width="72" align="center"/>
          <el-table-column prop="goalsFor" label="进球" width="82" align="center"/>
          <el-table-column prop="goalsAgainst" label="失球" width="82" align="center"/>
        </el-table>
      </TableWrapper>
    </DataState>
  </div>
</template>

<style scoped>.ranking{margin-top:var(--space-4)}.club{display:flex;align-items:center;gap:var(--space-3)}.club span{padding:2px var(--space-2);border-radius:var(--radius-full);color:var(--role-accent);background:var(--color-pitch-soft);font-size:var(--font-size-xs)}:deep(.current-club-row td.el-table__cell){background:var(--color-pitch-soft)!important}</style>
