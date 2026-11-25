import request from './request'

// 企业评估数据维护接口。
export function fetchElectricCompanyData(params) {
  return request.get('/electric-company-data', { params })
}

export function fetchElectricCompanyDataOptions(params) {
  return request.get('/electric-company-data/options', { params })
}

export function fetchTransitionScoreTrend(params) {
  return request.get('/electric-company-data/transition-score-trend', { params })
}

export function createElectricCompanyData(data) {
  return request.post('/electric-company-data', data)
}

export function updateElectricCompanyData(id, data) {
  return request.put(`/electric-company-data/${id}`, data)
}

export function deleteElectricCompanyData(industryKey, institutionId, id) {
  return request.delete(`/electric-company-data/${industryKey}/${institutionId}/${id}`)
}
