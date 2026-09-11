import request from '../utils/request'

export const getPrefilledPassengers=()=>request.get('/user/prefilled-passengers')
export const addPrefilledPassenger=data=>request.post('/user/prefilled-passengers',data)
export const deletePrefilledPassenger=id=>request.delete(`/user/prefilled-passengers/${id}`)
