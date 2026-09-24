import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('EVENT_ADMIN pages expose no retired manual lifecycle controls', async () => {
  const files = await Promise.all([
    '../src/views/admin/AdminSeasons.vue',
    '../src/views/admin/AdminSchedules.vue',
    '../src/views/admin/AdminSeasonDetail.vue',
    '../src/views/admin/AdminMatches.vue',
    '../src/views/admin/AdminMatchDetail.vue',
  ].map(source))
  const text = files.join('\n')
  for (const label of ['生成赛程', '确认赛程', '确认发布', '新增轮次', '开始比赛', '取消比赛', '发布比赛', '票务配置']) {
    assert.doesNotMatch(text, new RegExp(label))
  }
  assert.match(files[1], /系统自动生成并发布/)
  assert.match(files[4], /提交我的比分/)
})

test('formal frontend APIs contain no retired schedule match or ticket writes', async () => {
  const [league, match, ticket, router] = await Promise.all([
    source('../src/api/league.js'),
    source('../src/api/match.js'),
    source('../src/api/ticket.js'),
    source('../src/router/index.js'),
  ])
  assert.doesNotMatch(league, /schedule\/generate|schedule\/confirm|createRound/)
  assert.doesNotMatch(match, /updateMatchStatus|\/status/)
  assert.doesNotMatch(ticket, /createTicketZone|updateTicketZone|updateTicketZoneStatus|generateInventory|updateInventoryStatus|initializeStandardTicketing|\/admin\//)
  assert.doesNotMatch(router, /AdminMatchTickets|admin-match-tickets/)
})

test('required detail routes opt into the shared back control', async () => {
  const paths = [
    '../src/views/user/UserRounds.vue', '../src/views/user/UserStandings.vue',
    '../src/views/user/UserMatchDetail.vue', '../src/views/user/UserClubDetail.vue',
    '../src/views/user/UserOrderDetail.vue', '../src/views/user/UserTicketDetail.vue',
    '../src/views/club/ClubPlayers.vue', '../src/views/club/ClubCoaches.vue',
    '../src/views/club/ClubMatches.vue', '../src/views/club/ClubMatchTickets.vue',
    '../src/views/admin/AdminSeasonDetail.vue', '../src/views/admin/AdminMatchDetail.vue',
    '../src/views/admin/AdminStatisticsMatches.vue', '../src/views/admin/AdminClubDetail.vue',
    '../src/views/shared/AccountProfile.vue',
  ]
  for (const path of paths) assert.match(await source(path), /<PageHeader\s+back\b/)
  assert.match(await source('../src/views/auth/SwitchAccountView.vue'), /<BackButton\b/)
})
