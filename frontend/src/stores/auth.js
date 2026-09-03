import { defineStore } from 'pinia'
import { getCurrentUser, login as loginApi, register as registerApi } from '../api/auth'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY, FORMAL_ROLE_CODES, ROLE_HOME } from '../constants/app'
import { clearStoredAuth, restoreStoredAuth } from '../utils/authStorage'

const initialAuth = restoreStoredAuth(localStorage, sessionStorage)

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: initialAuth.token, user: initialAuth.user, sessionValidated: false }),
  getters: {
    hasValidIdentity: (state) => Boolean(state.token && state.user && FORMAL_ROLE_CODES.includes(state.user.roleCode)),
    isAuthenticated() { return this.hasValidIdentity },
    homePath: (state) => ROLE_HOME[state.user?.roleCode] || '/login',
  },
  actions: {
    persist() {
      if (this.token) localStorage.setItem(AUTH_TOKEN_KEY, this.token)
      if (this.user) localStorage.setItem(AUTH_USER_KEY, JSON.stringify(this.user))
    },
    async login(payload) {
      const response = await loginApi(payload)
      this.token = response.data.token
      this.user = response.data
      this.sessionValidated = false
      this.persist()
      await this.fetchMe()
      return this.homePath
    },
    register(payload) {
      return registerApi(payload)
    },
    async fetchMe() {
      const cachedRole = this.user?.roleCode
      const response = await getCurrentUser()
      const currentRole = response.data?.roleCode
      if (!FORMAL_ROLE_CODES.includes(currentRole) || (cachedRole && cachedRole !== currentRole)) {
        this.logout()
        throw new Error('登录身份缓存已失效，请重新登录')
      }
      this.user = { ...this.user, ...response.data }
      this.sessionValidated = true
      this.persist()
      return this.user
    },
    logout() {
      this.token = null
      this.user = null
      this.sessionValidated = false
      clearStoredAuth(localStorage, sessionStorage)
    },
  },
})
