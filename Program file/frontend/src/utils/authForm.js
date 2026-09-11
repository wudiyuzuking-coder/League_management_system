export const PHONE_PATTERN = /^1\d{10}$/

export const MANAGEMENT_ROLES = ['EVENT_ADMIN', 'ADMIN']
export const LOGIN_ROLES = ['USER', 'CLUB', ...MANAGEMENT_ROLES]
export const REGISTER_ROLES = ['USER', 'CLUB']

export const employeeNoPattern = () => /^\d{4}$/

const hasText = (value) => typeof value === 'string' && value.trim().length > 0
const hasLengthBetween = (value, min, max) => (
  typeof value === 'string' && value.length >= min && value.length <= max
)

export const isLoginFormValid = (form) => {
  if (!LOGIN_ROLES.includes(form.roleCode)) return false
  if (!PHONE_PATTERN.test(form.phone) || !hasText(form.password)) return false
  return !MANAGEMENT_ROLES.includes(form.roleCode)
    || employeeNoPattern(form.roleCode).test(form.employeeNo)
}

export const isRegistrationFormValid = (form) => {
  if (!REGISTER_ROLES.includes(form.roleCode)) return false
  if (!hasText(form.username) || !hasLengthBetween(form.username, 2, 50)) return false
  if (!PHONE_PATTERN.test(form.phone)) return false
  if (!hasText(form.password) || !hasLengthBetween(form.password, 6, 72)) return false
  if (!hasText(form.confirmPassword) || form.confirmPassword !== form.password) return false
  if (form.roleCode === 'USER') return true
  if (!hasText(form.realName) || form.realName.length > 80) return false
  return hasText(form.clubName) && form.clubName.length <= 100
}

export const getLoginErrorMessage = (error) => {
  const message = typeof error === 'string' ? error : error?.message
  const knownMessages = {
    '手机号不存在': '该账号未注册',
    '该账号未注册': '该账号未注册',
    '账号不存在': '该账号未注册',
    '密码错误': '密码错误，请重新输入',
    '所选身份与账号不匹配': '所选身份与账号不匹配',
    '请输入工号': '请输入对应身份的工号',
    '管理人员工号不能为空': '请输入对应身份的工号',
    '工号与账号不匹配': '工号错误，与该账号不匹配',
    '该工号不存在': '该工号不存在',
    '工号与手机号不匹配': '工号与手机号不匹配',
    '工号与所选身份不匹配': '工号与所选身份不匹配',
    'Network Error': '网络连接失败，请确认后端服务已启动',
  }
  return knownMessages[message] || message || '登录请求失败，请稍后重试'
}
