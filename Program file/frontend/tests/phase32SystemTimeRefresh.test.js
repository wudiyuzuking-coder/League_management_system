import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('successful set and reset trigger a dedicated business view refresh', async () => {
  const store = await source('../src/stores/systemTime.js')
  assert.match(store, /businessTimeRevision:\s*0/)
  assert.match(store, /async set\([\s\S]*?this\.apply\(response\.data\)[\s\S]*?this\.businessTimeRevision\+\+/)
  assert.match(store, /async reset\([\s\S]*?this\.apply\(response\.data\)[\s\S]*?this\.businessTimeRevision\+\+/)
  const syncAction = store.match(/async sync\([\s\S]*?\n\s*},\s*\n\s*async set/)[0]
  assert.doesNotMatch(syncAction, /businessTimeRevision\+\+/)
})

test('management layout remounts only the current routed business view', async () => {
  const layout = await source('../src/layouts/ManagementLayout.vue')
  assert.match(layout, /<RouterView\s+:key="`\$\{route\.fullPath\}:\$\{systemTimeStore\.businessTimeRevision\}`"\s*\/>/)
  assert.doesNotMatch(layout, /window\.location|location\.reload/)
})
