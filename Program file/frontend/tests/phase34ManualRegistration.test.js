import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('manual registration open reloads the admin season list after success', async () => {
  const page = await source('../src/views/admin/AdminSeasons.vue')
  assert.match(page, /await confirmation\.value\.execute\(\); confirmVisible\.value = false; await load\(\)/)
  assert.match(page, /await openSeasonRegistration\(row\.seasonId\)/)
})

test('club available seasons are rendered exactly from the backend response', async () => {
  const [page, api] = await Promise.all([
    source('../src/views/club/ClubEnrollments.vue'),
    source('../src/api/club.js'),
  ])
  assert.match(page, /available\.value=a\.data/)
  assert.doesNotMatch(page, /registrationStartTime|registration_start_time/)
  assert.match(api, /getAvailableEnrollmentSeasons = \(\) => request\.get\('\/club\/enrollments\/available-seasons'\)/)
})

test('phase34 adds no cross-client polling or realtime transport', async () => {
  const pages = await Promise.all([
    source('../src/views/admin/AdminSeasons.vue'),
    source('../src/views/club/ClubEnrollments.vue'),
  ])
  const text = pages.join('\n')
  assert.doesNotMatch(text, /WebSocket|EventSource|setInterval|setTimeout/)
})
