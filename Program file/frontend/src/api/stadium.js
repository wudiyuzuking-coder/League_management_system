import request from '../utils/request'
export const getStadium=(id)=>request.get(`/stadiums/${id}`)
export const getZones=(stadiumId)=>request.get(`/stadiums/${stadiumId}/zones`)
export const getLayout=(zoneId)=>request.get(`/stadium-zones/${zoneId}/layout`)
