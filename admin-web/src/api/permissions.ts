import { http } from './http'
import type { ApiResponse } from './types'

export type PermissionStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface PermissionRequest {
  id: number
  enterpriseId: number
  applicantUserId: number
  requestedName: string
  requestedPhone: string
  status: PermissionStatus
  reviewerUserId: number | null
  reviewRemark: string | null
  createdAt: string
  reviewedAt: string | null
}

export async function listPermissionRequests(): Promise<PermissionRequest[]> {
  const response = await http.get<ApiResponse<PermissionRequest[]>>('/admin/permission-requests')
  return response.data.data
}

export async function reviewPermissionRequest(
  id: number,
  action: 'approve' | 'reject',
  remark: string,
): Promise<void> {
  await http.post(`/admin/permission-requests/${id}/${action}`, { remark })
}
