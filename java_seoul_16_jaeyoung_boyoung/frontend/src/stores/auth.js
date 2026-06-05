import { defineStore } from 'pinia'
import { login as loginApi, fetchMe as fetchMeApi } from '../api/auth'

export const TOKEN_KEY = 'naengkeum.token'
const MEMBER_KEY = 'naengkeum.memberId'
const NICK_KEY = 'naengkeum.nickname'

/**
 * 인증 스토어 — JWT 토큰 + 회원 식별 정보 보관.
 * localStorage와 동기화하여 새로고침에도 로그인 상태를 유지한다.
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    memberId: null,
    nickname: null,
  }),
  getters: {
    isAuthenticated: (s) => !!s.token,
  },
  actions: {
    /** 앱 시작 시 localStorage에서 복원 */
    restore() {
      const token = localStorage.getItem(TOKEN_KEY)
      if (!token) return
      this.token = token
      this.memberId = Number(localStorage.getItem(MEMBER_KEY)) || null
      this.nickname = localStorage.getItem(NICK_KEY) || null
    },

    /** 로그인 — POST /api/auth/login */
    async login(email, password, rememberMe = false) {
      const data = await loginApi({ email, password, rememberMe })
      this.token = data.token
      this.memberId = data.memberId
      this.nickname = data.nickname
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(MEMBER_KEY, String(data.memberId ?? ''))
      localStorage.setItem(NICK_KEY, data.nickname ?? '')
      return data
    },

    /** 마이페이지 조회 — GET /api/member/me (인증 필요) */
    async fetchMe() {
      const me = await fetchMeApi()
      this.memberId = me.memberId
      this.nickname = me.nickname
      localStorage.setItem(NICK_KEY, me.nickname ?? '')
      return me
    },

    /** 토큰/회원정보 초기화 */
    clearAuth() {
      this.token = null
      this.memberId = null
      this.nickname = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(MEMBER_KEY)
      localStorage.removeItem(NICK_KEY)
    },

    logout() {
      this.clearAuth()
    },
  },
})
