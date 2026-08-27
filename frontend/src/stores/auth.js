import { defineStore } from 'pinia'
import { getCurrentUser, login as loginApi, register as registerApi } from '../api/auth'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY, ROLE_HOME } from '../constants/app'

const storedUser = () => {
  try {
    return JSON.parse(localStorage.getItem(AUTH_USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem(AUTH_TOKEN_KEY), user: storedUser() }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
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
      this.persist()
      await this.fetchMe()
      return this.homePath
    },
    register(payload) {
      return registerApi(payload)
    },
    async fetchMe() {
      const response = await getCurrentUser()
      this.user = { ...this.user, ...response.data }
      this.persist()
      return this.user
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem(AUTH_TOKEN_KEY)
      localStorage.removeItem(AUTH_USER_KEY)
    },
  },
})
