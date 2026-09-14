import request from '../utils/request'

export const login = (data, config = {}) => request.post('/auth/login', data, { skipErrorNotification: true, ...config })
export const probeManagementAccount = data => request.post('/auth/management/probe', data, { skipErrorNotification: true })
export const activateManagementAccount = data => request.post('/auth/management/activate', data, { skipErrorNotification: true })
export const register = (data) => request.post('/auth/register', data)
export const getCurrentUser = (token) => request.get('/auth/me', token ? {
  headers: { Authorization: `Bearer ${token}` },
  preserveAuthOnUnauthorized: true,
  skipErrorNotification: true,
} : undefined)
export const updateProfile = (data) => request.put('/users/me', data)
export const changePassword = (data) => request.put('/users/me/password', data)
export const uploadAvatar = (file) => {
  const data = new FormData()
  data.append('file', file)
  return request.post('/profile/avatar', data, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const removeAvatar = () => request.delete('/profile/avatar')
export const cancelAccount = () => request.post('/account/cancel')
