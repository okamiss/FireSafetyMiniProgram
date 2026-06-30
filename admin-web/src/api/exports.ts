import { http } from './http'

async function download(path: string, filename: string): Promise<void> {
  const response = await http.get<Blob>(path, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

export function exportRepairs(): Promise<void> {
  return download('/exports/repairs', '报修数据.xlsx')
}

export function exportTrainingRecords(): Promise<void> {
  return download('/exports/training-records', '培训记录及答题明细.xlsx')
}
