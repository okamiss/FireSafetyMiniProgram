import { http } from './http'
import type { ApiResponse, UserRole } from './types'

export interface Enterprise {
  id: number
  parentId: number | null
  name: string
  contactName: string
  contactPhone: string
  enabled: boolean
}

export interface EnterpriseUser {
  id: number
  enterpriseId: number
  displayName: string
  phone: string
  role: UserRole
  enabled: boolean
  weChatBound: boolean
}

export interface CreateEnterpriseInput {
  parentId: number | null
  name: string
  contactName: string
  contactPhone: string
  administratorName: string
  administratorPhone: string
}

export async function listEnterprises(): Promise<Enterprise[]> {
  const response = await http.get<ApiResponse<Enterprise[]>>('/admin/enterprises')
  return response.data.data
}

export async function createEnterprise(input: CreateEnterpriseInput): Promise<void> {
  await http.post('/admin/enterprises', input)
}

export async function listEnterpriseUsers(enterpriseId: number): Promise<EnterpriseUser[]> {
  const response = await http.get<ApiResponse<EnterpriseUser[]>>(`/admin/enterprises/${enterpriseId}/users`)
  return response.data.data
}

export async function disableUser(userId: number): Promise<void> {
  await http.post(`/admin/users/${userId}/disable`)
}
