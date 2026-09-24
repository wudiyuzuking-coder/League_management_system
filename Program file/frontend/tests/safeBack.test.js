import test from 'node:test'
import assert from 'node:assert/strict'
import { clearNavigationSource, getSafeBackTarget, rememberNavigationSource } from '../src/utils/safeBack.js'

test('safe back returns a trusted same-role in-app source', () => {
  rememberNavigationSource({}, { name: 'user-rounds', fullPath: '/user/seasons/8/rounds', meta: { roles: ['USER'] } }, 'USER')
  assert.equal(getSafeBackTarget({ role: 'USER', fallback: '/user/seasons', currentPath: '/user/matches/3' }), '/user/seasons/8/rounds')
})

test('safe back uses fallback when there is no source', () => {
  clearNavigationSource()
  assert.equal(getSafeBackTarget({ role: 'USER', fallback: '/user/orders', currentPath: '/user/orders/3' }), '/user/orders')
})

test('safe back ignores a source owned by another role', () => {
  const source = { fullPath: '/user/seasons', roles: ['USER'] }
  assert.equal(getSafeBackTarget({ role: 'EVENT_ADMIN', fallback: '/admin/matches', currentPath: '/admin/matches/3', source }), '/admin/matches')
})

test('a directly loaded or refreshed detail page has a working fallback', () => {
  rememberNavigationSource({}, { name: undefined, fullPath: '/', meta: {} }, 'CLUB')
  assert.equal(getSafeBackTarget({ role: 'CLUB', fallback: '/club/schedules', currentPath: '/club/matches/4/tickets' }), '/club/schedules')
})
