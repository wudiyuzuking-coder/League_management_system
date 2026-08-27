import request from '../utils/request'

export const createOrder=(matchZoneId,ticketCount)=>request.post('/orders',{matchZoneId,ticketCount})
export const getOrders=(params)=>request.get('/orders',{params})
export const getOrder=(id)=>request.get(`/orders/${id}`)
export const cancelOrder=(id)=>request.post(`/orders/${id}/cancel`)
export const payOrder=(id,simulateResult)=>request.post(`/orders/${id}/pay`,{payMethod:'SIMULATED',simulateResult})
