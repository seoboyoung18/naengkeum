import axios from 'axios'
import { API_BASE } from '../config'
import { getToken, saveToken } from '../lib/token'

// 401 발생 시 호출될 핸들러(스토어가 등록) — 순환 import 방지용
let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

export const http = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// 요청: JWT 자동 첨부 (frontend/src/api/http.js 포팅)
http.interceptors.request.use((config) => {
  const t = getToken()
  if (t) config.headers.Authorization = `Bearer ${t}`
  return config
})

// 응답: 401 → 토큰 폐기 + 로그아웃 콜백
http.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      await saveToken(null)
      if (onUnauthorized) onUnauthorized()
    }
    return Promise.reject(error)
  },
)

export default http
