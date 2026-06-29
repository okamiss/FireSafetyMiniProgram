import { request } from './http'

export type PermissionStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface PermissionRequest {
  id: number
  requestedName: string
  requestedPhone: string
  status: PermissionStatus
  reviewRemark: string | null
  createdAt: string
}

export function listPermissionRequests() {
  return request<PermissionRequest[]>('/miniapp/permission-requests')
}

export function createPermissionRequest(name: string, phone: string) {
  return request<PermissionRequest>('/miniapp/permission-requests', {
    method: 'POST', data: { name, phone },
  })
}
