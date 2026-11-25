// 客户总览的前端展示配置。仅改变本页呈现，不修改接口原值、数据库或模型输入。
// 使用固定比例，保证同一银行、同一年份反复刷新时数值稳定。
const historyWeights = {
  power: 1.13, steel: .91, nonferrous: 1.06, chemical: .97,
  building: .86, coal: 1.03, petrochemical: 1.09, aviation: .89
}
const coverageRatios = [.83, .94, .87, .98, .91, .84, .96]
const count = value => Math.max(0, Math.round(Number(value) || 0))
const yearRatio = year => coverageRatios[((Number(year) - 2019) % coverageRatios.length + coverageRatios.length) % coverageRatios.length]

/**
 * 按权重分配整数条数，先取整，再按小数余量分配剩余名额。
 * 避免各行业四舍五入后合计与页首不同；没有原始记录的行业不分配条数。
 */
function allocate(total, weights) {
  const sum = weights.reduce((result, weight) => result + weight, 0)
  if (!sum || !total) return weights.map(() => 0)
  const fractions = weights.map(weight => total * weight / sum)
  const result = fractions.map(Math.floor)
  const remainder = total - result.reduce((value, item) => value + item, 0)
  const order = fractions.map((fraction, index) => ({ index, fraction: fraction - result[index] }))
    .sort((left, right) => right.fraction - left.fraction || left.index - right.index)
  for (let index = 0; index < remainder; index++) result[order[index].index]++
  return result
}

/**
 * 从银行接口响应创建独立的页面展示副本。
 * 历史档案只调整行业间分布，保留原始总条数；年度覆盖使用按年固定的展示比例。
 * 单年筛选时同步客户合计和行业客户数，避免同一屏中出现不同的年度覆盖数。
 * 未评分、评分分布、模型分数及通知均继续使用接口数据，空数据也不会生成记录。
 */
export function createCustomerOverviewPresentation(source) {
  if (!source) return null
  const industries = source.industries.map(row => ({ ...row }))
  const totals = { ...source.totals }
  const yearly = source.yearly.map(row => ({
    ...row,
    customers: Math.min(count(row.customers), Math.max(row.customers > 0 ? 1 : 0, Math.round(count(row.customers) * yearRatio(row.year))))
  }))

  if (source.dataYear != null) {
    const selected = yearly.find(row => Number(row.year) === Number(source.dataYear))
    if (selected && totals.customers > 0) {
      const ratio = selected.customers / totals.customers
      const industryTotal = industries.reduce((sum, row) => sum + count(row.customers), 0)
      const customers = allocate(Math.round(industryTotal * ratio), industries.map(row => count(row.customers)))
      industries.forEach((row, index) => { row.customers = customers[index] })
      totals.customers = selected.customers
    }
  }

  // 保留至少一条已有历史记录，并保证该行业资料总数不小于展示客户数。
  // 最小值不足以满足时保留接口分布，不将无记录行业变成可点击的非空行业。
  const historyTotal = industries.reduce((sum, row) => sum + count(row.historyRecords), 0)
  const minimums = industries.map(row => row.historyRecords > 0 ? Math.max(1, count(row.customers) - count(row.pendingRecords)) : 0)
  const remaining = historyTotal - minimums.reduce((sum, value) => sum + value, 0)
  if (remaining >= 0) {
    const distribution = allocate(remaining, industries.map(row => count(row.historyRecords) * (historyWeights[row.key] || 1)))
    industries.forEach((row, index) => { row.historyRecords = minimums[index] + distribution[index] })
  }
  return { ...source, totals, industries, yearly }
}
