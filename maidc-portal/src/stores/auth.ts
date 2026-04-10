import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import type { UserInfo } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  async function loginAction(username: string, password: string) {
    const res = await loginApi({ username, password })
    const data = res.data.data
    token.value = data.access_token
    setToken(data.access_token, data.refresh_token, data.expires_in)
    userInfo.value = {
      id: data.user.id,
      username: data.user.username,
      real_name: data.user.real_name,
      roles: data.user.roles,
      org_id: data.user.org_id,
      permissions: [],
    }
  }

  async function getUserInfoAction() {
    const res = await getUserInfoApi()
    userInfo.value = res.data.data
    return res.data.data
  }

  async function logoutAction() {
    try { await logoutApi() } finally {
      token.value = null
      userInfo.value = null
      removeToken()
    }
  }

  return { token, userInfo, loginAction, getUserInfoAction, logoutAction }
})
