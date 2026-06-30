import { http } from './http'
import type { ApiResponse } from './types'
import type { RepairStatus } from './repairs'

export interface DashboardSummary {
  repairTotal: number
  repairStatusCounts: Record<RepairStatus, number>
  trainingAssigned: number
  trainingCompleted: number
  trainingPassed: number
  trainingCompletionRate: number
  trainingPassRate: number
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const response = await http.get<ApiResponse<DashboardSummary>>('/dashboard/summary')
  return response.data.data
}
