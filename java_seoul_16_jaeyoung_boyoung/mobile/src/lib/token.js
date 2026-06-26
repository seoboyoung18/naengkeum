import * as SecureStore from 'expo-secure-store'

const KEY = 'naengkeum_token' // SecureStore 키는 영숫자/._- 만 허용

// 인터셉터에서 동기 접근하려고 메모리에도 보관
let current = null

export const getToken = () => current

export async function loadToken() {
  try {
    current = await SecureStore.getItemAsync(KEY)
  } catch (_) {
    current = null
  }
  return current
}

export async function saveToken(token) {
  current = token || null
  try {
    if (token) await SecureStore.setItemAsync(KEY, token)
    else await SecureStore.deleteItemAsync(KEY)
  } catch (_) {
    /* 무시 */
  }
}
