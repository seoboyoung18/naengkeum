import axios from 'axios'
import { API_BASE } from '../config'

// Vue 버전 frontend/src/api/http.js 를 RN으로 포팅한 클라이언트.
// TODO: 토큰 저장은 expo-secure-store / AsyncStorage 로 교체.
export const http = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

let accessToken = null
export function setToken(token) {
  accessToken = token
}

http.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})
