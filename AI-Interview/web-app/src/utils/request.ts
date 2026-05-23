import { useUserStore } from '@/stores/user'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const BASE_URL = import.meta.env.VITE_API_BASE || ''

async function request<T>(method: string, url: string, data?: unknown, isFormData?: boolean): Promise<ApiResponse<T>> {
  const userStore = useUserStore()

  const headers: Record<string, string> = {}

  if (!isFormData) {
    headers['Content-Type'] = 'application/json'
  }

  if (userStore.token) {
    headers['Authorization'] = `Bearer ${userStore.token}`
  }

  const config: RequestInit = {
    method,
    headers
  }

  if (data && method !== 'GET') {
    config.body = isFormData ? data as FormData : JSON.stringify(data)
  }

  const fullUrl = `${BASE_URL}${url}`

  const response = await fetch(fullUrl, config)

  if (response.status === 401) {
    userStore.clearUser()
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }

  if (!response.ok) {
    const errorText = await response.text()
    let msg = errorText || `HTTP ${response.status}`
    try {
      const errJson = JSON.parse(errorText)
      msg = errJson.message || errJson.error || msg
    } catch {}
    throw new Error(msg)
  }

  const json = await response.json()
  return json
}

export function get<T>(url: string): Promise<ApiResponse<T>> {
  return request<T>('GET', url)
}

export function post<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  return request<T>('POST', url, data)
}

export function postForm<T>(url: string, formData: FormData): Promise<ApiResponse<T>> {
  return request<T>('POST', url, formData, true)
}

export function put<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  return request<T>('PUT', url, data)
}

export function del<T>(url: string): Promise<ApiResponse<T>> {
  return request<T>('DELETE', url)
}
