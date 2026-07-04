import { defineStore } from 'pinia'
import { login as loginApi, fetchMe as fetchMeApi } from '../api/auth'

export const TOKEN_KEY = 'naengkeum.token'
const MEMBER_KEY = 'naengkeum.memberId'
const NICK_KEY = 'naengkeum.nickname'

/** JWT payload 디코드 (서명 검증은 서버 몫, 여기선 UI용 읽기 전용). 실패 시 null. */
function decodePayload(token) {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch (_) {
    return null
  }
}

/** JWT payload에서 role 추출. */
function roleFromToken(token) {
  return decodePayload(token)?.role || null
}

/**
 * 인증 스토어 — JWT 토큰 + 회원 식별 정보 보관.
 * localStorage와 동기화하여 새로고침에도 로그인 상태를 유지한다.
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    memberId: null,
    nickname: null,
    role: null, // JWT에서 디코드 (ADMIN 게이팅용)
  }),
  getters: {
    isAuthenticated: (s) => !!s.token,
    isAdmin: (s) => s.role === 'ADMIN',
  },
  actions: {
    /** 앱 시작 시 localStorage에서 복원 */
    restore() {
      const token = localStorage.getItem(TOKEN_KEY)
      if (!token) return
      this.token = token
      this.memberId = Number(localStorage.getItem(MEMBER_KEY)) || null
      this.nickname = localStorage.getItem(NICK_KEY) || null
      this.role = roleFromToken(token)
    },

    /** 로그인 — POST /api/auth/login */
    async login(email, password, rememberMe = false) {
      const data = await loginApi({ email, password, rememberMe })
      this.token = data.token
      this.memberId = data.memberId
      this.nickname = data.nickname
      this.role = roleFromToken(data.token)
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(MEMBER_KEY, String(data.memberId ?? ''))
      localStorage.setItem(NICK_KEY, data.nickname ?? '')
      return data
    },

    /**
     * 소셜 로그인 콜백 — 백엔드가 발급한 JWT 하나로 상태를 복원한다.
     * 토큰 payload(sub=memberId, nickname, role)에서 직접 읽어 추가 호출 없음.
     */
    loginWithToken(token) {
      const payload = decodePayload(token)
      if (!payload) throw new Error('유효하지 않은 토큰입니다')
      this.token = token
      this.memberId = payload.sub ? Number(payload.sub) : null
      this.nickname = payload.nickname || null
      this.role = payload.role || null
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(MEMBER_KEY, String(this.memberId ?? ''))
      localStorage.setItem(NICK_KEY, this.nickname ?? '')
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
      this.role = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(MEMBER_KEY)
      localStorage.removeItem(NICK_KEY)
    },

    logout() {
      this.clearAuth()
    },
  },
})
