import type { LoginRequest, LoginResponse } from '@/features/auth/model/authTypes.ts'
import { apiRequest } from '@/shared/api/apiClient.ts'

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: request,
    authenticated: false,
  })
}
