// 表格核心评估值的展示色规则，供历史数据与待评估数据页面共用。
export function getEnvironmentalRatingClass(value) {
  const text = String(value || '').trim()
  if (!text || text === '-') {
    return 'value-muted'
  }
  if (text.includes('A')) {
    return 'value-success'
  }
  if (text.includes('B')) {
    return 'value-primary'
  }
  return 'value-danger'
}

export function getTransitionScoreClass(value) {
  if (value === null || value === undefined || value === '') {
    return 'value-muted'
  }
  const score = Number(value)
  if (Number.isNaN(score)) {
    return 'value-muted'
  }
  if (score >= 80) {
    return 'value-success'
  }
  if (score >= 70) {
    return 'value-primary'
  }
  if (score >= 60) {
    return 'value-warning'
  }
  return 'value-danger'
}
