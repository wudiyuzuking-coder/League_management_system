let trustedSource = null

const normalizedRoles = route => Array.isArray(route?.meta?.roles) ? route.meta.roles : []

export const rememberNavigationSource = (to, from, role) => {
  if (!from?.name || from.meta?.public || from.name === 'login' || from.name === 'register') {
    trustedSource = null
    return
  }
  const roles = normalizedRoles(from)
  trustedSource = roles.length && !roles.includes(role)
    ? null
    : { fullPath: from.fullPath, roles }
}

export const clearNavigationSource = () => { trustedSource = null }

export const getSafeBackTarget = ({ role, fallback, currentPath, source = trustedSource }) => {
  if (!source?.fullPath || source.fullPath === currentPath) return fallback
  if (source.roles?.length && !source.roles.includes(role)) return fallback
  return source.fullPath
}
