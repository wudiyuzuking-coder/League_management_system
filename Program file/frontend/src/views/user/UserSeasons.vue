<script setup>
import {onMounted,ref} from 'vue'
import {getSeasons} from '../../api/league'

const rows=ref([]),loading=ref(false),error=ref('')
const load=async()=>{loading.value=true;error.value='';try{rows.value=(await getSeasons()).data}catch(e){error.value=e?.message||'加载赛季失败，请稍后重试。'}finally{loading.value=false}}
onMounted(load)
</script>

<template>
  <div class="user-seasons">
    <PageHeader title="联赛赛季" subtitle="浏览正在进行和即将开始的足球联赛，查看完整赛程与积分榜。" />
    <DataState :loading="loading" :error="error" :empty="!rows.length" empty-title="暂无可浏览的赛季" empty-description="新赛季发布后会显示在这里。" @retry="load">
      <div class="season-grid">
        <article v-for="season in rows" :key="season.seasonId" class="season-card">
          <div class="season-card__top"><span class="season-card__eyebrow">FOOTBALL LEAGUE</span><StatusTag :value="season.seasonStatus"/></div>
          <h2>{{season.seasonName}}</h2>
          <p class="season-card__dates">{{$formatDate(season.startDate)}} — {{$formatDate(season.endDate)}}</p>
          <div class="season-card__stats tabular-nums">
            <span><b>{{season.clubCount??'—'}}</b> 支球队</span>
            <span><b>{{season.roundCount??'—'}}</b> 轮</span>
            <span><b>{{season.matchCount??'—'}}</b> 场比赛</span>
          </div>
          <p class="season-card__description">{{season.description||'赛季赛程、比赛详情与售票信息将在确认后持续更新。'}}</p>
          <div class="season-card__actions">
            <RouterLink :to="`/user/seasons/${season.seasonId}/rounds`" class="el-button el-button--primary">查看赛程</RouterLink>
            <RouterLink :to="`/user/seasons/${season.seasonId}/standings`" class="el-button">积分榜</RouterLink>
          </div>
        </article>
      </div>
    </DataState>
  </div>
</template>

<style scoped>
.season-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-lg)}
.season-card{position:relative;overflow:hidden;padding:26px;border:1px solid var(--border-color);border-radius:var(--radius-lg);background:var(--surface);box-shadow:var(--shadow-sm)}
.season-card::before{position:absolute;top:0;right:0;left:0;height:4px;background:var(--primary);content:""}
.season-card__top{display:flex;align-items:center;justify-content:space-between;gap:var(--space-sm)}
.season-card__eyebrow{color:var(--primary);font-size:11px;font-weight:800;letter-spacing:.14em}
.season-card h2{margin:18px 0 8px;font-size:24px;line-height:1.3;text-wrap:balance}
.season-card__dates{margin:0;color:var(--text-secondary);font-weight:650}
.season-card__stats{display:grid;grid-template-columns:repeat(3,1fr);gap:var(--space-sm);margin:22px 0;padding:16px 0;border-top:1px solid var(--border-color);border-bottom:1px solid var(--border-color)}
.season-card__stats span{display:flex;flex-direction:column;color:var(--text-muted);font-size:12px}
.season-card__stats b{color:var(--text-primary);font-size:20px}
.season-card__description{min-height:44px;color:var(--text-secondary);line-height:1.6}
.season-card__actions{display:flex;gap:var(--space-sm);margin-top:var(--space-lg)}
</style>
