<script setup>
import {computed,onMounted,ref} from 'vue'
import {getClubPersonnelOverview} from '../../api/club'

const loading=ref(false),error=ref(''),overview=ref({players:[],coaches:[]})
const starters=computed(()=>overview.value.players?.filter(p=>p.status==='在队'&&p.lineupRole==='STARTER')||[])
const substitutes=computed(()=>overview.value.players?.filter(p=>p.status==='在队'&&p.lineupRole==='SUBSTITUTE')||[])
const activeCoaches=computed(()=>overview.value.coaches?.filter(c=>c.status==='现役')||[])
const issues=computed(()=>String(overview.value.reason||'').split(/[；;。]/).map(item=>item.trim()).filter(Boolean))
const metrics=computed(()=>[
  {label:'首发',value:overview.value.starterCount||0,meta:'目标 11 人',tone:overview.value.starterCount===11?'success':'warning'},
  {label:'替补',value:overview.value.substituteCount||0,meta:'最多 7 人'},
  {label:'现役教练',value:overview.value.activeCoachCount||0,meta:'当前教练组'},
  {label:'参赛状态',status:overview.value.compliant?'READY':'NOT_READY'},
])
const positionLabel={GOALKEEPER:'门将',DEFENDER:'后卫',MIDFIELDER:'中场',FORWARD:'前锋'}
const titleLabel={HEAD_COACH:'主教练',ASSISTANT_COACH:'助理教练'}
const load=async()=>{loading.value=true;error.value='';try{overview.value=(await getClubPersonnelOverview()).data}catch(e){error.value=e?.message||'加载人员信息失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="人员管理" subtitle="从阵容结构检查参赛准备情况，并维护球员与教练组。">
      <template #actions><RouterLink to="/club/players" class="el-button">球员管理</RouterLink><RouterLink to="/club/coaches" class="el-button el-button--primary">教练管理</RouterLink></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
      <MetricStrip :items="metrics" label="球队阵容摘要" />
      <CardShell class="compliance" variant="action" :title="overview.compliant?'队伍符合参赛标准':'当前队伍暂不符合比赛标准'" :subtitle="overview.compliant?'当前阵容和教练配置已满足报名合规要求。':'完成以下项目后即可满足报名要求。'">
        <div v-if="overview.compliant" class="compliance-ready"><StatusTag value="READY" label="准备完成" type="success"/><span>{{overview.message}}</span></div>
        <ul v-else class="issue-list"><li v-for="item in issues" :key="item">{{item}}</li><li v-if="!issues.length">{{overview.message}}</li></ul>
      </CardShell>
      <div class="personnel-sections">
        <TableWrapper title="首发阵容" description="号码、姓名和场上位置优先展示。" label="首发球员">
          <el-table :data="starters"><el-table-column label="号码" width="82" align="center"><template #default="{row}"><strong class="shirt-number score-nums">{{row.shirtNo??'—'}}</strong></template></el-table-column><el-table-column prop="playerName" label="姓名" min-width="150"/><el-table-column label="位置" width="120"><template #default="{row}">{{positionLabel[row.position]||'—'}}</template></el-table-column><el-table-column label="阵容" width="100"><template #default><StatusTag value="STARTER" /></template></el-table-column><el-table-column label="门将" width="90"><template #default="{row}"><StatusTag v-if="row.position==='GOALKEEPER'" value="GOALKEEPER" label="门将" type="info"/><span v-else>—</span></template></el-table-column></el-table>
        </TableWrapper>
        <TableWrapper title="替补球员" description="替补深度与位置分布。" label="替补球员">
          <el-table :data="substitutes"><el-table-column label="号码" width="82" align="center"><template #default="{row}"><strong class="shirt-number score-nums">{{row.shirtNo??'—'}}</strong></template></el-table-column><el-table-column prop="playerName" label="姓名" min-width="150"/><el-table-column label="位置" width="120"><template #default="{row}">{{positionLabel[row.position]||'—'}}</template></el-table-column><el-table-column label="阵容" width="100"><template #default><StatusTag value="SUBSTITUTE" /></template></el-table-column><el-table-column label="门将" width="90"><template #default="{row}"><StatusTag v-if="row.position==='GOALKEEPER'" value="GOALKEEPER" label="门将" type="info"/><span v-else>—</span></template></el-table-column></el-table>
        </TableWrapper>
        <TableWrapper title="教练组" description="主教练承担球队主要执教职责。" label="现役教练">
          <el-table :data="activeCoaches"><el-table-column prop="coachName" label="姓名" min-width="180"/><el-table-column label="角色" width="140"><template #default="{row}"><StatusTag :value="row.title" :label="titleLabel[row.title]||'—'" type="info"/></template></el-table-column><el-table-column label="状态" width="110"><template #default><StatusTag value="ACTIVE" label="现役" type="success"/></template></el-table-column></el-table>
        </TableWrapper>
      </div>
    </DataState>
  </div>
</template>

<style scoped>
.compliance{margin-top:var(--space-4)}.compliance-ready{display:flex;align-items:center;gap:var(--space-3);color:var(--color-text-secondary)}.issue-list{display:grid;gap:var(--space-2);margin:0;padding-left:20px;color:var(--color-danger)}.personnel-sections{display:grid;gap:var(--space-4);margin-top:var(--space-4)}.shirt-number{display:inline-grid;min-width:34px;height:34px;padding:0 var(--space-2);border:1px solid var(--color-line-strong);border-radius:var(--radius-sm);background:var(--color-surface-subtle);font-size:var(--font-size-lg);place-items:center}
</style>
