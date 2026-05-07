import axios from 'axios'

export interface FlowRawData {
  id: number
  monitorTime: string
  observedWl: number | null
  predictedWl: number | null
  windSpeed: number | null
  airPressure: number | null
  airTemperature: number | null
  waterTemperature: number | null
  stationCode: string | null
  dataSource: string | null
  qualityStatus: string
  createdAt: string
}

export interface ImportResult {
  successCount: number
  failCount: number
  errors: string[]
}

export interface QualitySummary {
  total: number
  normal: number
  warning: number
  error: number
  normalRate: number
  suitableForTraining: boolean
}

export async function importData(
  file: File,
  dataSource?: string,
  stationCode?: string
): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  if (dataSource) formData.append('dataSource', dataSource)
  if (stationCode) formData.append('stationCode', stationCode)

  const response = await axios.post('/api/data/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data.data
}

export async function listData(params: {
  startTime?: string
  endTime?: string
  dataSource?: string
  stationCode?: string
  qualityStatus?: string
  page?: number
  size?: number
}): Promise<{ content: FlowRawData[]; totalElements: number; totalPages: number; number: number }> {
  const response = await axios.get('/api/data/list', { params })
  return response.data.data
}

export async function getDetail(id: number): Promise<FlowRawData> {
  const response = await axios.get(`/api/data/detail/${id}`)
  return response.data.data
}

export async function deleteData(id: number): Promise<void> {
  await axios.delete(`/api/data/${id}`)
}

export async function batchDelete(ids: number[]): Promise<void> {
  await axios.post('/api/data/batch-delete', ids)
}

export async function updateData(id: number, data: Partial<FlowRawData>): Promise<FlowRawData> {
  const response = await axios.put(`/api/data/${id}`, data)
  return response.data.data
}

export async function checkQuality(): Promise<QualitySummary> {
  const response = await axios.post('/api/data/quality/check')
  return response.data.data
}
