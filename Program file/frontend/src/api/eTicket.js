import request from '../utils/request'

export const getMyTickets=(params)=>request.get('/tickets',{params})
export const getTicket=(id)=>request.get(`/tickets/${id}`)
