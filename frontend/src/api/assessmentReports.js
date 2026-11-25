import request from './request'

// 转型金融评估报告接口。
export function fetchPendingAssessmentReport(industryKey, institutionId, id, params) {
  return request.get(`/assessment-reports/pending/${industryKey}/${institutionId}/${id}`, { params, timeout: 60000 })
}
