import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { ROLE_MENUS } from '../src/config/navigation.js'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('USER navigation integrates matches into the season journey', async () => {
  assert.equal(ROLE_MENUS.USER.some(([path]) => path === '/user/matches'), false)
  const router = await source('../src/router/index.js')
  assert.match(router, /path:'matches'.*redirect:'\/user\/seasons'/)
  assert.match(router, /path:'matches\/:id'.*user-match-detail/)
})

test('season detail renders the complete confirmed schedule by round', async () => {
  const page = await source('../src/views/user/UserRounds.vue')
  const api = await source('../src/api/league.js')
  assert.match(api, /getSeasonSchedule=.*\/seasons\/\$\{seasonId\}\/schedule/)
  assert.match(page, /rounds=computed\(\(\)=>season\.value\.rounds/)
  assert.match(page, /round in rounds/)
  assert.match(page, /match in round\.matches/)
  assert.match(page, /match\.homeLogoUrl/)
  assert.match(page, /match\.awayLogoUrl/)
  assert.match(page, /match\.stadiumName/)
  assert.match(page, /match\.matchStatus/)
  assert.match(page, /match\.saleStatus/)
  assert.match(page, /<RouterLink[^>]+user\/matches/)
})

test('USER season cards expose confirmed counts and protect the schedule entry', async () => {
  const list = await source('../src/views/user/UserSeasons.vue')
  const detail = await source('../src/views/user/UserRounds.vue')
  assert.match(list, /season\.teamCount/)
  assert.match(list, /season\.roundCount/)
  assert.match(list, /season\.matchCount/)
  assert.match(list, /season\.teamCount\?\?0/)
  assert.match(list, /season\.roundCount\?\?0/)
  assert.match(list, /season\.matchCount\?\?0/)
  assert.doesNotMatch(list, /season\.(?:teamCount|roundCount|matchCount)\?\?['"]—['"]/)
  assert.match(list, /v-if="season\.scheduleConfirmed"[^>]+user\/seasons/)
  assert.match(list, /赛程将在报名结束后公布/)
  assert.match(detail, /getSeason\(route\.params\.id\)/)
  assert.match(detail, /if\(!summary\.scheduleConfirmed\)return/)
  assert.match(detail, /getSeasonSchedule\(route\.params\.id\)/)
})

test('EVENT_ADMIN season actions keep lifecycle controls and rely on automatic scheduling', async () => {
  const page = await source('../src/views/admin/AdminSeasons.vue')
  const api = await source('../src/api/league.js')
  assert.match(page, /开启报名/)
  assert.match(page, /结束报名/)
  assert.doesNotMatch(page, /generateSchedule|confirmSchedule/)
  assert.match(page, /系统将自动生成并发布赛程/)
  assert.match(page, /row\.seasonStatus === 'IN_PROGRESS'/)
  assert.doesNotMatch(page, /启用赛季|确认启用/)
  assert.match(api, /openSeasonRegistration=.*\/registration\/open/)
  assert.match(api, /closeSeasonRegistrationOnly=.*\/registration\/close/)
  assert.match(api, /closeSeasonRegistration=.*\/admin\/seasons\/\$\{seasonId\}\/close-registration/)
  assert.doesNotMatch(api, /schedule\/generate|schedule\/confirm/)
  assert.match(api, /finishSeason=.*\/admin\/seasons\/\$\{id\}\/finish/)
})

test('management season pages use admin endpoints instead of the public list', async () => {
  const api = await source('../src/api/league.js')
  const pages = await Promise.all([
    '../src/views/admin/AdminSeasons.vue',
    '../src/views/admin/AdminMatches.vue',
    '../src/views/admin/AdminMatchResultReminders.vue',
    '../src/views/admin/AdminSeasonRevenue.vue',
  ].map(source))
  assert.match(api, /getAdminSeasons=.*\/admin\/seasons/)
  pages.forEach(page => {
    assert.match(page, /getAdminSeasons/)
    assert.doesNotMatch(page, /\bgetSeasons\b/)
  })
})

test('visibility and purchasing controls use backend sale state independently', async () => {
  const page = await source('../src/views/user/UserRounds.vue')
  const ticketUi = await source('../src/components/TicketZoneList.vue')
  const statuses = await source('../src/constants/status.js')
  assert.match(page, /saleStatus==='NOT_STARTED'/)
  assert.match(page, /match\.saleStartTime/)
  assert.match(page, /match\.purchasable/)
  assert.match(ticketUi, /:disabled="!z\.saleAvailable/)
  assert.match(statuses, /ENDED: '已停售'/)
  assert.match(statuses, /FINISHED: '已结束'/)
})

test('ticket UI no longer contains obsolete seven-day or unified-sale wording', async () => {
  const files = [
    '../src/views/user/UserRounds.vue',
    '../src/components/TicketZoneList.vue',
    '../src/views/admin/AdminMatches.vue',
    '../src/views/admin/AdminSeasons.vue',
  ]
  const text = (await Promise.all(files.map(source))).join('\n')
  assert.doesNotMatch(text, /比赛前\s*7\s*天|赛季统一开售|报名截止.*开售/)
  assert.match(text, /售票时间按每场比赛日期自动计算/)
})
