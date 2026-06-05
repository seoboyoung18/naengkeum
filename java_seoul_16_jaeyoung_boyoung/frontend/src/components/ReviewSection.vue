<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { listReviews, createReview, updateReview, deleteReview } from '../api/review'
import { useToast } from '../composables/useToast'

const toast = useToast()

const props = defineProps({ recipeId: { type: [String, Number], required: true } })
const emit = defineEmits(['changed'])

const reviews = ref([])
const stats = ref({ avg: 0, dist: {} })
const total = ref(0)
const loading = ref(false)

// 작성 폼
const draft = reactive({ rating: 5, content: '' })
const submitting = ref(false)
const writeError = ref('')

// 수정 상태
const editingId = ref(null)
const editDraft = reactive({ rating: 5, content: '' })

const myReview = computed(() => reviews.value.find((r) => r.isOwner) || null)

async function load() {
  loading.value = true
  try {
    const data = await listReviews(props.recipeId, { page: 0, size: 50 })
    reviews.value = data.content
    stats.value = data.ratingStats || { avg: 0, dist: {} }
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

async function submit() {
  writeError.value = ''
  if (!draft.content.trim()) { writeError.value = '내용을 입력해 주세요'; return }
  submitting.value = true
  try {
    await createReview({ recipeId: Number(props.recipeId), rating: draft.rating, content: draft.content.trim() })
    draft.content = ''
    draft.rating = 5
    await load()
    emit('changed')
    toast.success('리뷰를 등록했어요')
  } catch (e) {
    if (e.response?.status === 409) writeError.value = '이미 이 레시피에 리뷰를 작성하셨습니다'
    else writeError.value = e.response?.data?.message || '리뷰 작성 실패'
  } finally {
    submitting.value = false
  }
}

function startEdit(r) {
  editingId.value = r.reviewId
  editDraft.rating = r.rating
  editDraft.content = r.content
}
function cancelEdit() { editingId.value = null }

async function saveEdit(r) {
  try {
    await updateReview(r.reviewId, { rating: editDraft.rating, content: editDraft.content.trim() })
    editingId.value = null
    await load()
    emit('changed')
    toast.success('리뷰를 수정했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '수정에 실패했어요')
  }
}

async function remove(r) {
  if (!confirm('리뷰를 삭제할까요?')) return
  try {
    await deleteReview(r.reviewId)
    await load()
    emit('changed')
    toast.success('리뷰를 삭제했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '삭제에 실패했어요')
  }
}

function fmtDate(s) { return s ? s.slice(0, 10) : '' }
function distPct(star) {
  const c = stats.value.dist?.[star] || 0
  return total.value ? Math.round((c / total.value) * 100) : 0
}

onMounted(load)
</script>

<template>
  <div class="reviews">
    <h3 class="sec">리뷰 <span class="cnt">{{ total }}</span></h3>

    <!-- 평점 통계 -->
    <div class="stats" v-if="total > 0">
      <div class="avg">
        <div class="big">{{ Number(stats.avg).toFixed(1) }}</div>
        <div class="stars">{{ '★'.repeat(Math.round(stats.avg)) }}{{ '☆'.repeat(5 - Math.round(stats.avg)) }}</div>
      </div>
      <div class="dist">
        <div v-for="star in [5,4,3,2,1]" :key="star" class="bar-row">
          <span class="lbl">{{ star }}</span>
          <div class="bar"><div class="fill" :style="{ width: distPct(star) + '%' }"></div></div>
        </div>
      </div>
    </div>

    <!-- 작성 폼 (이미 작성한 경우 숨김) -->
    <div v-if="!myReview" class="write">
      <div class="star-input">
        <span v-for="n in 5" :key="n" :class="{ on: n <= draft.rating }" @click="draft.rating = n">★</span>
      </div>
      <textarea v-model="draft.content" rows="2" placeholder="이 레시피 어땠나요? 꿀팁도 환영!"></textarea>
      <p v-if="writeError" class="err">{{ writeError }}</p>
      <button class="submit" :disabled="submitting" @click="submit">{{ submitting ? '등록 중…' : '리뷰 등록' }}</button>
    </div>
    <p v-else class="mine-note">✓ 내가 작성한 리뷰는 아래에서 수정/삭제할 수 있어요.</p>

    <!-- 목록 -->
    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="total === 0" class="muted">첫 리뷰를 남겨보세요!</p>
    <ul v-else class="list">
      <li v-for="r in reviews" :key="r.reviewId" class="item" :class="{ owner: r.isOwner }">
        <div class="top">
          <span class="nick">{{ r.nickname }}<span v-if="r.isOwner" class="me">나</span></span>
          <span class="date">{{ fmtDate(r.createdAt) }}</span>
        </div>

        <template v-if="editingId === r.reviewId">
          <div class="star-input">
            <span v-for="n in 5" :key="n" :class="{ on: n <= editDraft.rating }" @click="editDraft.rating = n">★</span>
          </div>
          <textarea v-model="editDraft.content" rows="2"></textarea>
          <div class="edit-actions">
            <button class="ghost" @click="cancelEdit">취소</button>
            <button class="submit sm" @click="saveEdit(r)">저장</button>
          </div>
        </template>

        <template v-else>
          <div class="rstars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</div>
          <p class="content">{{ r.content }}</p>
          <div v-if="r.isOwner" class="actions">
            <button @click="startEdit(r)">수정</button>
            <button @click="remove(r)">삭제</button>
          </div>
        </template>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.reviews { margin-top: 8px; }
.sec { font-size: 16px; margin: 0 0 12px; }
.sec .cnt { color: #16a34a; font-size: 14px; }

.stats { display: flex; gap: 16px; align-items: center; background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 14px; margin-bottom: 14px; }
.avg { text-align: center; flex: 0 0 80px; }
.avg .big { font-size: 30px; font-weight: 800; color: #16a34a; }
.avg .stars { color: #f59e0b; font-size: 13px; }
.dist { flex: 1; }
.bar-row { display: flex; align-items: center; gap: 8px; margin: 3px 0; }
.bar-row .lbl { font-size: 11px; color: #999; width: 10px; }
.bar { flex: 1; height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.bar .fill { height: 100%; background: #fbbf24; }

.write { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; margin-bottom: 14px; }
.star-input { font-size: 24px; color: #ddd; cursor: pointer; user-select: none; }
.star-input span.on { color: #f59e0b; }
.write textarea, .item textarea { width: 100%; margin-top: 8px; padding: 10px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; font-family: inherit; resize: vertical; box-sizing: border-box; }
.err { color: #e11d48; font-size: 13px; margin: 8px 0 0; }
.submit { margin-top: 10px; padding: 10px 16px; border: none; border-radius: 8px; background: #16a34a; color: #fff; font-weight: 700; cursor: pointer; }
.submit.sm { margin-top: 0; padding: 8px 14px; font-size: 13px; }
.submit:disabled { opacity: .6; }
.mine-note { font-size: 13px; color: #16a34a; margin: 0 0 12px; }

.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.item { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; }
.item.owner { border-color: #bbf7d0; background: #f6fef9; }
.top { display: flex; justify-content: space-between; align-items: center; }
.nick { font-size: 13px; font-weight: 600; }
.nick .me { font-size: 10px; background: #16a34a; color: #fff; border-radius: 4px; padding: 1px 5px; margin-left: 5px; }
.date { font-size: 11px; color: #aaa; }
.rstars { color: #f59e0b; font-size: 13px; margin: 4px 0; }
.content { font-size: 14px; margin: 4px 0 0; white-space: pre-wrap; }
.actions { display: flex; gap: 12px; margin-top: 8px; }
.actions button { border: none; background: none; color: #888; font-size: 12px; cursor: pointer; }
.edit-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.ghost { border: 1px solid #ddd; background: #fff; border-radius: 8px; padding: 8px 14px; font-size: 13px; cursor: pointer; }
.muted { color: #999; }
</style>
