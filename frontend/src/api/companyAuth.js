import request from './request'

export function companyLogin(data) {
  return request.post('/company-auth/login', data)
}

export function companyRegister(data) {
  return request.post('/company-auth/register', data)
}

export function fetchCompanyProfile(companyCode) {
  return request.get('/company-auth/profile', { params: { companyCode } })
}

export function updateCompanyProfile(data) {
  return request.put('/company-auth/profile', data)
}
