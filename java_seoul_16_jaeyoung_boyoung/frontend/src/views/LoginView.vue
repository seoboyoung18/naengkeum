<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const loading = ref(false)
const error = ref('')

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
</script>

<template>
  <div class="auth">
    <div class="card">
      <h1 class="title">🧊 냉큼</h1>
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
.title { font-size: 26px; margin: 0; text-align: center; }
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
</style>
