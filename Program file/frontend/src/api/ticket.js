import request from '../utils/request'
export const getTicketZones=(matchId)=>request.get(`/matches/${matchId}/ticket-zones`)
export const previewSeatAllocation=(id,ticketCount)=>request.post(`/match-ticket-zones/${id}/seat-allocation/preview`,{ticketCount})
export const previewTicketTypeAllocation=(matchId,ticketType,ticketCount)=>request.post(`/matches/${matchId}/ticket-types/${ticketType}/seat-allocation/preview`,{ticketCount})
