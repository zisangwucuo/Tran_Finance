import request from './request'

// 银行身份随登录会话发送，不允许调用者指定其他机构编码。
export function fetchCustomerOverview(dataYear) {
  return request.get('/customer-overview', { params: dataYear ? { dataYear } : {}, timeout: 30000 })
}
