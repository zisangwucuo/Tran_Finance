import request from './request'

// 企业端历史数据与待评估数据查询接口。
export function fetchCompanyAssessmentData(datasetType, params) {
  return request.get(`/company-data/${datasetType}`, { params })
}

export function fetchCompanyAssessmentOptions(datasetType, params) {
  return request.get(`/company-data/${datasetType}/options`, { params })
}

export function fetchCompanyAssessmentDetail(datasetType, industryKey, id, params) {
  return request.get(`/company-data/${datasetType}/${industryKey}/${id}`, { params })
}
