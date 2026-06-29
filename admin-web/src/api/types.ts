export interface ApiResponse<T> {
  status: 'ok' | 'error'
  code: string
  data: T
  message: string | null
}

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
