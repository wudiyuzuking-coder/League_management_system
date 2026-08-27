import request from '../utils/request'

export const getAdminUsers = params => request.get('/admin/users', {params})
export const createAdminUser = data => request.post('/admin/users', data)
export const updateAdminUser = (id,data) => request.put(`/admin/users/${id}`, data)
export const updateAdminUserStatus = (id,userStatus) => request.put(`/admin/users/${id}/status`, {userStatus})
export const getRoles = () => request.get('/admin/roles')
