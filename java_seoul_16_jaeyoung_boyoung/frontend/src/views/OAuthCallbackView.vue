<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const error = ref('')

onMounted(() => {
  // 백엔드 OAuth2SuccessHandler가 토큰을 URL fragment(#token=...)로 전달한다.
  const hash = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash
  const params = new URLSearchParams(hash)
  const token = params.get('token')

  if (!token) {
    error.value = '로그인 정보를 받지 못했습니다.'
    setTimeout(() => router.replace({ name: 'login', query: { error: 'oauth' } }), 1200)
    return
  }

  try {
    auth.loginWithToken(token)
    // 토큰이 노출된 주소를 히스토리에서 지우고 홈으로.
    router.replace('/home')
  } catch (_) {
    error.value = '로그인 처리에 실패했습니다.'
    setTimeout(() => router.replace({ name: 'login', query: { error: 'oauth' } }), 1200)
  }
})
</script>

<template>
  <div class="callback">
    <template v-if="!error">
      <div class="spinner" />
      <p>로그인 중…</p>
    </template>
    <p v-else class="err">{{ error }}</p>
  </div>
</template>

<style scoped>
.callback {
  min-height: 100vh;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 14px; background: #f7f8fa; color: #555;
}
.spinner {
  width: 36px; height: 36px; border-radius: 50%;
  border: 3px solid var(--line, #e5e7eb); border-top-color: var(--primary, #22c55e);
  animation: spin 0.8s linear infinite;
}
.err { color: #e11d48; font-size: 14px; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
