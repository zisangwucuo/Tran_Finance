import request from './request'

// 模型训练与评估设置接口。
export function fetchModelTrainingSummary(institutionId) {
  return request.get('/model-training/summary', { params: { institutionId } })
}

export function trainAllModels(institutionId, force = false) {
  return request.post('/model-training/train-all', { institutionId, force })
}

export function updateModels(institutionId, force = false, parameters) {
  return request.post('/model-training/update', { institutionId, force, parameters })
}

export function clearModels(institutionId) {
  return request.post('/model-training/clear', { institutionId })
}

export function notifyCompaniesForMissingData(institutionId) {
  return request.post('/model-training/notify-companies', { institutionId })
}
