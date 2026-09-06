import request from '../utils/request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getCurrentUser = () => request.get('/auth/me')
export const updateProfile = (data) => request.put('/users/me', data)
export const changePassword = (data) => request.put('/users/me/password', data)
export const uploadAvatar = (file) => {
  const data = new FormData()
  data.append('file', file)
  return request.post('/profile/avatar', data, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const removeAvatar = () => request.delete('/profile/avatar')
