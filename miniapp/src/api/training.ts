import { API_BASE_URL, authorizationHeader, request } from './http'

export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE'
export type TrainingTaskStatus = 'DRAFT' | 'PUBLISHED'

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

export interface AssignedTrainingTask {
  task: TrainingTask
  attemptsUsed: number
  bestScore: number
  passed: boolean
  completedAt: string | null
}

export interface PaperQuestion {
  id: number
  type: QuestionType
  title: string
  options: Record<string, string>
  score: number
  category: string | null
}

export interface TrainingPaper {
  task: TrainingTask
  attemptsUsed: number
  questions: PaperQuestion[]
}

export interface AttemptDetail {
  questionId: number
  userAnswers: string[]
  correctAnswers: string[]
  correct: boolean
  awardedScore: number
  explanation: string | null
}

export interface AttemptResult {
  recordId: number
  score: number
  passed: boolean
  attemptNo: number
  submittedAt: string
  details: AttemptDetail[]
}

export interface TrainingCertificate {
  id: number
  recordId: number
  taskId: number
  certificateNo: string
  issuedAt: string
  contentUrl: string
}

export function listAssignedTasks() {
  return request<AssignedTrainingTask[]>('/miniapp/training/tasks')
}

export function getTrainingPaper(taskId: number) {
  return request<TrainingPaper>(`/miniapp/training/tasks/${taskId}/paper`)
}

export function submitTrainingAnswers(taskId: number, answers: Record<number, string[]>) {
  return request<AttemptResult>(`/miniapp/training/tasks/${taskId}/submit`, {
    method: 'POST', data: { answers },
  })
}

export function listTrainingCertificates() {
  return request<TrainingCertificate[]>('/miniapp/training/certificates')
}

export function downloadTrainingCertificate(id: number): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: `${API_BASE_URL}/training/certificates/${id}/content`,
      header: authorizationHeader(),
      success: response => response.statusCode === 200
        ? resolve(response.tempFilePath)
        : reject(new Error('证书下载失败')),
      fail: error => reject(new Error(error.errMsg || '证书下载失败')),
    })
  })
}
