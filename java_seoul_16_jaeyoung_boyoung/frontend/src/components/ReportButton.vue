<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { createReport } from '../api/report'
import { useToast } from '../composables/useToast'

/**
 * 콘텐츠 신고 버튼 + 사유 입력 모달 (레시피/리뷰 공용).
 * - 비로그인 시 로그인 페이지로 유도.
 * - 이미 신고한 대상(409)은 안내 후 닫음.
 */
const props = defineProps({
  targetType: { type: String, required: true }, // 'RECIPE' | 'REVIEW'
  targetId: { type: [String, Number], required: true },
  label: { type: String, default: '신고' },
  compact: { type: Boolean, default: false }, // 리뷰처럼 작은 링크형
})

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const open = ref(false)
const reason = ref('')
const submitting = ref(false)

const targetLabel = props.targetType === 'REVIEW' ? '리뷰' : '레시피'

function start() {
  if (!auth.isAuthenticated) {
    toast.info('로그인 후 신고할 수 있어요')
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  reason.value = ''
  open.value = true
}

function close() {
  if (submitting.value) return
  open.value = false
}

async function submit() {
  submitting.value = true
  try {
    await createReport({
      targetType: props.targetType,
      targetId: Number(props.targetId),
      reason: reason.value.trim() || undefined,
    })
    toast.success('신고가 접수되었어요. 검토 후 조치할게요.')
    open.value = false
  } catch (e) {
    if (e.response?.status === 409) toast.info('이미 신고한 ' + targetLabel + '예요')
    else toast.error(e.response?.data?.message || '신고에 실패했어요')
    open.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="report-trigger"
    :class="{ compact }"
    @click="start"
  >
    <span aria-hidden="true">🚩</span> {{ label }}
  </button>

  <!-- 사유 입력 모달 -->
  <Teleport to="body">
    <div v-if="open" class="report-overlay" @click.self="close">
      <div class="report-modal" role="dialog" aria-modal="true">
        <h3 class="rm-title">{{ targetLabel }} 신고</h3>
        <p class="rm-sub">신고 사유를 알려주시면 검토에 도움이 돼요. (선택)</p>
        <textarea
          v-model="reason"
          rows="3"
          maxlength="255"
          placeholder="예) 욕설/광고/허위 정보 등"
        ></textarea>
        <div class="rm-count">{{ reason.length }}/255</div>
        <div class="rm-actions">
          <button class="ghost" :disabled="submitting" @click="close">취소</button>
          <button class="danger" :disabled="submitting" @click="submit">
            {{ submitting ? '접수 중…' : '신고하기' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.report-trigger {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--text-soft);
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--r-sm);
  padding: 8px 14px;
  cursor: pointer;
}
.report-trigger:hover { color: #e11d48; border-color: #fecdd3; }
.report-trigger.compact {
  border: none;
  background: none;
  padding: 0;
  font-size: 12px;
  color: #aaa;
}
.report-trigger.compact:hover { color: #e11d48; }

.report-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.report-modal {
  width: 100%; max-width: 380px;
  background: var(--surface);
  border-radius: var(--r-md);
  padding: 22px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, .25);
}
.rm-title { margin: 0 0 6px; font-size: 18px; }
.rm-sub { margin: 0 0 14px; font-size: 13px; color: var(--text-soft); }
.report-modal textarea {
  width: 100%; box-sizing: border-box;
  padding: 10px; border: 1px solid var(--line); border-radius: 8px;
  font-size: 14px; font-family: inherit; resize: vertical;
}
.rm-count { text-align: right; font-size: 11px; color: #aaa; margin-top: 4px; }
.rm-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 14px; }
.rm-actions button { border-radius: var(--r-sm); padding: 10px 18px; font-size: 14px; font-weight: 700; cursor: pointer; }
.rm-actions button:disabled { opacity: .6; cursor: default; }
.ghost { border: 1px solid var(--line); background: #fff; color: var(--text); }
.danger { border: none; background: #e11d48; color: #fff; }
.danger:hover:not(:disabled) { background: #be123c; }
</style>
