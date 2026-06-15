import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface UserInfo {
  userId: number
  nickname: string
  avatarUrl: string
  token: string
  email?: string
  needBindEmail?: boolean
}

export const useUserStore = defineStore('user', () => {
  const userId = ref<number>(0)
  const nickname = ref<string>('')
  const avatarUrl = ref<string>('')
  const token = ref<string>('')
  const isAdmin = ref<boolean>(false)
  const email = ref<string>('')
  const needBindEmail = ref<boolean>(false)

  const isLoggedIn = computed(() => !!token.value && userId.value > 0)

  function setUser(info: UserInfo) {
    userId.value = info.userId
    nickname.value = info.nickname
    avatarUrl.value = info.avatarUrl
    token.value = info.token
    email.value = info.email || ''
    needBindEmail.value = info.needBindEmail || false
    localStorage.setItem('token', info.token)
    localStorage.setItem('userId', String(info.userId))
    localStorage.setItem('nickname', info.nickname)
    localStorage.setItem('avatarUrl', info.avatarUrl || '')
    localStorage.setItem('email', info.email || '')
    localStorage.setItem('needBindEmail', info.needBindEmail ? '1' : '0')
  }

  function setAdmin(admin: boolean) {
    isAdmin.value = admin
    localStorage.setItem('isAdmin', admin ? '1' : '0')
  }

  function clearUser() {
    userId.value = 0
    nickname.value = ''
    avatarUrl.value = ''
    token.value = ''
    isAdmin.value = false
    email.value = ''
    needBindEmail.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('nickname')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('isAdmin')
    localStorage.removeItem('email')
    localStorage.removeItem('needBindEmail')
  }

  function restoreToken() {
    const saved = localStorage.getItem('token')
    if (saved) {
      token.value = saved
      userId.value = Number(localStorage.getItem('userId')) || 0
      nickname.value = localStorage.getItem('nickname') || ''
      avatarUrl.value = localStorage.getItem('avatarUrl') || ''
      isAdmin.value = localStorage.getItem('isAdmin') === '1'
      email.value = localStorage.getItem('email') || ''
      needBindEmail.value = localStorage.getItem('needBindEmail') === '1'
    }
  }

  return { userId, nickname, avatarUrl, token, isAdmin, email, needBindEmail, isLoggedIn, setUser, setAdmin, clearUser, restoreToken }
})
