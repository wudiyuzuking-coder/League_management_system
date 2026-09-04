import { defineStore } from 'pinia'
import { APP_NAME, CURRENT_STAGE } from '../constants/app'

export const useAppStore = defineStore('app', {
  state: () => ({
    appName: APP_NAME,
    currentStage: CURRENT_STAGE,
  }),
})
