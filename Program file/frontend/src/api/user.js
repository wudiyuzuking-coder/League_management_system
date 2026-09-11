import request from '../utils/request'

export const getAdminUsers = params => request.get('/admin/users', {params})
export const updateAdminUserStatus = (id,userStatus) => request.put(`/admin/users/${id}/status`, {userStatus})
export const getInternalUsers = params => request.get('/admin/internal-users', {params})
export const createInternalUser = data => request.post('/admin/internal-users', data)
export const updateInternalUserStatus = (id,userStatus) => request.put(`/admin/internal-users/${id}/status`, {userStatus})
export const getClubApplications = params => request.get('/admin/club-applications', {params})
export const approveClubUser = id => request.post(`/admin/club-applications/${id}/approve`, {mode:'CREATE_NEW'})
export const getRoles = () => request.get('/admin/roles')
