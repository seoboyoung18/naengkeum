import { create } from 'zustand'
import { login as loginApi } from '../api/auth'
import { setUnauthorizedHandler } from '../api/http'
import { getToken, loadToken, saveToken } from '../lib/token'
import { openSocialLogin } from '../lib/socialAuth'

// ── JWT payload 디코드 (UI용 읽기 전용, role 추출) ──
// RN 환경별 atob 부재 대비 순수 JS base64 디코더.
const B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
function atobLite(input) {
  const str = String(input).replace(/=+$/, '')
  let output = ''
  let bc = 0
  let bs = 0
  let buffer
  for (let i = 0; (buffer = str.charAt(i++)); ) {
    buffer = B64.indexOf(buffer)
    if (~buffer) {
      bs = bc % 4 ? bs * 64 + buffer : buffer
      if (bc++ % 4) output += String.fromCharCode(255 & (bs >> ((-2 * bc) & 6)))
    }
  }
  return output
}
function decodePayload(token) {
  try {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atobLite(part))
  } catch (_) {
    return null
  }
}

export const useAuth = create((set, get) => ({
  token: null,
  memberId: null,
  nickname: null,
  role: null,
  ready: false, // 토큰 복원 완료 여부

  isAuthenticated: () => !!get().token,
  isAdmin: () => get().role === 'ADMIN',

  /** 앱 시작 시 SecureStore에서 복원 */
  restore: async () => {
    const t = await loadToken()
    if (t) {
      const p = decodePayload(t)
      set({
        token: t,
        memberId: p?.sub ? Number(p.sub) : null,
        nickname: p?.nickname || null,
        role: p?.role || null,
      })
    }
    set({ ready: true })
  },

  /** 로그인 — POST /api/auth/login */
  login: async (email, password, rememberMe = false) => {
    const data = await loginApi({ email, password, rememberMe })
    await saveToken(data.token)
    const p = decodePayload(data.token)
    set({
      token: data.token,
      memberId: data.memberId,
      nickname: data.nickname,
      role: p?.role || null,
    })
    return data
  },

  /** 원시 JWT로 로그인 상태 반영(소셜 로그인 공용) */
  loginWithToken: async (token) => {
    await saveToken(token)
    const p = decodePayload(token)
    set({
      token,
      memberId: p?.sub ? Number(p.sub) : null,
      nickname: p?.nickname || null,
      role: p?.role || null,
    })
  },

  /** 소셜 로그인 — provider: 'google' | 'kakao'. 취소 시 null 반환 */
  socialLogin: async (provider) => {
    const token = await openSocialLogin(provider)
    if (!token) return null
    await get().loginWithToken(token)
    return token
  },

  setNickname: (nickname) => set({ nickname }),

  logout: async () => {
    await saveToken(null)
    set({ token: null, memberId: null, nickname: null, role: null })
  },
}))

// 401 공통 처리: 토큰 폐기 후 인증 상태 초기화 → 네비게이터가 로그인으로 전환
setUnauthorizedHandler(() => {
  useAuth.setState({ token: null, memberId: null, nickname: null, role: null })
})

export { getToken }
