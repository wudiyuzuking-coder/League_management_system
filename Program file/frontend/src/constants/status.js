export const STATUS_LABELS = {
  PENDING_ACTIVATION: '待激活', PENDING_CLUB_APPROVAL: '待俱乐部审核', ENABLED: '正常', DISABLED: '已停用', LOCKED: '已锁定', ACTIVE: '进行中', INACTIVE: '已停用',
  DRAFT: '草稿', REGISTRATION: '报名中', PREPARING: '准备中', IN_PROGRESS: '进行中', FINISHED: '已结束', PUBLISHED: '已发布', SCHEDULE_PUBLISHED: '赛程已公布', CANCELLED: '已取消',
  GENERATED: '生成中', CONFIRMED: '已发布', PENDING_ADMIN_REVIEW: '待管理员确认',
  NOT_STARTED: '未开售', ON_SALE: '售票中', ENDED: '已停售', SOLD_OUT: '已售罄', NOT_ENABLED: '票务未启用', MATCH_UNAVAILABLE: '不可售', PAUSED: '已暂停', CLOSED: '已关闭',
  AVAILABLE: '可售', SOLD: '已售',
  PENDING_PAYMENT: '待支付', PAID: '已支付', REFUND_PENDING: '退票审核中', REFUNDED: '已退票',
  CREATED: '已创建', SUCCESS: '成功', FAILED: '失败',
  UNUSED: '未使用', USED: '已使用', VOID: '已作废',
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回',
  SUBMITTED: '已报名', CONFLICT: '提交冲突', SINGLE_SUBMISSION: '单人提交',
  TODAY: '今天比赛', OVERDUE: '已逾期',
  STARTER: '首发', SUBSTITUTE: '替补', TRANSFERRED: '已离队', READY: '符合标准', NOT_READY: '待完善',
  CODE_NOT_FOUND: '票码不存在', WRONG_MATCH: '非当前比赛', ORDER_INVALID: '订单无效',
  TICKET_USED: '电子票已使用', TICKET_REFUNDED: '电子票已退票', TICKET_VOID: '电子票已作废',
  USER_CANCELLED: '用户取消', PAYMENT_TIMEOUT: '支付超时',
}

export const STATUS_TYPES = {
  ENABLED: 'success', ACTIVE: 'success', PUBLISHED: 'success', SCHEDULE_PUBLISHED: 'success', ON_SALE: 'success', AVAILABLE: 'success',
  PAID: 'success', SUCCESS: 'success', USED: 'success', APPROVED: 'success',
  DRAFT: 'info', PREPARING: 'primary', DISABLED: 'info', INACTIVE: 'info', CLOSED: 'info', ENDED: 'info', CANCELLED: 'info', VOID: 'info', FINISHED: 'info',
  PENDING_ACTIVATION: 'warning', PENDING_CLUB_APPROVAL: 'warning', PENDING_PAYMENT: 'warning', REFUND_PENDING: 'warning', LOCKED: 'warning', PAUSED: 'warning', CREATED: 'warning', PENDING: 'warning', GENERATED: 'warning', NOT_STARTED: 'warning', PENDING_ADMIN_REVIEW: 'warning', REGISTRATION: 'warning',
  SUBMITTED: 'info', SINGLE_SUBMISSION: 'warning', TODAY: 'warning',
  STARTER: 'success', SUBSTITUTE: 'info', TRANSFERRED: 'info', READY: 'success', NOT_READY: 'warning',
  CONFIRMED: 'success', SOLD_OUT: 'danger',
  CONFLICT: 'danger', OVERDUE: 'danger',
  FAILED: 'danger', REFUNDED: 'danger', REJECTED: 'danger', CODE_NOT_FOUND: 'danger', WRONG_MATCH: 'danger',
  ORDER_INVALID: 'danger', TICKET_USED: 'danger', TICKET_REFUNDED: 'danger', TICKET_VOID: 'danger',
}

export const statusLabel = value => STATUS_LABELS[value] || value || '—'
export const statusType = value => STATUS_TYPES[value] || 'info'

export const ACCOUNT_STATUS_LABELS = {
  PENDING_ACTIVATION: '待首次启用',
  PENDING_CLUB_APPROVAL: '俱乐部审核中',
  ENABLED: '已启用',
  DISABLED: '已停用',
  CANCELLED: '已注销',
}

export const CLUB_STATUS_LABELS = {
  ACTIVE: '已启用',
  DISABLED: '已停用',
}

export const accountStatusLabel = value => ACCOUNT_STATUS_LABELS[value] || statusLabel(value)
export const clubStatusLabel = value => CLUB_STATUS_LABELS[value] || statusLabel(value)
