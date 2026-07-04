import http from './http'

/** 로그인 — { email, password, rememberMe } → { token, nickname, memberId } */
export async function login(payload) {
  const { data } = await http.post('/api/auth/login', payload)
  return data
}

/** 회원가입 — { email, password, nickname, allergies?, marketingAgree? } → { memberId } */
export async function register(payload) {
  const { data } = await http.post('/api/auth/register', payload)
  return data
}

/** 이메일 중복 확인 — ?email= → { available } */
export async function checkEmail(email) {
  const { data } = await http.get('/api/auth/check-email', { params: { email } })
  return data
}

/** 마이페이지(내 정보) — 인증 필요 */
export async function fetchMe() {
  const { data } = await http.get('/api/member/me')
  return data
}
