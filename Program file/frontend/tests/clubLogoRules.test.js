import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('club logo UI advertises and accepts JPEG PNG including octet-stream fallback', async () => {
  const source=await readFile(new URL('../src/views/club/ClubProfile.vue',import.meta.url),'utf8')
  assert.match(source,/accept="\.jpg,\.jpeg,\.png,image\/jpeg,image\/png"/)
  assert.match(source,/application\/octet-stream/)
  assert.match(source,/支持 JPEG、PNG，最大 2MB/)
  assert.match(source,/2\*1024\*1024/)
})
