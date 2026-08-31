import request from '../utils/request'
export const getMatches=(params)=>request.get('/matches',{params})
export const getMatch=(id)=>request.get(`/matches/${id}`)
export const getStadiums=()=>request.get('/stadiums')
export const createMatch=(data)=>request.post('/admin/matches',data)
export const updateMatch=(id,data)=>request.put(`/admin/matches/${id}`,data)
export const updateMatchStatus=(id,matchStatus)=>request.put(`/admin/matches/${id}/status`,{matchStatus})
export const updateMatchScore=(id,data)=>request.put(`/admin/matches/${id}/score`,data)
export const getResultReminders=(params)=>request.get('/admin/matches/result-reminders',{params})
