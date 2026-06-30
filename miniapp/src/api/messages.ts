import { request } from './http'

export type ExternalDeliveryStatus = 'PENDING' | 'SENT' | 'SKIPPED' | 'FAILED'
export interface StationMessage {
  id: number
  messageType: string
  title: string
  content: string
  businessType: string | null
  businessId: number | null
  read: boolean
  readAt: string | null
  externalStatus: ExternalDeliveryStatus
  createdAt: string
}
export interface SubscriptionConfig { enabled: boolean; templateIds: string[] }

export function listMessages() { return request<StationMessage[]>('/miniapp/messages') }
export function markMessageRead(id: number) {
  return request<StationMessage>(`/miniapp/messages/${id}/read`, { method: 'POST' })
}
export function getSubscriptionConfig() {
  return request<SubscriptionConfig>('/miniapp/messages/subscription-config')
}
