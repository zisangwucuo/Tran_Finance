import request from './request'

export function fetchFinancialDataNotices(params) {
  return request.get('/financial-data-notices', { params })
}
