import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resultReminderCountFrom } from '../src/utils/resultReminderCount.js'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('admin season detail uses backend statistics and distinguishes loading from real zero', async () => {
  const page = await source('../src/views/admin/AdminSeasonDetail.vue')
  assert.match(page, /detailLoaded\.value \? '加载中'/)
  assert.match(page, /season\.value\.submittedTeamCount == null \? '—'/)
  assert.match(page, /season\.value\.matchCount \?\? '—'/)
  assert.doesNotMatch(page, /rounds\.value\.reduce/)
  assert.doesNotMatch(page, /submittedTeamCount \?\? 0/)
})

test('result reminder count always adopts the refreshed backend total', () => {
  assert.equal(resultReminderCountFrom({ data: { total: 1 } }), 1)
  assert.equal(resultReminderCountFrom({ data: { total: 0 } }), 0)
  assert.equal(resultReminderCountFrom({ data: { total: 2 } }), 2)
})

test('successful score submission refreshes shared reminder state without local decrement', async () => {
  const [detail, layout, store, reminders] = await Promise.all([
    source('../src/views/admin/AdminMatchDetail.vue'),
    source('../src/layouts/ManagementLayout.vue'),
    source('../src/stores/resultReminders.js'),
    source('../src/views/admin/AdminMatchResultReminders.vue'),
  ])
  assert.match(detail, /await submitMatchResult[\s\S]*await resultReminderStore\.refreshCount\(\)/)
  assert.match(layout, /resultReminderStore\.count/)
  assert.match(store, /await getResultReminders\(\{ page: 1, size: 1 \}\)/)
  assert.match(store, /this\.count = resultReminderCountFrom\(response\)/)
  assert.match(reminders, /watch\(\(\) => resultReminderStore\.revision, load\)/)
  assert.doesNotMatch([detail, layout, store, reminders].join('\n'), /count\s*--|count\s*-=\s*1/)
})
