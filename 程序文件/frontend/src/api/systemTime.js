import request from '../utils/request'

export const getSystemTime = () => request.get('/system-time')
export const setSystemTime = targetTime => request.put('/system-time', { targetTime })
export const resetSystemTime = () => request.post('/system-time/reset')
