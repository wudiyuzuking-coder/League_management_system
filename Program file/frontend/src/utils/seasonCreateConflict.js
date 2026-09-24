export const SEASON_NAME_CONFLICT_MESSAGE = '赛季名称已存在，请添加编号后重试'

export const applySeasonNameConflict = (state, error) => {
  if (error?.__httpStatus !== 409) return false
  state.nameError = SEASON_NAME_CONFLICT_MESSAGE
  return true
}

export const clearSeasonNameConflict = state => { state.nameError = '' }
