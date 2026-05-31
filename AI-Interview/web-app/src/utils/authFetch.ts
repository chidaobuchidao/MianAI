import { useUserStore } from '@/stores/user'

const LOGIN_EXPIRED_MESSAGE = '登录状态已失效，请重新登录后再试'

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

function resolveToken(): string {
  const userStore = useUserStore()
  if (!userStore.token) {
    userStore.restoreToken()
  }
  return userStore.token || localStorage.getItem('token') || ''
}

export async function authFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const userStore = useUserStore()
  const token = resolveToken()

  if (!token) {
    userStore.clearUser()
    redirectToLogin()
    throw new Error(LOGIN_EXPIRED_MESSAGE)
  }

  const headers = new Headers(init.headers)
  if (!headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(input, { ...init, headers })

  if (response.status === 401 || response.status === 403) {
    userStore.clearUser()
    redirectToLogin()
    throw new Error(LOGIN_EXPIRED_MESSAGE)
  }

  return response
}
