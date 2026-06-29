import { http } from './http'
import type { ApiResponse, AuthenticationResult, SessionTokens, SessionUser } from './types'

export async function adminLogin(username: string, password: string): Promise<AuthenticationResult> {
  const response = await http.post<ApiResponse<AuthenticationResult>>('/auth/admin-login', { username, password })
  return response.data.data
}

export async function currentUser(): Promise<SessionUser> {
  const response = await http.get<ApiResponse<SessionUser>>('/auth/me')
  return response.data.data
}

export async function refreshSession(refreshToken: string): Promise<SessionTokens> {
  const response = await http.post<ApiResponse<SessionTokens>>('/auth/refresh', { refreshToken })
  return response.data.data
}

export async function logout(): Promise<void> {
  await http.post('/auth/logout')
}
