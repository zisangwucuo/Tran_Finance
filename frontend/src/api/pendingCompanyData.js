import request from './request'

// 待评估企业数据维护接口。
export function fetchPendingCompanyData(params) {
  return request.get('/pending-company-data', { params })
}

export function fetchPendingCompanyDataOptions(params) {
  return request.get('/pending-company-data/options', { params })
}

export function createPendingCompanyData(data) {
  return request.post('/pending-company-data', data)
}

export function updatePendingCompanyData(id, data) {
  return request.put(`/pending-company-data/${id}`, data)
}

export function scorePendingCompanyData(id, data) {
  return request.post(`/pending-company-data/${id}/score`, data, { timeout: 60000 })
}

export function creditPendingCompanyData(id, data) {
  return request.post(`/pending-company-data/${id}/credit`, data)
}

export function deletePendingCompanyData(industryKey, institutionId, id) {
  return request.delete(`/pending-company-data/${industryKey}/${institutionId}/${id}`)
}
