<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyPage, updateMe, listMyReviews } from '../api/member'
import { listWishlist, removeRecipeWish, removeAiWish } from '../api/wishlist'
import { useAuthStore } from '../stores/auth'
import AiRecipeModal from '../components/AiRecipeModal.vue'

const router = useRouter()
const auth = useAuthStore()

const me = ref(null)
const wishes = ref([])
const reviews = ref([])
const loading = ref(true)

// 프로필 수정 모달
const showEdit = ref(false)
const edit = reactive({ nickname: '', allergies: '', currentPassword: '', newPassword: '' })
const editError = ref('')
const saving = ref(false)

// AI 레시피 상세 모달
const aiModalId = ref(null)

async function loadAll() {
  loading.value = true
  try {
    const [m, w, r] = await Promise.all([
      getMyPage(),
      listWishlist({ page: 0, size: 50 }),
      listMyReviews({ page: 0, size: 50 }),
    ])
    me.value = m
    wishes.value = w.content
    reviews.value = r.content
  } finally {
    loading.value = false
  }
}

function openEdit() {
  edit.nickname = me.value.nickname
  edit.allergies = (me.value.allergies || []).join(', ')
  edit.currentPassword = ''
  edit.newPassword = ''
  editError.value = ''
  showEdit.value = true
}

async function saveEdit() {
  editError.value = ''
  const payload = {}
  if (edit.nickname && edit.nickname !== me.value.nickname) payload.nickname = edit.nickname.trim()
  const allergies = edit.allergies.split(',').map((s) => s.trim()).filter(Boolean)
  payload.allergies = allergies
  if (edit.newPassword) {
    if (!edit.currentPassword) { editError.value = '현재 비밀번호를 입력해 주세요'; return }
    payload.currentPassword = edit.currentPassword
    payload.newPassword = edit.newPassword
  }
  saving.value = true
  try {
    const updated = await updateMe(payload)
    me.value = updated
    auth.nickname = updated.nickname
    localStorage.setItem('naengkeum.nickname', updated.nickname ?? '')
    showEdit.value = false
  } catch (e) {
    editError.value = e.response?.data?.message || '수정 실패'
  } finally {
    saving.value = false
  }
}

async function removeWish(item) {
  if (!confirm('찜을 해제할까요?')) return
  try {
    if (item.type === 'AI') await removeAiWish(item.aiRecipeId)
    else await removeRecipeWish(item.recipeId)
    wishes.value = wishes.value.filter((w) => w.wishlistId !== item.wishlistId)
  } catch (e) {
    alert(e.response?.data?.message || '해제 실패')
  }
}

function openWish(item) {
  if (item.type === 'AI') aiModalId.value = item.aiRecipeId
  else router.push({ name: 'recipe-detail', params: { recipeId: item.recipeId } })
}

function logout() { auth.logout(); router.push({ name: 'login' }) }
function fmtDate(s) { return s ? s.slice(0, 10) : '' }

onMounted(loadAll)
</script>

<template>
  <section>
    <h2 class="h">마이페이지</h2>
    <p v-if="loading" class="muted">불러오는 중…</p>

    <template v-else-if="me">
      <!-- 프로필 -->
      <div class="profile">
        <div class="prow">
          <div>
            <div class="nick">{{ me.nickname }}</div>
            <div class="email">{{ me.email }}</div>
          </div>
          <button class="edit" @click="openEdit">정보 수정</button>
        </div>
        <div class="allergy" v-if="me.allergies?.length">
          <span class="lbl">알레르기</span>
          <span v-for="a in me.allergies" :key="a" class="chip">{{ a }}</span>
        </div>
        <ul class="stats">
          <li><span>{{ me.stats.fridgeCount }}</span>냉장고</li>
          <li><span>{{ me.stats.wishlistCount }}</span>찜</li>
          <li><span>{{ me.stats.reviewCount }}</span>리뷰</li>
          <li><span>{{ me.stats.followerCount }}</span>팔로워</li>
        </ul>
      </div>

      <!-- 찜 목록 -->
      <h3 class="sec">찜한 레시피 <span class="cnt">{{ wishes.length }}</span></h3>
      <p v-if="wishes.length === 0" class="muted">아직 찜한 레시피가 없어요.</p>
      <ul v-else class="list">
        <li v-for="w in wishes" :key="w.wishlistId" class="card" @click="openWish(w)">
          <span class="type" :class="w.type === 'AI' ? 'ai' : 'db'">{{ w.type === 'AI' ? '🤖 AI' : '📖' }}</span>
          <div class="info">
            <div class="title">{{ w.title }}</div>
            <div class="sub"><span v-if="w.cookTime">⏱ {{ w.cookTime }}분</span></div>
          </div>
          <button class="rm" @click.stop="removeWish(w)">♥</button>
        </li>
      </ul>

      <!-- 내 리뷰 -->
      <h3 class="sec">내 리뷰 <span class="cnt">{{ reviews.length }}</span></h3>
      <p v-if="reviews.length === 0" class="muted">작성한 리뷰가 없어요.</p>
      <ul v-else class="list">
        <li v-for="r in reviews" :key="r.reviewId" class="rcard" @click="router.push({ name: 'recipe-detail', params: { recipeId: r.recipeId } })">
          <div class="rtop">
            <span class="rtitle">{{ r.recipeTitle }}</span>
            <span class="date">{{ fmtDate(r.createdAt) }}</span>
          </div>
          <div class="rstars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</div>
          <p class="rcontent">{{ r.content }}</p>
        </li>
      </ul>

      <button class="logout" @click="logout">로그아웃</button>
    </template>

    <!-- 프로필 수정 모달 -->
    <div v-if="showEdit" class="overlay" @click.self="showEdit = false">
      <div class="sheet">
        <div class="shd"><h3>정보 수정</h3><button class="x" @click="showEdit = false">✕</button></div>
        <label>닉네임</label>
        <input v-model="edit.nickname" type="text" />
        <label>알레르기 (콤마 구분)</label>
        <input v-model="edit.allergies" type="text" placeholder="예: 계란, 우유" />
        <div class="pw">
          <label>비밀번호 변경 (선택)</label>
          <input v-model="edit.currentPassword" type="password" placeholder="현재 비밀번호" autocomplete="current-password" />
          <input v-model="edit.newPassword" type="password" placeholder="새 비밀번호" autocomplete="new-password" />
        </div>
        <p v-if="editError" class="err">{{ editError }}</p>
        <button class="save" :disabled="saving" @click="saveEdit">{{ saving ? '저장 중…' : '저장' }}</button>
      </div>
    </div>

    <!-- AI 레시피 상세 모달 -->
    <AiRecipeModal v-if="aiModalId" :ai-recipe-id="aiModalId" @close="aiModalId = null" />
  </section>
</template>

<style scoped>
.profile { background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 16px; margin-bottom: 18px; }
.prow { display: flex; align-items: center; justify-content: space-between; }
.nick { font-size: 18px; font-weight: 700; }
.email { font-size: 13px; color: #999; margin-top: 2px; }
.edit { border: 1px solid #ddd; background: #fff; border-radius: 8px; padding: 7px 12px; font-size: 13px; cursor: pointer; }
.allergy { margin-top: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.allergy .lbl { font-size: 12px; color: #999; }
.chip { font-size: 12px; background: #fef2f2; color: #ef4444; border-radius: 999px; padding: 3px 10px; }
.stats { list-style: none; display: flex; gap: 8px; padding: 0; margin: 14px 0 0; }
.stats li { flex: 1; background: #f5f7f9; border-radius: 10px; padding: 10px 4px; text-align: center; font-size: 11px; color: #666; }
.stats li span { display: block; font-size: 17px; font-weight: 800; color: #16a34a; }

.sec { font-size: 16px; margin: 20px 0 10px; }
.sec .cnt { color: #16a34a; font-size: 14px; }
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.card { display: flex; align-items: center; gap: 12px; background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; cursor: pointer; }
.type { font-size: 13px; flex: 0 0 auto; }
.type.ai { color: #7c3aed; font-weight: 700; }
.info { flex: 1; min-width: 0; }
.title { font-size: 15px; font-weight: 600; }
.sub { font-size: 12px; color: #999; margin-top: 2px; }
.rm { border: none; background: none; color: #ef4444; font-size: 20px; cursor: pointer; }

.rcard { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; cursor: pointer; }
.rtop { display: flex; justify-content: space-between; align-items: center; }
.rtitle { font-size: 14px; font-weight: 600; }
.date { font-size: 11px; color: #aaa; }
.rstars { color: #f59e0b; font-size: 13px; margin: 4px 0; }
.rcontent { font-size: 14px; margin: 0; color: #555; }

.logout { width: 100%; margin-top: 22px; padding: 12px; border: 1px solid #eee; background: #fff; border-radius: 8px; color: #888; font-size: 14px; cursor: pointer; }
.muted { color: #999; }

.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: flex-end; justify-content: center; z-index: 60; }
.sheet { width: 100%; max-width: 480px; background: #fff; border-radius: 16px 16px 0 0; padding: 18px 18px 26px; }
.shd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.shd h3 { margin: 0; font-size: 17px; }
.x { border: none; background: none; font-size: 18px; cursor: pointer; color: #888; }
.sheet label { display: block; font-size: 13px; color: #555; margin: 12px 0 4px; }
.sheet input { width: 100%; padding: 11px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.pw { margin-top: 6px; }
.pw input { margin-bottom: 8px; }
.err { color: #e11d48; font-size: 13px; margin: 10px 0 0; }
.save { width: 100%; margin-top: 16px; padding: 13px; border: none; border-radius: 8px; background: #16a34a; color: #fff; font-weight: 700; cursor: pointer; }
.save:disabled { opacity: .6; }
</style>
