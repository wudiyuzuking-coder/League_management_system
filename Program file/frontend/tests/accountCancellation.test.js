import test from 'node:test'
import assert from 'node:assert/strict'
import { cancelCurrentAccount } from '../src/utils/accountCancellation.js'

test('confirmed cancellation requests backend then clears auth and redirects', async () => {
  const events=[]
  await cancelCurrentAccount({confirm:async()=>events.push('confirm'),request:async()=>events.push('request'),logout:()=>events.push('logout'),notify:message=>events.push(message),redirect:async path=>events.push(path)})
  assert.deepEqual(events,['confirm','request','logout','账号已注销','/login'])
})

test('cancelled confirmation sends no request and preserves login state', async () => {
  let requested=false,loggedOut=false
  await assert.rejects(cancelCurrentAccount({confirm:async()=>{throw 'cancel'},request:async()=>{requested=true},logout:()=>{loggedOut=true},redirect:async()=>{}}))
  assert.equal(requested,false)
  assert.equal(loggedOut,false)
})

test('backend conflict preserves login state', async () => {
  let loggedOut=false
  await assert.rejects(cancelCurrentAccount({confirm:async()=>{},request:async()=>{throw {status:409,message:'blocked'}},logout:()=>{loggedOut=true},redirect:async()=>{}}),error=>error.status===409)
  assert.equal(loggedOut,false)
})
