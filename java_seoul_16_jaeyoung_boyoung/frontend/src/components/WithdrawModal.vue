<script setup>
import { ref, onMounted } from 'vue'
import { deleteMe, getMyPage } from '../api/member'
import { useAuthStore } from '../stores/auth'

const emit = defineEmits(['close', 'withdrawn'])
const auth = useAuthStore()

const socialOnly = ref(false)   // 소셜 로그인 전용 계정(비밀번호 없음)
const loadingMe = ref(true)
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    const me = await getMyPage()
    socialOnly.value = !!me.socialOnly
  } catch (_) {
    socialOnly.value = false // 조회 실패 시 보수적으로 비밀번호 입력 노출
  } finally {
    loadingMe.value = false
  }
})

async function onConfirm() {
  error.value = ''
  if (!socialOnly.value && !password.value) {
    error.value = '비밀번호를 입력해 주세요'
    return
  }
  loading.value = true
  try {
    await deleteMe(socialOnly.value ? undefined : password.value)
    auth.clearAuth()
    emit('withdrawn')
  } catch (e) {
    error.value = e.response?.data?.message || '탈퇴에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <div class="sheet-hd">
        <h3>회원 탈퇴</h3>
        <button class="x" @click="emit('close')">✕</button>
      </div>

      <p class="warn">정말 탈퇴하시겠습니까?</p>
      <p class="desc">
        탈퇴하면 계정과 내 데이터(냉장고·조미료·레시피·리뷰 등)에 다시 접근할 수 없습니다.
        이 작업은 되돌릴 수 없어요.
      </p>

      <p v-if="loadingMe" class="muted">확인 중…</p>

      <template v-else>
        <template v-if="!socialOnly">
          <label>비밀번호 확인</label>
          <input
            v-model="password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            autocomplete="current-password"
            @keyup.enter="onConfirm"
          />
        </template>
        <p v-else class="social-note">🔗 소셜 로그인 계정이라 비밀번호 없이 탈퇴됩니다.</p>

        <p v-if="error" class="err">{{ error }}</p>

        <div class="actions">
          <button class="btn cancel" type="button" @click="emit('close')">취소</button>
          <button class="btn danger" type="button" :disabled="loading" @click="onConfirm">
            {{ loading ? '처리 중…' : '탈퇴하기' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(17,24,39,.5); display: flex; align-items: center; justify-content: center; padding: 20px; box-sizing: border-box; z-index: 60; }
.sheet { width: 100%; max-width: 420px; background: #fff; border-radius: 16px; padding: 22px; box-shadow: 0 20px 48px rgba(0,0,0,.18); }
.sheet-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sheet-hd h3 { margin: 0; font-size: 17px; }
.x { border: none; background: none; font-size: 18px; cursor: pointer; color: #888; }
.warn { font-size: 16px; font-weight: 800; color: #ef4444; margin: 8px 0 6px; }
.desc { font-size: 13px; color: #666; line-height: 1.5; margin: 0 0 14px; }
.muted { font-size: 13px; color: #999; margin: 8px 0; }
.social-note { font-size: 13px; color: #555; background: #f1f5f9; border-radius: 8px; padding: 10px 12px; margin: 4px 0 0; }
label { display: block; font-size: 13px; color: #555; margin: 0 0 4px; }
input { padding: 11px 12px; border: 1px solid var(--line, #e5e7eb); border-radius: 8px; font-size: 14px; width: 100%; box-sizing: border-box; }
.err { color: #e11d48; font-size: 13px; margin: 10px 0 0; }
.actions { display: flex; gap: 8px; margin-top: 18px; }
.btn { flex: 1; padding: 12px; border-radius: 8px; font-size: 14px; font-weight: 700; cursor: pointer; border: 1px solid var(--line, #e5e7eb); }
.btn.cancel { background: #fff; color: #555; }
.btn.danger { border: none; background: #ef4444; color: #fff; }
.btn.danger:disabled { opacity: .6; }
</style>
