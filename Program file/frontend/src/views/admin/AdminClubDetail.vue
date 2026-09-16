<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getAdminClub} from '../../api/club'
import {maskPhone} from '../../utils/privacy'

const route=useRoute(),club=ref({}),loading=ref(false),error=ref('')
const metrics=computed(()=>[
  {label:'俱乐部状态',status:club.value.clubStatus==='ACTIVE'?'ENABLED':club.value.clubStatus},
  club.value.leaderStatus?{label:'负责人状态',status:club.value.leaderStatus}:{label:'负责人状态',value:'未绑定'},
  {label:'所在城市',value:club.value.homeCity||'未填写'},
])
const load=async()=>{loading.value=true;error.value='';try{club.value=(await getAdminClub(route.params.id)).data}catch(e){error.value=e?.message||'加载俱乐部详情失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div class="governance-page">
    <PageHeader :breadcrumb="[{label:'俱乐部管理',to:'/admin/clubs'},{label:club.clubName||'俱乐部详情'}]" :title="club.clubName||'俱乐部详情'" subtitle="查看俱乐部基本资料、负责人和主场登记信息。"><template #status><StatusTag v-if="club.clubStatus" :value="club.clubStatus==='ACTIVE'?'ENABLED':club.clubStatus" /></template></PageHeader>
    <DataState :loading="loading" :error="error" :empty="!club.clubId" empty-title="未找到俱乐部" empty-description="该俱乐部可能已不存在或暂时无法访问。" @retry="load">
      <MetricStrip :items="metrics" label="俱乐部治理摘要" />
      <div class="detail-grid">
        <CardShell title="基本资料" subtitle="俱乐部公开身份与简介。"><dl class="detail-list"><div><dt>正式名称</dt><dd>{{club.clubName||'—'}}</dd></div><div><dt>简称</dt><dd>{{club.shortName||'—'}}</dd></div><div class="detail-list__wide"><dt>简介</dt><dd>{{club.description||'—'}}</dd></div></dl></CardShell>
        <CardShell title="主场信息" subtitle="由俱乐部负责人维护的主场登记。"><dl class="detail-list"><div><dt>城市</dt><dd>{{club.homeCity||'—'}}</dd></div><div><dt>地址</dt><dd>{{club.homeAddress||'—'}}</dd></div><div><dt>主场编号</dt><dd class="score-nums">{{club.homeStadiumId||'未绑定'}}</dd></div></dl></CardShell>
        <CardShell title="管理信息" subtitle="负责人账号与当前治理状态。"><dl class="detail-list"><div><dt>负责人</dt><dd>{{club.leaderName||'未绑定'}}</dd></div><div><dt>负责人手机号</dt><dd class="score-nums">{{maskPhone(club.leaderPhone)}}</dd></div><div><dt>用户名</dt><dd>{{club.leaderNickname||'—'}}</dd></div><div><dt>负责人账号状态</dt><dd><StatusTag v-if="club.leaderStatus" :value="club.leaderStatus" /><span v-else>—</span></dd></div></dl></CardShell>
      </div>
      <el-alert class="notice" type="info" :closable="false" title="球员、教练与主场资料由俱乐部负责人自行维护。" />
    </DataState>
  </div>
</template>

<style scoped>
.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4);margin-top:var(--space-4)}
.detail-grid>:last-child{grid-column:1/-1}
.detail-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4);margin:0}.detail-list div{min-width:0}.detail-list__wide{grid-column:1/-1}.detail-list dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.detail-list dd{margin:var(--space-1) 0 0;overflow-wrap:anywhere;font-weight:var(--font-weight-semibold)}
.notice{margin-top:var(--space-4)}
@media(max-width:900px){.detail-grid{grid-template-columns:1fr}.detail-grid>:last-child{grid-column:auto}}
</style>
