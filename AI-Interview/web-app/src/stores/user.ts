import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface UserInfo {
  userId: number
  nickname: string
  avatarUrl: string
  token: string
}

export const useUserStore = defineStore('user', () => {
  const userId = ref<number>(0)
  const nickname = ref<string>('')
  const avatarUrl = ref<string>('')
  const token = ref<string>('')

  const isLoggedIn = computed(() => !!token.value && userId.value > 0)

  function setUser(info: UserInfo) {
    userId.value = info.userId
    nickname.value = info.nickname
    avatarUrl.value = info.avatarUrl
    token.value = info.token
    localStorage.setItem('token', info.token)
  }

  function clearUser() {
    userId.value = 0
    nickname.value = ''
    avatarUrl.value = ''
    token.value = ''
    localStorage.removeItem('token')
  }

  function restoreToken() {
    const saved = localStorage.getItem('token')
    if (saved) {
      token.value = saved
    }
  }

  return { userId, nickname, avatarUrl, token, isLoggedIn, setUser, clearUser, restoreToken }
})
