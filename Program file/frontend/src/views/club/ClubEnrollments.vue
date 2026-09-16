<script setup>
import {computed,onMounted,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {createClubEnrollment,getAvailableEnrollmentSeasons,getClubEnrollment,getClubEnrollments,getClubPersonnelOverview,getClubProfile} from '../../api/club'
import {formatDateTime} from '../../utils/format'

const available=ref([]),history=ref([]),profile=ref({}),personnel=ref({players:[],coaches:[]}),loading=ref(false),error=ref(''),dialog=ref(false),detailVisible=ref(false),detail=ref({}),detailId=ref(null),detailError=ref(''),season=ref(null),submitting=ref(false),detailLoading=ref(false)
const starters=computed(()=>personnel.value.players?.filter(p=>p.status==='在队'&&p.lineupRole==='STARTER')||[])
const substitutes=computed(()=>personnel.value.players?.filter(p=>p.status==='在队'&&p.lineupRole==='SUBSTITUTE')||[])
const coaches=computed(()=>personnel.value.coaches?.filter(c=>c.status==='现役')||[])
const readiness=computed(()=>[
  {label:'首发',value:starters.value.length,meta:'目标 11 人',tone:starters.value.length===11?'success':'warning'},
  {label:'替补',value:substitutes.value.length,meta:'当前阵容'},
  {label:'现役教练',value:coaches.value.length,meta:'当前教练组'},
  {label:'报名准备',status:personnel.value.compliant?'READY':'NOT_READY'},
])
const seasonMetrics=computed(()=>season.value?[
  {label:'报名状态',status:'REGISTRATION'},
  {label:'报名截止',value:formatDateTime(season.value.registrationDeadline),meta:'截止时间'},
  {label:'已报名球队',value:`${Math.max(0,Number(season.value.maxClubs||0)-Number(season.value.remainingSlots||0))} / ${season.value.maxClubs||0}`},
]:[])
const positionLabel={GOALKEEPER:'门将',DEFENDER:'后卫',MIDFIELDER:'中场',FORWARD:'前锋'},titleLabel={HEAD_COACH:'主教练',ASSISTANT_COACH:'助理教练'},roleLabel={STARTER:'首发',SUBSTITUTE:'替补'}
const load=async()=>{loading.value=true;error.value='';try{const [a,h,p,o]=await Promise.all([getAvailableEnrollmentSeasons(),getClubEnrollments(),getClubProfile(),getClubPersonnelOverview()]);available.value=a.data;history.value=h.data;profile.value=p.data;personnel.value=o.data}catch(e){error.value=e?.message||'加载报名信息失败，请稍后重试。'}finally{loading.value=false}}
const open=row=>{if(row.timeConflict)return;if(!personnel.value.compliant)return ElMessage.error(personnel.value.message+(personnel.value.reason?`：${personnel.value.reason}`:''));season.value=row;dialog.value=true}
const submit=async()=>{if(!personnel.value.compliant)return ElMessage.error(personnel.value.message);submitting.value=true;try{await createClubEnrollment({seasonId:season.value.seasonId});dialog.value=false;ElMessage.success('赛季报名已提交，当前阵容已保存为报名快照');await load()}finally{submitting.value=false}}
const show=async id=>{detailId.value=id;detailLoading.value=true;detailError.value='';detailVisible.value=true;try{detail.value=(await getClubEnrollment(id)).data}catch(e){detailError.value=e?.message||'加载报名阵容失败，请稍后重试。'}finally{detailLoading.value=false}}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="赛季报名" subtitle="核对当前阵容、主场和参赛条件后保存报名快照。">
      <template #status><StatusTag :value="personnel.compliant?'READY':'NOT_READY'" :label="personnel.compliant?'可以报名':'待完善'" /></template>
    </PageHeader>
    <DataState :loading="loading" :error="error" :empty="false" @retry="load">
      <MetricStrip :items="readiness" label="报名准备摘要" />
      <CardShell class="readiness-card" variant="action" :title="personnel.compliant?'准备完成，可以报名':'报名准备尚未完成'" :subtitle="personnel.compliant?'提交报名时将保存当前阵容与主场快照。':(personnel.reason||personnel.message)">
        <ActionToolbar title="当前阵容 → 保存快照" description="报名后仍以本次保存的阵容和主场信息为准。"><template #actions><RouterLink to="/club/personnel" class="el-button">检查人员</RouterLink><RouterLink to="/club/profile" class="el-button">检查主场</RouterLink></template></ActionToolbar>
      </CardShell>

      <section class="enrollment-section" aria-labelledby="available-heading">
        <div class="section-heading"><div><h2 id="available-heading">可报名赛季</h2><p>选择赛季后进入最终准备确认。</p></div><span>{{available.length}} 个</span></div>
        <EmptyState v-if="!available.length" title="当前没有可报名的赛季" description="新的报名窗口开放后会显示在这里。" />
        <div v-else class="season-list">
          <CardShell v-for="row in available" :key="row.seasonId" variant="fixture" compact>
            <template #header><div class="season-title"><h3>{{row.seasonName}}</h3><StatusTag :value="row.timeConflict?'CONFLICT':'REGISTRATION'" :label="row.timeConflict?'时间冲突':'报名开放'" /></div></template>
            <div class="season-facts"><div><span>赛季周期</span><strong>{{row.startDate}} — {{row.endDate}}</strong></div><div><span>报名截止</span><strong>{{formatDateTime(row.registrationDeadline)}}</strong></div><div><span>已报名球队</span><strong class="tabular-nums">{{Math.max(0,Number(row.maxClubs||0)-Number(row.remainingSlots||0))}} / {{row.maxClubs}}</strong></div></div>
            <template #footer><div class="season-action"><span v-if="row.timeConflict">与已有参赛赛季时间冲突</span><span v-else>{{personnel.compliant?'阵容与主场检查通过':'请先完成报名准备'}}</span><el-button type="primary" :disabled="row.timeConflict||!personnel.compliant" @click="open(row)">确认报名</el-button></div></template>
          </CardShell>
        </div>
      </section>

      <TableWrapper class="enrollment-section" title="报名记录" description="已提交的阵容和主场快照。" label="报名历史">
        <el-table :data="history"><el-table-column prop="seasonName" label="赛季" min-width="180"/><el-table-column prop="stadiumName" label="主场" min-width="150"/><el-table-column label="报名时间" min-width="170"><template #default="{row}">{{formatDateTime(row.submittedAt)}}</template></el-table-column><el-table-column label="阵容" width="130"><template #default="{row}">{{row.playerCount}} 球员 / {{row.coachCount}} 教练</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.enrollmentStatus" /></template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="{row}"><el-button link type="primary" @click="show(row.enrollmentId)">查看报名阵容</el-button></template></el-table-column></el-table>
      </TableWrapper>
    </DataState>

    <el-dialog v-model="dialog" class="app-dialog" :title="season?.seasonName||'确认赛季报名'" width="900px" :close-on-click-modal="!submitting" :close-on-press-escape="!submitting" :show-close="!submitting">
      <MetricStrip :items="seasonMetrics" label="赛季报名信息" />
      <div class="snapshot-grid"><CardShell title="阵容快照" compact><dl class="snapshot-facts"><div><dt>首发</dt><dd>{{starters.length}}</dd></div><div><dt>替补</dt><dd>{{substitutes.length}}</dd></div><div><dt>教练</dt><dd>{{coaches.length}}</dd></div></dl></CardShell><CardShell title="主场" compact><dl class="venue-summary"><div><dt>场馆</dt><dd>{{profile.city}} {{profile.stadiumName}}</dd></div><div><dt>地址</dt><dd>{{profile.address}}</dd></div><div><dt>容量</dt><dd>{{profile.capacity||0}} 座 · STANDARD_8</dd></div></dl></CardShell></div>
      <TableWrapper title="首发阵容" label="报名首发快照" compact><el-table :data="starters" max-height="260"><el-table-column prop="shirtNo" label="号码" width="70"/><el-table-column prop="playerName" label="姓名"/><el-table-column label="位置"><template #default="{row}">{{positionLabel[row.position]}}</template></el-table-column></el-table></TableWrapper>
      <TableWrapper class="dialog-section" title="替补与教练" label="报名替补及教练快照" compact><el-table :data="[...substitutes,...coaches]" max-height="220"><el-table-column label="姓名"><template #default="{row}">{{row.playerName||row.coachName}}</template></el-table-column><el-table-column label="角色"><template #default="{row}">{{row.playerName?roleLabel[row.lineupRole]:titleLabel[row.title]}}</template></el-table-column></el-table></TableWrapper>
      <template #footer><el-button :disabled="submitting" @click="dialog=false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">提交报名</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" class="app-dialog" title="报名阵容快照" width="850px"><DataState :loading="detailLoading" :error="detailError" :empty="false" @retry="show(detailId)"><MetricStrip :items="[{label:'赛季',value:detail.seasonName},{label:'状态',status:detail.enrollmentStatus},{label:'主场',value:detail.stadiumName},{label:'提交时间',value:formatDateTime(detail.submittedAt)}]" label="报名快照摘要"/><TableWrapper class="dialog-section" title="报名球员" label="报名球员"><el-table :data="detail.players||[]"><el-table-column prop="shirtNo" label="号码" width="70"/><el-table-column prop="playerName" label="姓名"/><el-table-column label="位置"><template #default="{row}">{{positionLabel[row.position]}}</template></el-table-column><el-table-column label="阵容"><template #default="{row}"><StatusTag :value="row.lineupRole"/></template></el-table-column></el-table></TableWrapper><TableWrapper class="dialog-section" title="报名教练" label="报名教练"><el-table :data="detail.coaches||[]"><el-table-column prop="coachName" label="姓名"/><el-table-column label="职务"><template #default="{row}">{{titleLabel[row.title]}}</template></el-table-column></el-table></TableWrapper></DataState></el-dialog>
  </div>
</template>

<style scoped>
.readiness-card,.enrollment-section{margin-top:var(--space-4)}.section-heading{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:var(--space-3)}.section-heading h2{margin:0;font-size:var(--font-size-xl)}.section-heading p{margin:var(--space-1) 0 0;color:var(--color-text-muted)}.section-heading>span{color:var(--color-text-muted);font-size:var(--font-size-sm)}.season-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4)}.season-title{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3);width:100%}.season-title h3{margin:0;font-size:var(--font-size-lg)}.season-facts{display:grid;gap:var(--space-3)}.season-facts div{display:flex;align-items:center;justify-content:space-between;gap:var(--space-4)}.season-facts span,.season-action span{color:var(--color-text-muted);font-size:var(--font-size-sm)}.season-facts strong{font-size:var(--font-size-sm);text-align:right}.season-action{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3)}.snapshot-grid{display:grid;grid-template-columns:1fr 1fr;gap:var(--space-3);margin:var(--space-4) 0}.snapshot-facts{display:grid;grid-template-columns:repeat(3,1fr);margin:0;text-align:center}.snapshot-facts dt,.venue-summary dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.snapshot-facts dd{margin:var(--space-1) 0 0;font-size:var(--font-size-2xl);font-weight:var(--font-weight-bold)}.venue-summary{display:grid;gap:var(--space-2);margin:0}.venue-summary div{display:grid;grid-template-columns:64px 1fr}.venue-summary dd{margin:0;font-weight:var(--font-weight-medium)}.dialog-section{margin-top:var(--space-3)}@media(max-width:800px){.season-list,.snapshot-grid{grid-template-columns:1fr}}
</style>
