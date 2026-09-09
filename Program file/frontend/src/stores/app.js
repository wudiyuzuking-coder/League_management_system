import { defineStore } from 'pinia'
import { APP_NAME } from '../constants/app'

export const useAppStore = defineStore('app', {
  state: () => ({
    appName: APP_NAME,
  }),
})
