import request from '../utils/request'

export const getOverviewStatistics=params=>request.get('/admin/statistics/overview',{params})
export const getMatchStatistics=params=>request.get('/admin/statistics/matches',{params})
export const getMatchStatistic=id=>request.get(`/admin/statistics/matches/${id}`)
export const getClubStatistics=params=>request.get('/admin/statistics/clubs',{params})
export const getPopularMatches=params=>request.get('/admin/statistics/popular-matches',{params})
export const getSalesTrend=params=>request.get('/admin/statistics/sales-trend',{params})
export const getRefundStatistics=params=>request.get('/admin/statistics/refunds',{params})
export const getCheckinStatistics=params=>request.get('/admin/statistics/checkins',{params})
export const getOwnClubStatistics=params=>request.get('/club/statistics/overview',{params})
export const getOwnClubMatchStatistics=params=>request.get('/club/statistics/matches',{params})
