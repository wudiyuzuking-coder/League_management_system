import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { applySeasonNameConflict, clearSeasonNameConflict, SEASON_NAME_CONFLICT_MESSAGE } from '../src/utils/seasonCreateConflict.js'

test('season duplicate keeps the dialog model and all entered values', () => {
  const state = { visible: true, nameError: '', form: { seasonName: '城市联赛', startDate: '2027-03-01', maxClubs: 16 } }
  const before = structuredClone(state.form)
  assert.equal(applySeasonNameConflict(state, { __httpStatus: 409, message: SEASON_NAME_CONFLICT_MESSAGE }), true)
  assert.equal(state.visible, true)
  assert.deepEqual(state.form, before)
  assert.equal(state.nameError, '赛季名称已存在，请添加编号后重试')
})

test('season conflict clears only when the name watcher invokes its reset', () => {
  const state = { nameError: SEASON_NAME_CONFLICT_MESSAGE }
  clearSeasonNameConflict(state)
  assert.equal(state.nameError, '')
  assert.equal(applySeasonNameConflict(state, { __httpStatus: 400 }), false)
})

test('season page consumes create 409 locally beside the name field', async () => {
  const page = await readFile(new URL('../src/views/admin/AdminSeasons.vue', import.meta.url), 'utf8')
  assert.match(page, /createSeason\(form, \{ skipErrorNotification: true \}\)/)
  assert.match(page, /:error="createConflict\.nameError"/)
  assert.match(page, /watch\(\(\) => form\.seasonName/)
})
