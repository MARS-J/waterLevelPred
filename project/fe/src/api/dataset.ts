import axios from 'axios'

export interface FlowDataset {
  id: number
  name: string
  description: string | null
  timeRangeStart: string | null
  timeRangeEnd: string | null
  recordCount: number
  qualityScore: number | null
  createdAt: string
}

export async function createDataset(request: {
  name: string
  description?: string
  startTime: string
  endTime: string
}): Promise<FlowDataset> {
  const response = await axios.post('/api/datasets/create', request)
  return response.data.data
}

export async function listDatasets(): Promise<FlowDataset[]> {
  const response = await axios.get('/api/datasets/list')
  return response.data.data
}

export async function deleteDataset(id: number): Promise<void> {
  await axios.delete(`/api/datasets/${id}`)
}
