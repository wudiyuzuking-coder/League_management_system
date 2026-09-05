import { defineStore } from 'pinia'
import { getSystemTime, resetSystemTime, setSystemTime } from '../api/systemTime'

export const useSystemTimeStore = defineStore('systemTime', {
  state: () => ({ offsetMs: 0, nowMs: Date.now(), synced: false, revision: 0 }),
  actions: {
    apply(data) {
      this.offsetMs = Number(data.offsetSeconds || 0) * 1000
      this.nowMs = Date.now() + this.offsetMs
      this.synced = true
      this.revision++
    },
    tick() {
      this.nowMs = Date.now() + this.offsetMs
    },
    async sync() {
      const response = await getSystemTime()
      this.apply(response.data)
      return response.data
    },
    async set(targetTime) {
      const response = await setSystemTime(targetTime)
      this.apply(response.data)
      return response.data
    },
    async reset() {
      const response = await resetSystemTime()
      this.apply(response.data)
      return response.data
    },
    notifyBusinessChange() {
      this.revision++
    },
  },
})
