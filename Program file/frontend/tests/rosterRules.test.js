import test from 'node:test'
import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'

const read=path=>readFile(new URL(path,import.meta.url),'utf8')

test('player form validates age against system time and blocks invalid submission',async()=>{
  const source=await read('../src/views/club/ClubPlayers.vue')
  assert.match(source,/useSystemTimeStore/)
  assert.match(source,/球员年龄必须在18至50岁之间/)
  assert.match(source,/:error="playerAgeError"/)
  assert.match(source,/:disabled="saving\|\|!!playerAgeError"/)
})

test('coach form validates age against system time and blocks invalid submission',async()=>{
  const source=await read('../src/views/club/ClubCoaches.vue')
  assert.match(source,/useSystemTimeStore/)
  assert.match(source,/教练年龄必须在18至100岁之间/)
  assert.match(source,/:error="coachAgeError"/)
  assert.match(source,/:disabled="saving\|\|!!coachAgeError"/)
})
