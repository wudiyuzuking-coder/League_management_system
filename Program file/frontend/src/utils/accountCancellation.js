export const cancelCurrentAccount = async ({ confirm, request, logout, redirect, notify }) => {
  await confirm()
  await request()
  logout()
  notify?.('账号已注销')
  await redirect('/login')
}
