import axios from 'axios'
import router from '../router'
import { useAuthStore, TOKEN_KEY } from '../stores/auth'

/**
 * 공통 Axios 인스턴스.
 * - baseURL: 백엔드(Spring Boot). 백엔드 CORS가 localhost:5173 허용.
 * - 요청 인터셉터: 토큰이 있으면 Authorization: Bearer 자동 첨부.
 * - 응답 인터셉터: 401(미인증/만료/위조)이면 로그아웃 후 /login으로.
 */
/** 백엔드 base URL — axios 인스턴스와 SSE fetch에서 공통 사용 */
export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const http = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

// ----- 요청: JWT 자동 첨부 -----
http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ----- 응답: 401 공통 처리 -----
http.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      // 백엔드 9종 에러코드 중 UNAUTHORIZED / TOKEN_EXPIRED / INVALID_TOKEN
      try {
        useAuthStore().clearAuth()
      } catch (_) {
        // pinia 미초기화 등 — 무시
      }
      const current = router.currentRoute.value
      if (current.name !== 'login') {
        router.push({ name: 'login', query: { redirect: current.fullPath } })
      }
    }
    return Promise.reject(error)
  }
)

export default http
