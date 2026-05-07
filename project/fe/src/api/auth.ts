import axios from 'axios'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  id: number
  username: string
  displayName: string
  role: string
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await axios.post('/api/auth/login', data)
  return response.data.data
}
