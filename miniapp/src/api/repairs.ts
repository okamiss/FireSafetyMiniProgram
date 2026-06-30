import { API_BASE_URL, authorizationHeader, request, type ApiResponse } from './http'

export type RepairUrgency = 'NORMAL' | 'URGENT'
export type RepairStatus = 'PENDING_ACCEPTANCE' | 'PROCESSING' | 'COMPLETED' | 'CLOSED'

export interface CreateRepairInput {
  urgency: RepairUrgency
  faultType: string
  location: string
  description: string
  contactName: string
  contactPhone: string
}

export interface RepairTicket extends CreateRepairInput {
  id: number
  enterpriseId: number
  reporterUserId: number
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

export function listRepairs() { return request<RepairTicket[]>('/miniapp/repairs') }
export function createRepair(input: CreateRepairInput) {
  return request<RepairTicket>('/miniapp/repairs', { method: 'POST', data: input })
}
export function getRepairHistory(id: number) {
  return request<RepairHistory[]>(`/miniapp/repairs/${id}/history`)
}
export function getRepairAttachments(id: number) {
  return request<RepairAttachment[]>(`/miniapp/repairs/${id}/attachments`)
}

export function uploadRepairAttachment(repairId: number, filePath: string): Promise<RepairAttachment> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/miniapp/repairs/${repairId}/attachments`,
      filePath,
      name: 'file',
      header: authorizationHeader(),
      success: response => {
        let body: ApiResponse<RepairAttachment> | null = null
        try { body = JSON.parse(response.data) as ApiResponse<RepairAttachment> }
        catch { reject(new Error('上传响应格式错误')); return }
        if (response.statusCode >= 200 && response.statusCode < 300 && body.status === 'ok') {
          resolve(body.data); return
        }
        reject(new Error(body.message || '照片上传失败'))
      },
      fail: error => reject(new Error(error.errMsg || '照片上传失败')),
    })
  })
}

export function downloadAttachment(attachmentId: number): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: `${API_BASE_URL}/repair-attachments/${attachmentId}/content`,
      header: authorizationHeader(),
      success: response => response.statusCode === 200
        ? resolve(response.tempFilePath)
        : reject(new Error('照片下载失败')),
      fail: error => reject(new Error(error.errMsg || '照片下载失败')),
    })
  })
}
