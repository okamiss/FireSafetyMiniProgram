import { http } from './http'
import type { ApiResponse } from './types'

export type ExternalDeliveryStatus = 'PENDING' | 'SENT' | 'SKIPPED' | 'FAILED'
export interface StationMessage {
  id: number
  enterpriseId: number
  recipientUserId: number
  messageType: string
  title: string
  content: string
  businessType: string | null
  businessId: number | null
  read: boolean
  readAt: string | null
  externalStatus: ExternalDeliveryStatus
  externalErrorCode: string | null
  externalErrorMessage: string | null
  externalSentAt: string | null
  createdAt: string
}

export async function listMessages(): Promise<StationMessage[]> {
  const response = await http.get<ApiResponse<StationMessage[]>>('/admin/messages')
  return response.data.data
}
