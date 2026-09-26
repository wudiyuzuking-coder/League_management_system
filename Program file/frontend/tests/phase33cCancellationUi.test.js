import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('AdminSeasons exposes the exact lifecycle action matrix without a more menu', async () => {
  const page = await source('../src/views/admin/AdminSeasons.vue')
  assert.doesNotMatch(page, /更多|el-dropdown|handleAction/)
  assert.match(page, /row\.seasonStatus === 'DRAFT'[^>]+>调整赛季/)
  assert.match(page, /row\.seasonStatus === 'DRAFT'[^>]+>开启报名/)
  assert.match(page, /row\.seasonStatus === 'REGISTRATION'[^>]+>结束报名/)
  assert.match(page, /row\.seasonStatus === 'IN_PROGRESS'[^>]+>结束赛季/)
  assert.doesNotMatch(page, /row\.seasonStatus === 'PREPARING'[^>]+(?:button|RouterLink)/)
  assert.doesNotMatch(page, /row\.seasonStatus === '(?:FINISHED|CANCELLED)'[^>]+(?:button|RouterLink)/)
  assert.match(page, /查看详情/)
})

test('cancelled admin season detail shows context and removes every write action', async () => {
  const page = await source('../src/views/admin/AdminSeasonDetail.vue')
  assert.match(page, /season\.value\.seasonStatus === 'CANCELLED'/)
  assert.match(page, /取消原因：/)
  assert.match(page, /取消时间：/)
  assert.match(page, /未提供取消原因/)
  assert.match(page, /v-if="!cancelled"[^>]*class="standings-toolbar"/)
  assert.match(page, /v-if="!cancelled"[^>]*width="144"/)
  assert.match(page, /该赛季已取消，未安排赛程/)
})

test('club enrollment history keeps submitted enrollment separate from cancelled season', async () => {
  const page = await source('../src/views/club/ClubEnrollments.vue')
  assert.match(page, /label="报名状态"/)
  assert.match(page, /报名已提交/)
  assert.match(page, /label="赛季状态"/)
  assert.match(page, /row\.seasonStatus==='CANCELLED'/)
  assert.match(page, /因\$\{row\.cancelReason\}，该赛季已取消/)
  assert.match(page, /row\.cancelledAt/)
  assert.match(page, /detail\.seasonStatus==='CANCELLED'/)
  assert.doesNotMatch(page, /等待排赛|继续报名|重新提交|修改报名/)
})

test('club schedules renders cancellation notices without fabricating schedule data', async () => {
  const [page, api] = await Promise.all([
    source('../src/views/club/ClubSchedules.vue'),
    source('../src/api/club.js'),
  ])
  assert.match(api, /getSeasonNotifications = \(\) => request\.get\('\/club\/season-notifications'\)/)
  assert.equal(api.match(/export const getSeasonNotifications/g)?.length, 1)
  assert.doesNotMatch(api, /season-notifications.*clubId/)
  assert.match(page, /v-if="notifications\.length"/)
  assert.match(page, /notice\.seasonName/)
  assert.match(page, /因参赛俱乐部不足 2 支，该赛季已取消/)
  assert.match(page, /notice\.cancelledAt/)
  assert.match(page, /StatusTag value="CANCELLED"/)
  assert.match(page, /void loadNotifications\(\)/)
  assert.match(page, /catch\(e\)\{console\.warn\('加载赛季取消通知失败'/)
  assert.doesNotMatch(page, /notifications.*(?:roundNo|matchId)/)
})

test('cancelled status uses the shared danger semantic', async () => {
  const statuses = await source('../src/constants/status.js')
  assert.match(statuses, /CANCELLED: '已取消'/)
  assert.match(statuses, /CANCELLED: 'danger'/)
})
