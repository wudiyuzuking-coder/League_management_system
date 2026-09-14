import test from 'node:test'
import assert from 'node:assert/strict'
import { ROLE_MENUS } from '../src/config/navigation.js'
import { accountStatusLabel } from '../src/constants/status.js'

test('all role sidebars omit account profile and USER refunds', () => {
  for (const entries of Object.values(ROLE_MENUS)) {
    assert.equal(entries.some(([, label]) => label === '账号资料'), false)
  }
  assert.equal(ROLE_MENUS.USER.some(([path]) => path.includes('refund')), false)
})

test('ADMIN navigation exposes three separate account domains', () => {
  assert.deepEqual(ROLE_MENUS.ADMIN.slice(1), [
    ['/admin/users', '用户管理'],
    ['/admin/internal-users', '内部人员管理'],
    ['/admin/clubs', '俱乐部管理'],
  ])
})

test('account states use distinct Chinese labels', () => {
  assert.equal(accountStatusLabel('PENDING_ACTIVATION'), '待首次启用')
  assert.equal(accountStatusLabel('PENDING_CLUB_APPROVAL'), '俱乐部审核中')
  assert.equal(accountStatusLabel('ENABLED'), '已启用')
  assert.equal(accountStatusLabel('DISABLED'), '已停用')
  assert.equal(accountStatusLabel('LOCKED'), '已锁定')
  assert.equal(accountStatusLabel('CANCELLED'), '已注销')
})

test('account menu places cancellation after logout for every authenticated role', async () => {
  const source = await import('node:fs/promises').then(fs => fs.readFile(new URL('../src/layouts/ManagementLayout.vue', import.meta.url), 'utf8'))
  const logout = source.indexOf('command="logout"')
  const cancellation = source.indexOf('command="cancel"')
  assert.ok(logout >= 0)
  assert.ok(cancellation > logout)
  assert.match(source, /注销账号/)
})
