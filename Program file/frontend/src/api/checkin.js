import request from '../utils/request'

export const getCheckerMatches = (params) => request.get('/checker/matches', { params })
export const checkinTicket = (matchId, ticketCode) => request.post(`/checker/matches/${matchId}/checkin`, { ticketCode })
export const getCheckerCheckins = (params) => request.get('/checker/checkins', { params })
export const getAdminCheckins = (params) => request.get('/admin/checkins', { params })
export const getAdminCheckin = (id) => request.get(`/admin/checkins/${id}`)
