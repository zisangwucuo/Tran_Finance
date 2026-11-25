import request from './request'

export function fetchCompanyMissingData(params) {
  return request.get('/company-missing-data', { params })
}

export function fetchCompanyMissingDataDetail(id, companyCode) {
  return request.get(`/company-missing-data/${id}`, { params: { companyCode } })
}

export function saveCompanyMissingValues(id, data) {
  return request.put(`/company-missing-data/${id}`, data)
}

export function submitCompanyMissingData(id, companyCode) {
  return request.post(`/company-missing-data/${id}/submit`, null, { params: { companyCode } })
}
