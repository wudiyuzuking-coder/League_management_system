import request from '../utils/request'

export const applyRefund=(orderId,reason)=>request.post(`/orders/${orderId}/refund`,{reason})
export const getMyRefunds=(params)=>request.get('/refunds',{params})
export const getMyRefund=(id)=>request.get(`/refunds/${id}`)
export const getAdminRefunds=(params)=>request.get('/admin/refunds',{params})
export const getAdminRefund=(id)=>request.get(`/admin/refunds/${id}`)
export const approveRefund=(id,auditReason)=>request.post(`/admin/refunds/${id}/approve`,{auditReason})
export const rejectRefund=(id,auditReason)=>request.post(`/admin/refunds/${id}/reject`,{auditReason})
