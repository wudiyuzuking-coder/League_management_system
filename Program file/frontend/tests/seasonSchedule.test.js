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
    '../src/views/admin/AdminMatchTickets.vue',
    '../src/views/admin/AdminSeasons.vue',
  ]
  const text = (await Promise.all(files.map(source))).join('\n')
  assert.doesNotMatch(text, /比赛前\s*7\s*天|赛季统一开售|报名截止.*开售/)
  assert.match(text, /14天/)
})
