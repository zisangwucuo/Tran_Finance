import request from './request'

// 联邦学习接口。模型由各金融机构本地随机森林按样本量加权集成，不传输原始企业数据。
export function fetchFederatedLearningSummary() {
  return request.get('/federated-learning/summary')
}

export function trainFederatedModels(institutionId) {
  return request.post('/federated-learning/train', { institutionId })
}

export function clearFederatedModels() {
  return request.post('/federated-learning/clear')
}
