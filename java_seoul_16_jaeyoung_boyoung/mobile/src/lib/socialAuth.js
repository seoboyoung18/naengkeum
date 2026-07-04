import * as WebBrowser from 'expo-web-browser'
import { makeRedirectUri } from 'expo-auth-session'
import { API_BASE } from '../config'

// 웹(리다이렉트 복귀) 세션 완료 처리 — 네이티브에선 무해.
WebBrowser.maybeCompleteAuthSession()

/**
 * 백엔드 OAuth2 성공 핸들러가 앱 스킴으로 되돌려줄 때 URL에 실려오는 JWT를 추출한다.
 * 형태: naengkeum://oauth#token=<JWT>  (혹은 exp://.../--/oauth#token=...)
 */
function extractToken(url) {
  if (!url) return null
  const m = url.match(/[#?&]token=([^&#]+)/)
  return m ? decodeURIComponent(m[1]) : null
}

/**
 * 소셜 로그인 시작 — 인앱 브라우저로 백엔드 OAuth2 인가요청을 열고,
 * 앱 스킴으로 복귀하면 토큰을 반환한다. 사용자가 취소하면 null.
 *
 * provider: 'google' | 'kakao' (백엔드 registrationId와 일치)
 *
 * 동작:
 *  1) redirectUri = naengkeum://oauth (Expo Go에선 exp://.../--/oauth 자동)
 *  2) ${API}/oauth2/authorization/{provider}?app_redirect=<redirectUri> 를 연다
 *  3) 백엔드가 provider 로그인 완료 후 redirectUri#token=... 로 되돌려줌
 *  Google/Kakao 콘솔에는 백엔드 콜백만 등록돼 있으면 되고, 앱 스킴 등록은 불필요하다.
 */
export async function openSocialLogin(provider) {
  const redirectUri = makeRedirectUri({ scheme: 'naengkeum', path: 'oauth' })
  const authUrl = `${API_BASE}/oauth2/authorization/${provider}?app_redirect=${encodeURIComponent(redirectUri)}`

  const result = await WebBrowser.openAuthSessionAsync(authUrl, redirectUri)
  if (result.type === 'cancel' || result.type === 'dismiss') return null
  if (result.type !== 'success') throw new Error('소셜 로그인이 완료되지 않았어요')

  const token = extractToken(result.url)
  if (!token) throw new Error('로그인 토큰을 받지 못했어요')
  return token
}
