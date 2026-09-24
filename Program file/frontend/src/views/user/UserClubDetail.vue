<script setup>
import {computed,onMounted,ref} from 'vue'
import {useRoute} from 'vue-router'
import {getUserClubDetail} from '../../api/club'

const route=useRoute(),data=ref(null),loading=ref(false),error=ref('')
const positions={GOALKEEPER:'守门员',DEFENDER:'后卫',MIDFIELDER:'中场',FORWARD:'前锋'}
const coachTitles={HEAD_COACH:'主教练',ASSISTANT_COACH:'助理教练',GOALKEEPER_COACH:'守门员教练',FITNESS_COACH:'体能教练'}
const club=computed(()=>data.value?.club||{})
const standingMetrics=computed(()=>{const row=data.value?.standing;if(!row)return[];return[{label:'排名',value:`第 ${row.rank} 名`,meta:row.seasonName},{label:'积分',value:row.points},{label:'比赛',value:row.matchesPlayed,meta:'场'},{label:'胜 / 平 / 负',value:`${row.wins} / ${row.draws} / ${row.losses}`},{label:'进 / 失球',value:`${row.goalsFor} / ${row.goalsAgainst}`} ]})
const load=async()=>{loading.value=true;error.value='';try{data.value=(await getUserClubDetail(route.params.clubId)).data}catch(e){error.value=e?.message||'加载俱乐部信息失败，请稍后重试。'}finally{loading.value=false}}
const score=m=>m.homeScore==null?'VS':`${m.homeScore} : ${m.awayScore}`
onMounted(load)
</script>

<template>
  <div>
    <PageHeader back back-fallback="/user/seasons" :title="club.clubName||'俱乐部详情'" subtitle="球队资料、当前阵容与近期比赛。" :breadcrumb="[{label:'联赛赛季',to:'/user/seasons'},{label:'俱乐部详情'}]" />
    <DataState :loading="loading" :error="error" :empty="!data" empty-title="俱乐部不存在或暂不可查看" @retry="load">
      <CardShell variant="fixture" class="club-hero">
        <div class="club-identity"><el-avatar :size="92" :src="club.logoUrl" :alt="`${club.clubName}队徽`">{{club.clubName?.[0]}}</el-avatar><div><h2>{{club.clubName}}</h2><p>{{club.description||'暂无俱乐部简介。'}}</p></div></div>
        <dl class="club-facts"><div><dt>简称</dt><dd>{{club.shortName||'未设置'}}</dd></div><div><dt>所在城市</dt><dd>{{club.homeCity||'未设置'}}</dd></div><div><dt>主场</dt><dd>{{club.homeStadium?.stadiumName||'暂未配置'}}</dd></div><div><dt>场馆容量</dt><dd>{{club.homeStadium?.capacity??0}} 座</dd></div><div><dt>场馆地址</dt><dd>{{club.homeStadium?.address||'暂未配置'}}</dd></div></dl>
      </CardShell>

      <MetricStrip v-if="data.standing" class="club-section" :items="standingMetrics" label="当前赛季战绩" />
      <EmptyState v-else class="club-section" title="暂无赛季战绩" description="球队产生正式赛果后会显示在这里。" />

      <div class="roster-grid club-section">
        <TableWrapper title="当前球员" label="俱乐部球员"><el-table :data="data.players||[]"><el-table-column prop="number" label="号码" width="78" align="center"><template #default="{row}"><strong class="score-nums">{{row.number??'—'}}</strong></template></el-table-column><el-table-column prop="name" label="姓名" min-width="140"/><el-table-column label="位置" width="110"><template #default="{row}">{{positions[row.position]||'—'}}</template></el-table-column><el-table-column label="年龄" width="80"><template #default="{row}">{{row.age==null?'—':`${row.age}岁`}}</template></el-table-column><el-table-column prop="nationality" label="国籍" min-width="100"/></el-table></TableWrapper>
        <TableWrapper title="教练团队" label="俱乐部教练"><el-table :data="data.coaches||[]"><el-table-column prop="name" label="姓名" min-width="140"/><el-table-column label="职务" min-width="130"><template #default="{row}">{{coachTitles[row.title]||'—'}}</template></el-table-column><el-table-column prop="nationality" label="国籍" min-width="100"/></el-table></TableWrapper>
      </div>

      <TableWrapper class="club-section" title="最近结束比赛" label="最近结束比赛"><el-table :data="data.recentMatches||[]"><el-table-column label="日期" min-width="160"><template #default="{row}">{{$formatDateTime(row.matchTime)}}</template></el-table-column><el-table-column label="对阵" min-width="240"><template #default="{row}">{{row.homeClubName}} <strong class="fixture-score score-nums">{{score(row)}}</strong> {{row.awayClubName}}</template></el-table-column><el-table-column prop="stadiumName" label="场馆" min-width="140"/></el-table></TableWrapper>
      <TableWrapper class="club-section" title="接下来比赛" :description="data.nextMatch?`最近比赛还有 ${data.daysUntilNextMatch} 天`:'正式赛程公布后会显示未来比赛。'" label="接下来比赛"><el-table :data="data.upcomingMatches||[]"><el-table-column label="时间" min-width="170"><template #default="{row}">{{$formatDateTime(row.matchTime)}}</template></el-table-column><el-table-column label="对阵" min-width="220"><template #default="{row}">{{row.homeClubName}} VS {{row.awayClubName}}</template></el-table-column><el-table-column prop="stadiumName" label="场馆" min-width="140"/><el-table-column label="操作" width="120"><template #default="{row}"><RouterLink :to="`/user/matches/${row.matchId}`" class="el-button el-button--primary is-link">查看比赛</RouterLink></template></el-table-column></el-table></TableWrapper>
    </DataState>
  </div>
</template>

<style scoped>.club-identity{display:flex;align-items:center;gap:var(--space-5)}.club-identity h2{margin:0;font-size:var(--font-size-2xl)}.club-identity p{max-width:62ch;margin:var(--space-2) 0 0;color:var(--color-text-secondary);line-height:var(--line-height-body)}.club-facts{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--space-5);margin:var(--space-6) 0 0;padding-top:var(--space-5);border-top:1px solid var(--color-line)}.club-facts div:last-child{grid-column:span 2}.club-facts dt{color:var(--color-text-muted);font-size:var(--font-size-xs)}.club-facts dd{margin:var(--space-1) 0 0;font-weight:var(--font-weight-semibold)}.club-section{margin-top:var(--space-4)}.roster-grid{display:grid;grid-template-columns:1.2fr .8fr;gap:var(--space-4)}.fixture-score{margin:0 var(--space-2)}@media(max-width:900px){.club-facts{grid-template-columns:repeat(2,1fr)}.roster-grid{grid-template-columns:1fr}}</style>
