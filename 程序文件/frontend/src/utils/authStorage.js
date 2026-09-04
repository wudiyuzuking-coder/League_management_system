import { AUTH_TOKEN_KEY, AUTH_USER_KEY, FORMAL_ROLE_CODES } from '../constants/app.js'

export const clearStoredAuth = (localStore, sessionStore) => {
  localStore.removeItem(AUTH_TOKEN_KEY)
  localStore.removeItem(AUTH_USER_KEY)
  sessionStore.removeItem(AUTH_TOKEN_KEY)
  sessionStore.removeItem(AUTH_USER_KEY)
}

export const restoreStoredAuth = (localStore, sessionStore) => {
  try {
    const token = localStore.getItem(AUTH_TOKEN_KEY)
    const user = JSON.parse(localStore.getItem(AUTH_USER_KEY) || 'null')
    if (token && user && FORMAL_ROLE_CODES.includes(user.roleCode)) return { token, user }
  } catch {
    // Invalid legacy cache is cleared below.
  }
  clearStoredAuth(localStore, sessionStore)
  return { token: null, user: null }
}
