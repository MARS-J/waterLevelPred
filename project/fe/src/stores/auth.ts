import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '../api/auth'
import { login as loginApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<LoginResponse | null>(loadUserFromStorage())
  const error = ref<string | null>(null)

  const isLoggedIn = computed(() => user.value !== null)
  const displayName = computed(() => user.value?.displayName ?? '')
  const userRole = computed(() => user.value?.role ?? '')

  function loadUserFromStorage(): LoginResponse | null {
    try {
      const stored = localStorage.getItem('auth_user')
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  }

  function saveUserToStorage(u: LoginResponse) {
    localStorage.setItem('auth_user', JSON.stringify(u))
  }

  function clearStorage() {
    localStorage.removeItem('auth_user')
  }

  async function login(username: string, password: string): Promise<boolean> {
    error.value = null
    try {
      const result = await loginApi({ username, password })
      user.value = result
      saveUserToStorage(result)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err?.response?.data?.message ?? '登录失败，请检查网络连接'
      return false
    }
  }

  function logout() {
    user.value = null
    clearStorage()
  }

  return {
    user,
    error,
    isLoggedIn,
    displayName,
    userRole,
    login,
    logout,
  }
})
