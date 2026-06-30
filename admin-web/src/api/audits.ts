import { http } from './http'
import type { ApiResponse } from './types'

export type AuditModule = 'AUTH' | 'PERMISSION' | 'REPAIR'
export type AuditAction = 'ADMIN_LOGIN' | 'APPROVE' | 'REJECT' | 'ACCEPT' | 'COMPLETE' | 'CLOSE'

export interface OperationLog {
  id: number
  enterpriseId: number | null
  operatorId: number
  module: AuditModule
  action: AuditAction
  businessId: number | null
  result: 'SUCCESS'
  detail: string | null
  ipAddress: string | null
  createdAt: string
}

export async function listAuditLogs(): Promise<OperationLog[]> {
  const response = await http.get<ApiResponse<OperationLog[]>>('/admin/audit-logs')
  return response.data.data
}
