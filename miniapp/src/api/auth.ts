import { request } from './http'

export type UserRole = 'SUPER_ADMIN' | 'PLATFORM_OPERATOR' | 'ENTERPRISE_ADMIN' | 'EMPLOYEE'

export interface SessionUser {
  userId: number
  role: UserRole
  enterpriseId: number | null
  displayName: string
}

export interface SessionTokens {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface AuthenticationResult {
  user: SessionUser
  tokens: SessionTokens
}

export interface WeChatLoginResult {
  bindingRequired: boolean
  bindingTicket: string | null
  user: SessionUser | null
  tokens: SessionTokens | null
}

export function weChatLogin(code: string) {
  return request<WeChatLoginResult>('/auth/wechat-login', { method: 'POST', data: { code } })
}

export function bindWeChatPhone(bindingTicket: string, phoneCode: string) {
  return request<AuthenticationResult>('/auth/wechat-bind-phone', {
    method: 'POST', data: { bindingTicket, phoneCode },
  })
}

export function logout() {
  return request<void>('/auth/logout', { method: 'POST' })
}
