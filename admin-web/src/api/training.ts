import { http } from './http'
import type { ApiResponse } from './types'

export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE'
export type TrainingTaskStatus = 'DRAFT' | 'PUBLISHED'

export interface TrainingQuestion {
  id: number
  type: QuestionType
  title: string
  options: Record<string, string>
  correctAnswers: string[]
  score: number
  category: string | null
  explanation: string | null
  enabled: boolean
}

export interface CreateQuestionInput {
  type: QuestionType
  title: string
  options: Record<string, string>
  correctAnswers: string[]
  score: number
  category: string
  explanation: string | null
}

export interface TrainingTask {
  id: number
  title: string
  description: string | null
  startAt: string
  endAt: string
  passScore: number
  maxAttempts: number
  status: TrainingTaskStatus
  questionIds: number[]
  targetEnterpriseIds: number[]
  targetUserIds: number[]
  publishedAt: string | null
}

export interface CreateTrainingTaskInput {
  title: string
  description: string | null
  startAt: string
  endAt: string
  passScore: number
  maxAttempts: number
  questionIds: number[]
  targetEnterpriseIds: number[]
  targetUserIds: number[]
}

export interface QuestionImportError { rowNumber: number; message: string }
export interface QuestionImportResult {
  totalRows: number
  importedRows: number
  errors: QuestionImportError[]
}

export interface TrainingRecord {
  id: number
  taskId: number
  taskTitle: string
  userId: number
  userName: string
  enterpriseId: number
  enterpriseName: string
  score: number
  passed: boolean
  attemptNo: number
  submittedAt: string
}

export interface TrainingCertificate {
  id: number
  recordId: number
  taskId: number
  certificateNo: string
  issuedAt: string
  contentUrl: string
}

export async function listQuestions(): Promise<TrainingQuestion[]> {
  const response = await http.get<ApiResponse<TrainingQuestion[]>>('/admin/training/questions')
  return response.data.data
}

export async function createQuestion(input: CreateQuestionInput): Promise<TrainingQuestion> {
  const response = await http.post<ApiResponse<TrainingQuestion>>('/admin/training/questions', input)
  return response.data.data
}

export async function importQuestions(file: File): Promise<QuestionImportResult> {
  const form = new FormData()
  form.append('file', file)
  const response = await http.post<ApiResponse<QuestionImportResult>>('/admin/training/questions/import', form)
  return response.data.data
}

export async function downloadQuestionImportTemplate(): Promise<void> {
  const response = await http.get<Blob>('/admin/training/questions/import-template', { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = '培训题库导入模板.xlsx'
  link.click()
  URL.revokeObjectURL(url)
}

export async function listTasks(): Promise<TrainingTask[]> {
  const response = await http.get<ApiResponse<TrainingTask[]>>('/admin/training/tasks')
  return response.data.data
}

export async function createTask(input: CreateTrainingTaskInput): Promise<TrainingTask> {
  const response = await http.post<ApiResponse<TrainingTask>>('/admin/training/tasks', input)
  return response.data.data
}

export async function publishTask(id: number): Promise<TrainingTask> {
  const response = await http.post<ApiResponse<TrainingTask>>(`/admin/training/tasks/${id}/publish`)
  return response.data.data
}

export async function listRecords(): Promise<TrainingRecord[]> {
  const response = await http.get<ApiResponse<TrainingRecord[]>>('/admin/training/records')
  return response.data.data
}

export async function listCertificates(): Promise<TrainingCertificate[]> {
  const response = await http.get<ApiResponse<TrainingCertificate[]>>('/admin/training/certificates')
  return response.data.data
}

export async function getCertificateObjectUrl(id: number): Promise<string> {
  const response = await http.get<Blob>(`/training/certificates/${id}/content`, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}
