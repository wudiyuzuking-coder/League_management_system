export const STATUS_LABELS = {
  ENABLED: '启用', DISABLED: '停用', ACTIVE: '进行中', INACTIVE: '停用',
  DRAFT: '草稿', PUBLISHED: '已发布', IN_PROGRESS: '进行中', FINISHED: '已结束', CANCELLED: '已取消',
  ON_SALE: '销售中', PAUSED: '已暂停', CLOSED: '已关闭',
  AVAILABLE: '可售', LOCKED: '已锁定', SOLD: '已售',
  PENDING_PAYMENT: '待支付', PAID: '已支付', REFUND_PENDING: '退票审核中', REFUNDED: '已退票',
  CREATED: '已创建', SUCCESS: '成功', FAILED: '失败',
  UNUSED: '未使用', USED: '已使用', VOID: '已作废',
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回',
  CODE_NOT_FOUND: '票码不存在', WRONG_MATCH: '非当前比赛', ORDER_INVALID: '订单无效',
  TICKET_USED: '电子票已使用', TICKET_REFUNDED: '电子票已退票', TICKET_VOID: '电子票已作废',
  USER_CANCELLED: '用户取消', PAYMENT_TIMEOUT: '支付超时',
}

export const STATUS_TYPES = {
  ENABLED: 'success', ACTIVE: 'success', PUBLISHED: 'success', ON_SALE: 'success', AVAILABLE: 'success',
  PAID: 'success', SUCCESS: 'success', USED: 'success', APPROVED: 'success',
  DRAFT: 'info', DISABLED: 'info', INACTIVE: 'info', CLOSED: 'info', CANCELLED: 'info', VOID: 'info',
  PENDING_PAYMENT: 'warning', REFUND_PENDING: 'warning', LOCKED: 'warning', PAUSED: 'warning', CREATED: 'warning', PENDING: 'warning',
  FAILED: 'danger', REFUNDED: 'danger', REJECTED: 'danger', CODE_NOT_FOUND: 'danger', WRONG_MATCH: 'danger',
  ORDER_INVALID: 'danger', TICKET_USED: 'danger', TICKET_REFUNDED: 'danger', TICKET_VOID: 'danger',
}

export const statusLabel = value => STATUS_LABELS[value] || value || '—'
export const statusType = value => STATUS_TYPES[value] || 'info'
