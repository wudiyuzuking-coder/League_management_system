import assert from 'node:assert/strict'
import test from 'node:test'

import { AUTH_TOKEN_KEY, AUTH_USER_KEY, ROLE_HOME } from '../src/constants/app.js'
import { restoreStoredAuth } from '../src/utils/authStorage.js'

const storage = (entries = {}) => {
  const values = new Map(Object.entries(entries))
  return {
    getItem: (key) => values.get(key) ?? null,
    removeItem: (key) => values.delete(key),
    has: (key) => values.has(key),
  }
}

for (const [roleCode, home] of Object.entries({ USER: '/user', CLUB: '/club', EVENT_ADMIN: '/admin', ADMIN: '/admin' })) {
  test(`${roleCode} cache is restored`, () => {
    const local = storage({ [AUTH_TOKEN_KEY]: 'token', [AUTH_USER_KEY]: JSON.stringify({ roleCode }) })
    const session = storage()
    assert.equal(restoreStoredAuth(local, session).user.roleCode, roleCode)
    assert.equal(ROLE_HOME[roleCode], home)
  })
}

for (const roleCode of ['CHECKER', 'UNKNOWN_ROLE']) {
  test(`${roleCode} cache is cleared`, () => {
    const local = storage({ [AUTH_TOKEN_KEY]: 'token', [AUTH_USER_KEY]: JSON.stringify({ roleCode }) })
    const session = storage({ [AUTH_TOKEN_KEY]: 'legacy-token', [AUTH_USER_KEY]: 'legacy-user' })
    assert.deepEqual(restoreStoredAuth(local, session), { token: null, user: null })
    assert.equal(local.has(AUTH_TOKEN_KEY), false)
    assert.equal(local.has(AUTH_USER_KEY), false)
    assert.equal(session.has(AUTH_TOKEN_KEY), false)
    assert.equal(session.has(AUTH_USER_KEY), false)
  })
}

test('role cache without token is cleared', () => {
  const local = storage({ [AUTH_USER_KEY]: JSON.stringify({ roleCode: 'USER' }) })
  const session = storage()
  assert.deepEqual(restoreStoredAuth(local, session), { token: null, user: null })
  assert.equal(local.has(AUTH_USER_KEY), false)
})
