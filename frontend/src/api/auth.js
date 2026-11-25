import request from './request'

// 金融机构用户认证接口。
export function login(data) {
  return request.post('/financial-auth/login', data)
}

export function register(data) {
  return request.post('/financial-auth/register', data)
}
