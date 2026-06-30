import { http } from './http'
import type { ApiResponse } from './types'

export type RepairUrgency = 'NORMAL' | 'URGENT'
export type RepairStatus = 'PENDING_ACCEPTANCE' | 'PROCESSING' | 'COMPLETED' | 'CLOSED'

export interface RepairTicket {
  id: number
  enterpriseId: number
  reporterUserId: number
  urgency: RepairUrgency
  faultType: string
  location: string
  description: string
  contactName: string
  contactPhone: string
  status: RepairStatus
  handlerUserId: number | null
  result: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
  closedAt: string | null
}

export interface RepairHistory {
  id: number
  fromStatus: RepairStatus | null
  toStatus: RepairStatus
  operatorUserId: number
  remark: string
  createdAt: string
}

export interface RepairAttachment {
  id: number
  repairId: number
  originalName: string
  contentType: string
  fileSize: number
  createdAt: string
  contentUrl: string
}

export async function listRepairs(): Promise<RepairTicket[]> {
  const response = await http.get<ApiResponse<RepairTicket[]>>('/admin/repairs')
  return response.data.data
}

export async function getRepairHistory(id: number): Promise<RepairHistory[]> {
  const response = await http.get<ApiResponse<RepairHistory[]>>(`/admin/repairs/${id}/history`)
  return response.data.data
}

export async function getRepairAttachments(id: number): Promise<RepairAttachment[]> {
  const response = await http.get<ApiResponse<RepairAttachment[]>>(`/admin/repairs/${id}/attachments`)
  return response.data.data
}

export async function getAttachmentObjectUrl(id: number): Promise<string> {
  const response = await http.get<Blob>(`/repair-attachments/${id}/content`, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

export async function transitionRepair(
  id: number,
  action: 'accept' | 'complete' | 'close',
  remark: string,
): Promise<RepairTicket> {
  const response = await http.post<ApiResponse<RepairTicket>>(`/admin/repairs/${id}/${action}`, { remark })
  return response.data.data
}
