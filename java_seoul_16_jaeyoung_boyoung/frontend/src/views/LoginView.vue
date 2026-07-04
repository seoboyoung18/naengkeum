<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { API_BASE } from '../api/http'
import LottieIcon from '../components/LottieIcon.vue'
import fridgeAnim from '../assets/fridge-green.json'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const loading = ref(false)
// 소셜 로그인 실패 시 백엔드가 ?error=oauth 로 돌려보낸다.
const error = ref(route.query.error === 'oauth' ? '소셜 로그인에 실패했습니다. 다시 시도해 주세요.' : '')

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value.trim(), password.value, rememberMe.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.push(redirect)
  } catch (e) {
    error.value = e.response?.data?.message || '로그인에 실패했습니다'
  } finally {
    loading.value = false
  }
}

/** 소셜 로그인 시작 — 백엔드 authorize 엔드포인트로 풀페이지 이동(OAuth2 리다이렉트). */
function socialLogin(provider) {
  window.location.href = `${API_BASE}/oauth2/authorization/${provider}`
}
</script>

<template>
  <div class="auth">
    <div class="card">
      <h1 class="title"><LottieIcon :data="fridgeAnim" :size="56" :zoom="1.7" /> <span class="brand">냉큼</span></h1>
      <p class="sub">남은 재료로 큼지막한 행복을</p>

      <form @submit.prevent="onSubmit">
        <label>이메일</label>
        <input v-model="email" type="email" placeholder="test@email.com" autocomplete="username" />

        <label>비밀번호</label>
        <input v-model="password" type="password" placeholder="비밀번호" autocomplete="current-password" />

        <label class="remember">
          <input v-model="rememberMe" type="checkbox" /> 로그인 유지
        </label>

        <p v-if="error" class="err">{{ error }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? '로그인 중…' : '로그인' }}
        </button>
      </form>

      <RouterLink class="link" to="/register">회원가입</RouterLink>

      <div class="divider"><span>또는</span></div>

      <div class="social">
        <button type="button" class="social-btn kakao" @click="socialLogin('kakao')">
          <span class="ico" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18"><path fill="#000" d="M12 3C6.9 3 3 6.3 3 10.3c0 2.6 1.7 4.9 4.3 6.2-.2.7-.7 2.5-.8 2.9 0 0 0 .3.2.4.1.1.3 0 .3 0 .5-.1 2.7-1.8 3.6-2.5.4 0 .8.1 1.1.1 5.1 0 9-3.3 9-7.3S17.1 3 12 3z"/></svg>
          </span>
          카카오로 시작하기
        </button>

        <button type="button" class="social-btn google" @click="socialLogin('google')">
          <span class="ico" aria-hidden="true">
            <svg viewBox="0 0 18 18" width="18" height="18">
              <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.71-1.57 2.68-3.88 2.68-6.62z"/>
              <path fill="#34A853" d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18z"/>
              <path fill="#FBBC05" d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3.01-2.33z"/>
              <path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58C13.46.89 11.43 0 9 0A9 9 0 0 0 .96 4.95l3.01 2.33C4.68 5.16 6.66 3.58 9 3.58z"/>
            </svg>
          </span>
          Google로 시작하기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: #f7f8fa; padding: 24px;
}
.card {
  width: 100%; max-width: 360px;
  background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 16px;
  padding: 28px 22px;
}
.title { font-size: 26px; margin: 0; text-align: center; display: flex; align-items: center; justify-content: center; gap: 4px; transform: translateX(-4px); }
.title .bi { display: inline-flex; color: var(--primary-deep); }
.title .bi :deep(svg) { width: 26px; height: 26px; }
.title .brand { color: var(--primary-deep); font-weight: 800; letter-spacing: -0.3px; }
.sub { color: #888; font-size: 13px; text-align: center; margin: 6px 0 20px; }
form { display: flex; flex-direction: column; }
label { font-size: 13px; color: #555; margin: 10px 0 4px; }
input[type='email'], input[type='password'] {
  padding: 11px 12px; border: 1px solid var(--line); border-radius: 8px; font-size: 14px;
}
.remember { display: flex; align-items: center; gap: 6px; flex-direction: row; margin-top: 14px; color: #444; }
.err { color: #e11d48; font-size: 13px; margin: 12px 0 0; }
.submit {
  margin-top: 18px; padding: 12px; border: none; border-radius: 8px;
  background: var(--primary); color: var(--on-primary); font-size: 15px; font-weight: 700; cursor: pointer;
}
.submit:disabled { opacity: 0.6; cursor: not-allowed; }
.link { display: block; text-align: center; margin-top: 16px; color: var(--primary-deep); font-size: 13px; }

/* 구분선 "또는" */
.divider {
  display: flex; align-items: center; text-align: center;
  margin: 20px 0 14px; color: #aaa; font-size: 12px;
}
.divider::before, .divider::after {
  content: ''; flex: 1; height: 1px; background: var(--line, #e5e7eb);
}
.divider span { padding: 0 12px; }

/* 소셜 로그인 버튼 */
.social { display: flex; flex-direction: column; gap: 10px; }
.social-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 11px 12px; border-radius: 8px; font-size: 14px; font-weight: 600;
  cursor: pointer; border: 1px solid transparent;
}
.social-btn .ico { display: inline-flex; }
.social-btn.kakao { background: #fee500; color: #191600; }
.social-btn.google { background: #fff; color: #3c4043; border-color: var(--line, #dadce0); }
.social-btn:hover { filter: brightness(0.97); }
</style>
