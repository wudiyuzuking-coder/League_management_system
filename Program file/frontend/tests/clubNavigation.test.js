import test from 'node:test'
import assert from 'node:assert/strict'
import { ROLE_MENUS } from '../src/config/navigation.js'

test('CLUB primary navigation uses personnel overview and removes legacy player stats entry', () => {
  assert.deepEqual(ROLE_MENUS.CLUB, [
    ['/club/personnel', '人员管理'],
    ['/club/enrollments', '赛季报名'],
    ['/club/schedules', '已发布赛程'],
    ['/club/statistics', '俱乐部数据'],
    ['/club/profile', '俱乐部资料'],
  ])
  assert.equal(ROLE_MENUS.CLUB.some(([path]) => path === '/club/stats'), false)
  assert.equal(ROLE_MENUS.CLUB.some(([, label]) => label === '本队比赛'), false)
})
