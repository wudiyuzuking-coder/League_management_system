export const ROLE_LABELS = {
  USER: '普通用户',
  CLUB: '俱乐部负责人',
  EVENT_ADMIN: '赛事管理员',
  ADMIN: '系统管理员',
}

export const ROLE_MENUS = {
  USER: [
    ['/user/seasons', '联赛赛季'],
    ['/user/matches', '比赛列表'],
    ['/user/orders', '我的订单'],
    ['/user/tickets', '我的电子票'],
    ['/user/refunds', '我的退票'],
  ],
  CLUB: [
    ['/club/profile', '俱乐部资料'],
    ['/club/players', '球员管理'],
    ['/club/coaches', '教练管理'],
    ['/club/stats', '赛季数据'],
    ['/club/matches', '本队比赛'],
    ['/club/statistics', '主场统计'],
    ['/club/enrollments', '赛季报名'],
    ['/club/schedules', '已确认赛程'],
  ],
  EVENT_ADMIN: [
    ['/admin/seasons', '赛季与积分榜'],
    ['/admin/enrollments', '赛季报名'],
    ['/admin/schedules', '赛程管理'],
    ['/admin/matches', '比赛管理'],
    ['/admin/matches/result-reminders', '赛果待维护'],
    ['/admin/stadiums', '场馆与座位'],
    ['/admin/refunds', '退票审核'],
    ['/admin/statistics', '统计分析'],
  ],
  ADMIN: [
    ['/admin/users', '用户管理'],
    ['/admin/internal-users', '内部人员管理'],
    ['/admin/clubs', '俱乐部管理'],
  ],
}

export const MENU_PATHS = [...new Set(Object.values(ROLE_MENUS).flat().map(([path]) => path))]
  .sort((left, right) => right.length - left.length)
