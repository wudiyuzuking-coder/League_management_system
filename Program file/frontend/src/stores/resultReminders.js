import { defineStore } from 'pinia'
import { getResultReminders } from '../api/match'
import { resultReminderCountFrom } from '../utils/resultReminderCount'

export const useResultReminderStore = defineStore('resultReminders', {
  state: () => ({ count: 0, revision: 0 }),
  actions: {
    async refreshCount() {
      const response = await getResultReminders({ page: 1, size: 1 })
      this.count = resultReminderCountFrom(response)
      this.revision++
      return this.count
    },
    clear() {
      this.count = 0
      this.revision++
    },
  },
})
