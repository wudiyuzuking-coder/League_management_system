export const ROLE_LABELS = {
  USER: '普通用户',
  CLUB: '俱乐部负责人',
  EVENT_ADMIN: '赛事管理员',
  ADMIN: '系统管理员',
}

export const ROLE_MENUS = {
  USER: [
    ['/user/seasons', '联赛赛季'],
    ['/user/orders', '我的订单'],
    ['/user/tickets', '我的电子票'],
    ['/user/passengers', '预填购票人'],
  ],
  CLUB: [
    ['/club/personnel', '人员管理'],
    ['/club/enrollments', '赛季报名'],
    ['/club/schedules', '已确认赛程'],
    ['/club/statistics', '俱乐部数据'],
    ['/club/profile', '俱乐部资料'],
  ],
  EVENT_ADMIN: [
    ['/admin/seasons', '赛季与积分榜'],
    ['/admin/enrollments', '赛季报名'],
    ['/admin/schedules', '赛程管理'],
    ['/admin/matches', '比赛管理'],
    ['/admin/matches/result-reminders', '赛果待维护'],
    ['/admin/statistics', '统计分析'],
    ['/admin/season-revenue', '赛季营收'],
  ],
  ADMIN: [
    ['/admin/result-reviews', '赛果确认'],
    ['/admin/users', '用户管理'],
    ['/admin/internal-users', '内部人员管理'],
    ['/admin/clubs', '俱乐部管理'],
  ],
}

export const MENU_PATHS = [...new Set(Object.values(ROLE_MENUS).flat().map(([path]) => path))]
  .sort((left, right) => right.length - left.length)
