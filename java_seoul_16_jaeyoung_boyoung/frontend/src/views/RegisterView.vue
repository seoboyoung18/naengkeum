<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { register as registerApi, checkEmail as checkEmailApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import LottieIcon from '../components/LottieIcon.vue'
import fridgeAnim from '../assets/fridge-green.json'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  email: '',
  password: '',
  passwordConfirm: '',
  nickname: '',
  allergies: '',        // 콤마 구분 입력 → 제출 시 배열로 변환
  marketingAgree: false,
})

// 이메일 중복확인 상태: null(미확인) | true(사용가능) | false(중복)
const emailAvailable = ref(null)
const checking = ref(false)
const loading = ref(false)
const error = ref('')

// 이메일이 바뀌면 중복확인 결과 초기화
watch(() => form.email, () => { emailAvailable.value = null })

const emailValid = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)
const passwordValid = (v) => v.length >= 8 && /[A-Za-z]/.test(v) && /\d/.test(v) && /[^A-Za-z0-9]/.test(v)
const nicknameValid = (v) => v.length >= 2 && v.length <= 10

async function onCheckEmail() {
  error.value = ''
  if (!emailValid(form.email)) { error.value = '올바른 이메일 형식이 아닙니다'; return }
  checking.value = true
  try {
    const { available } = await checkEmailApi(form.email.trim())
    emailAvailable.value = available
  } catch (e) {
    error.value = e.response?.data?.message || '이메일 확인 실패'
  } finally {
    checking.value = false
  }
}

async function onSubmit() {
  error.value = ''
  if (!emailValid(form.email)) return (error.value = '올바른 이메일 형식이 아닙니다')
  if (emailAvailable.value !== true) return (error.value = '이메일 중복확인을 해주세요')
  if (!passwordValid(form.password)) return (error.value = '비밀번호는 영문·숫자·특수문자 포함 8자 이상')
  if (form.password !== form.passwordConfirm) return (error.value = '비밀번호가 일치하지 않습니다')
  if (!nicknameValid(form.nickname)) return (error.value = '닉네임은 2~10자여야 합니다')

  loading.value = true
  try {
    const allergies = form.allergies
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)

    await registerApi({
      email: form.email.trim(),
      password: form.password,
      nickname: form.nickname.trim(),
      allergies,
      marketingAgree: form.marketingAgree,
    })

    // 가입 성공 → 자동 로그인 후 홈으로
    await auth.login(form.email.trim(), form.password)
    router.push('/home')
  } catch (e) {
    error.value = e.response?.data?.message || '회원가입에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth">
    <div class="card">
      <h1 class="title"><LottieIcon :data="fridgeAnim" :size="48" :zoom="1.7" /> <span class="brand">냉큼</span> 회원가입</h1>

      <form @submit.prevent="onSubmit">
        <label>이메일</label>
        <div class="row">
          <input v-model="form.email" type="email" placeholder="test@email.com" autocomplete="username" />
          <button type="button" class="ghost" :disabled="checking" @click="onCheckEmail">
            {{ checking ? '확인중' : '중복확인' }}
          </button>
        </div>
        <p v-if="emailAvailable === true" class="hint ok">✓ 사용 가능한 이메일입니다</p>
        <p v-else-if="emailAvailable === false" class="hint bad">이미 사용 중인 이메일입니다</p>

        <label>비밀번호</label>
        <input v-model="form.password" type="password" placeholder="영문+숫자+특수문자 8자 이상" autocomplete="new-password" />

        <label>비밀번호 확인</label>
        <input v-model="form.passwordConfirm" type="password" placeholder="비밀번호 재입력" autocomplete="new-password" />

        <label>닉네임</label>
        <input v-model="form.nickname" type="text" placeholder="2~10자" />

        <label>알레르기 (선택)</label>
        <input v-model="form.allergies" type="text" placeholder="콤마로 구분 예: 계란, 우유" />

        <label class="check">
          <input v-model="form.marketingAgree" type="checkbox" /> 마케팅 정보 수신 동의 (선택)
        </label>

        <p v-if="error" class="err">{{ error }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? '가입 중…' : '회원가입' }}
        </button>
      </form>

      <RouterLink class="link" to="/login">이미 계정이 있으신가요? 로그인</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.auth { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f7f8fa; padding: 24px; }
.card { width: 100%; max-width: 360px; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 16px; padding: 26px 22px; }
.title { font-size: 22px; margin: 0 0 18px; text-align: center; display: flex; align-items: center; justify-content: center; gap: 4px; transform: translateX(-4px); }
.title .bi { display: inline-flex; color: var(--primary-deep); }
.title .bi :deep(svg) { width: 22px; height: 22px; }
.title .brand { color: var(--primary-deep); font-weight: 800; letter-spacing: -0.3px; }
form { display: flex; flex-direction: column; }
label { font-size: 13px; color: #555; margin: 12px 0 4px; }
input[type='email'], input[type='password'], input[type='text'] {
  padding: 11px 12px; border: 1px solid var(--line); border-radius: 8px; font-size: 14px; width: 100%;
}
.row { display: flex; gap: 8px; }
.row input { flex: 1; }
.ghost { white-space: nowrap; border: 1px solid var(--primary-deep); color: var(--primary-deep); background: #fff; border-radius: 8px; padding: 0 12px; font-size: 13px; cursor: pointer; }
.ghost:disabled { opacity: 0.5; }
.hint { font-size: 12px; margin: 6px 0 0; }
.hint.ok { color: var(--primary-deep); }
.hint.bad { color: #e11d48; }
.check { display: flex; flex-direction: row; align-items: center; gap: 6px; margin-top: 14px; color: #444; font-size: 13px; }
.err { color: #e11d48; font-size: 13px; margin: 12px 0 0; }
.submit { margin-top: 18px; padding: 12px; border: none; border-radius: 8px; background: var(--primary); color: var(--on-primary); font-size: 15px; font-weight: 700; cursor: pointer; }
.submit:disabled { opacity: 0.6; cursor: not-allowed; }
.link { display: block; text-align: center; margin-top: 16px; color: var(--primary-deep); font-size: 13px; }
</style>
