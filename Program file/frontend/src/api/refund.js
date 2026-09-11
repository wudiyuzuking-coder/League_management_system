import request from '../utils/request'

export const applyRefund=(orderId,reason)=>request.post(`/orders/${orderId}/refund`,{reason})
export const getMyRefunds=(params)=>request.get('/refunds',{params})
export const getMyRefund=(id)=>request.get(`/refunds/${id}`)
