<script setup>
import {onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getSeason,getStandings} from '../../api/league'

const route=useRoute(),season=ref({}),rows=ref([]),loading=ref(false),error=ref('')
const rowClassName=({row})=>Number(row.rank)<=3?`podium-row podium-row--${row.rank}`:''
const load=async()=>{loading.value=true;error.value='';try{const [s,r]=await Promise.all([getSeason(route.params.id),getStandings(route.params.id)]);season.value=s.data;rows.value=r.data}catch(e){error.value=e?.message||'加载积分榜失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="`${season.seasonName||'联赛'}积分榜`" subtitle="排名按现有联赛积分与净胜球规则计算。" :breadcrumb="[{label:'联赛赛季',to:'/user/seasons'},{label:'积分榜'}]">
      <template #actions><RouterLink :to="`/user/seasons/${route.params.id}/rounds`" class="el-button el-button--primary">查看赛程</RouterLink></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无积分数据" empty-description="比赛产生赛果后，积分榜会显示在这里。" @retry="load">
      <TableWrapper title="联赛排名" description="前三名使用轻量强调，比赛数据使用等宽数字便于比较。" label="联赛积分榜">
        <el-table :data="rows" :row-class-name="rowClassName">
          <el-table-column prop="rank" label="排名" width="82" align="right"><template #default="{row}"><strong class="rank score-nums">{{row.rank}}</strong></template></el-table-column>
          <el-table-column label="俱乐部" min-width="210"><template #default="{row}"><RouterLink :to="`/user/clubs/${row.clubId}`" class="club"><el-avatar :size="34" :src="row.logoUrl" :alt="`${row.clubName}队徽`">{{row.clubName?.[0]}}</el-avatar><strong>{{row.clubName}}</strong></RouterLink></template></el-table-column>
          <el-table-column prop="matchesPlayed" label="场次" width="80" align="right" class-name="tabular-nums"/>
          <el-table-column label="胜 / 平 / 负" width="140" align="right"><template #default="{row}"><span class="tabular-nums">{{row.wins}} / {{row.draws}} / {{row.losses}}</span></template></el-table-column>
          <el-table-column prop="goalDifference" label="净胜球" width="100" align="right" class-name="tabular-nums"/>
          <el-table-column prop="goalsFor" label="进球" width="80" align="right" class-name="tabular-nums"/>
          <el-table-column prop="goalsAgainst" label="失球" width="80" align="right" class-name="tabular-nums"/>
          <el-table-column prop="points" label="积分" width="90" align="right"><template #default="{row}"><strong class="points score-nums">{{row.points}}</strong></template></el-table-column>
        </el-table>
      </TableWrapper>
    </DataState>
  </div>
</template>

<style scoped>.club{display:flex;align-items:center;gap:var(--space-3)}.club:hover strong{color:var(--role-accent)}.rank{font-size:var(--font-size-lg)}.points{font-size:var(--font-size-lg)}:deep(.podium-row td.el-table__cell){background:var(--color-surface-subtle)}:deep(.podium-row--1 .rank){color:var(--color-warning)}</style>
