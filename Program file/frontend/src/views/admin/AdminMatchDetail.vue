<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage} from 'element-plus'
import {getMatch,getMatchResult,submitMatchResult} from '../../api/match'
import {formatDateTime} from '../../utils/format'

const route=useRoute(),match=ref({}),workflow=ref({submissions:[]}),loading=ref(false),error=ref(''),scoreVisible=ref(false),scoreSaving=ref(false),scoreRef=ref(),score=reactive({homeScore:0,awayScore:0})
const scoreRules={homeScore:[{required:true,type:'number',min:0,message:'比分不得小于0'}],awayScore:[{required:true,type:'number',min:0,message:'比分不得小于0'}]}
const resultState=computed(()=>workflow.value.reviewStatus==='AUTO_PUBLISHED'?'赛果已自动发布':workflow.value.reviewReason==='CONFLICT'?'比分冲突待确认':workflow.value.reviewStatus?'等待多人确认':'尚未提交赛果')
const metrics=computed(()=>[
  {label:'比赛时间',value:formatDateTime(match.value.matchTime)},
  {label:'比赛场馆',value:match.value.stadiumName||'待安排'},
  {label:'自动开售',value:formatDateTime(match.value.saleStartTime)},
  {label:'赛果状态',value:resultState.value},
])
const load=async()=>{loading.value=true;error.value='';try{match.value=(await getMatch(route.params.id)).data;workflow.value=(await getMatchResult(route.params.id)).data}catch(e){error.value=e?.message||'加载比赛详情失败，请稍后重试。'}finally{loading.value=false}}
const saveScore=async()=>{if(scoreSaving.value)return;await scoreRef.value.validate();scoreSaving.value=true;try{await submitMatchResult(match.value.matchId,score);scoreVisible.value=false;ElMessage.success('比分已独立提交，系统将按多人确认规则处理');await load()}finally{scoreSaving.value=false}}
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader back back-fallback="/admin/matches" :breadcrumb="[{label:'比赛管理',to:'/admin/matches'},{label:'比赛详情'}]" :title="match.homeClubName&&match.awayClubName?`${match.homeClubName} 对阵 ${match.awayClubName}`:'比赛详情'" :subtitle="`${match.seasonName||'赛事'}${match.roundName?` · ${match.roundName}`:''}`"><template #status><StatusTag v-if="match.matchStatus" :value="match.matchStatus" /></template><template #actions><el-button v-if="['PUBLISHED','IN_PROGRESS'].includes(match.matchStatus)" type="success" @click="scoreVisible=true">提交我的比分</el-button></template></PageHeader>
    <DataState :loading="loading" :error="error" :empty="!match.matchId" empty-title="未找到比赛" empty-description="该比赛可能已不存在或暂时无法访问。" @retry="load">
      <MetricStrip :items="metrics" label="比赛运营摘要" />
      <CardShell class="fixture-card" title="对阵信息" subtitle="比分与队伍信息来自当前比赛记录。" variant="fixture">
        <div class="fixture"><div><el-avatar :size="56" :src="match.homeLogoUrl||undefined" :alt="`${match.homeClubName||'主队'}队徽`">{{match.homeClubName?.slice(0,1)}}</el-avatar><strong class="fixture-team-name">{{match.homeClubName||'主队待定'}}</strong></div><strong class="score-nums">{{match.homeScore==null?'VS':`${match.homeScore} : ${match.awayScore}`}}</strong><div><el-avatar :size="56" :src="match.awayLogoUrl||undefined" :alt="`${match.awayClubName||'客队'}队徽`">{{match.awayClubName?.slice(0,1)}}</el-avatar><strong class="fixture-team-name">{{match.awayClubName||'客队待定'}}</strong></div></div>
        <el-alert v-if="workflow.reviewStatus" :title="resultState" :type="workflow.reviewStatus==='AUTO_PUBLISHED'?'success':'warning'" :closable="false" />
      </CardShell>
      <div class="detail-grid">
        <CardShell title="赛果信息"><dl class="detail-list"><div><dt>当前赛果</dt><dd class="score-nums">{{match.homeScore==null?'尚未发布':`${match.homeScore} : ${match.awayScore}`}}</dd></div><div><dt>已提交人数</dt><dd class="score-nums">{{workflow.submissions?.length||0}}</dd></div><div><dt>审核状态</dt><dd>{{resultState}}</dd></div></dl></CardShell>
        <CardShell title="运营信息"><dl class="detail-list"><div><dt>比赛状态</dt><dd><StatusTag :value="match.matchStatus" /></dd></div><div><dt>状态说明</dt><dd>{{match.matchStatus==='PUBLISHED'?'比赛将在预定开始时间自动进入进行中':'状态由系统按赛事时间自动推进'}}</dd></div><div><dt>自动开售</dt><dd>{{$formatDateTime(match.saleStartTime)}}</dd></div><div><dt>自动停售</dt><dd>{{$formatDateTime(match.saleEndTime)}}</dd></div></dl></CardShell>
      </div>
    </DataState>
    <el-dialog v-model="scoreVisible" title="独立提交比分" width="420px"><el-alert title="提交后不可覆盖；系统需要不同赛事管理员形成共识。" type="info" :closable="false" /><el-form ref="scoreRef" :model="score" :rules="scoreRules" label-width="100px" class="score-form"><el-form-item :label="match.homeClubName" prop="homeScore"><el-input-number v-model="score.homeScore" :min="0" aria-label="主队比分" /></el-form-item><el-form-item :label="match.awayClubName" prop="awayScore"><el-input-number v-model="score.awayScore" :min="0" aria-label="客队比分" /></el-form-item></el-form><template #footer><el-button :disabled="scoreSaving" @click="scoreVisible=false">取消</el-button><el-button type="primary" :loading="scoreSaving" @click="saveScore">确认提交</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.fixture-card{margin-top:var(--space-4)}.fixture{display:grid;grid-template-columns:minmax(0,1fr) 140px minmax(0,1fr);align-items:center;gap:var(--space-4);text-align:center}.fixture>div{display:flex;align-items:center;justify-content:center;gap:var(--space-3);min-width:0}.fixture-team-name{overflow-wrap:anywhere;font-size:var(--font-size-xl)}.fixture>strong.score-nums{font-size:var(--font-size-3xl)}.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4);margin-top:var(--space-4)}.detail-list{display:grid;gap:var(--space-4);margin:0}.detail-list dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.detail-list dd{margin:var(--space-1) 0 0;font-weight:var(--font-weight-semibold)}.score-form{margin-top:var(--space-4)}
@media(max-width:900px){.fixture{grid-template-columns:1fr}.detail-grid{grid-template-columns:1fr}}
</style>
