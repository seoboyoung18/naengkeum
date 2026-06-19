<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyPage, updateMe, listMyReviews, uploadProfilePhoto } from '../api/member'
import { API_BASE } from '../api/http'
import { listWishlist, removeRecipeWish, removeAiWish } from '../api/wishlist'
import { useAuthStore } from '../stores/auth'
import AiRecipeModal from '../components/AiRecipeModal.vue'
import MyRecipeList from '../components/MyRecipeList.vue'
import { useToast } from '../composables/useToast'
import InlineIcon from '../components/InlineIcon.vue'
import clockSvg from '../assets/icons/clock-outline.svg?raw'
import userSvg from '../assets/icons/user.svg?raw'
import botUrl from '../assets/icons/message-bot.svg'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

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

// 프로필 사진
const avatarInput = ref(null)
const avatarUploading = ref(false)
const avatarUrl = computed(() => {
  const u = me.value?.profileImageUrl
  if (!u) return null
  return u.startsWith('/') ? API_BASE + u : u
})
function pickAvatar() {
  if (!avatarUploading.value) avatarInput.value?.click()
}
async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast.error('jpg, png, webp 이미지만 올릴 수 있어요')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.error('이미지는 5MB 이하여야 해요')
    return
  }
  avatarUploading.value = true
  try {
    const { profileImageUrl } = await uploadProfilePhoto(file)
    me.value.profileImageUrl = profileImageUrl
    toast.success('프로필 사진을 변경했어요')
  } catch (err) {
    toast.error(err.response?.data?.message || '업로드에 실패했어요')
  } finally {
    avatarUploading.value = false
  }
}

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
    toast.success('정보를 수정했어요')
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
    toast.success('찜을 해제했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '해제에 실패했어요')
  }
}

function openWish(item) {
  if (item.type === 'AI') aiModalId.value = item.aiRecipeId
  else router.push({ name: 'recipe-detail', params: { recipeId: item.recipeId } })
}

function fmtDate(s) { return s ? s.slice(0, 10) : '' }

onMounted(loadAll)
</script>

<template>
  <section>
    <h2 class="h">마이페이지</h2>
    <p v-if="loading" class="muted">불러오는 중…</p>

    <template v-else-if="me">
      <!-- 프로필 + 스탯 -->
      <div class="profile">
        <div class="prow">
          <div class="pleft">
            <button class="avatar-wrap" type="button" @click="pickAvatar" :title="avatarUploading ? '업로드 중…' : '프로필 사진 변경'">
              <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" alt="프로필 사진" />
              <span v-else class="avatar"><template v-if="me.nickname">{{ me.nickname[0] }}</template><InlineIcon v-else :svg="userSvg" :size="28" /></span>
              <span class="cam">{{ avatarUploading ? '…' : '📷' }}</span>
            </button>
            <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onAvatarChange" />
            <div class="pinfo">
              <div class="nick">{{ me.nickname }}</div>
              <div class="email">{{ me.email }}</div>
              <div class="allergy" v-if="me.allergies?.length">
                <span class="lbl">알레르기</span>
                <span v-for="a in me.allergies" :key="a" class="chip">{{ a }}</span>
              </div>
            </div>
          </div>
          <button class="edit" @click="openEdit">정보 수정</button>
        </div>
        <ul class="stats">
          <li class="clk" @click="router.push('/fridge')"><span>{{ me.stats.fridgeCount }}</span>냉장고</li>
          <li class="clk" @click="router.push('/mypage')"><span>{{ me.stats.wishlistCount }}</span>찜</li>
          <li><span>{{ me.stats.reviewCount }}</span>리뷰</li>
          <li class="clk" @click="router.push({ name: 'follow-list', query: { tab: 'followers' } })"><span>{{ me.stats.followerCount }}</span>팔로워</li>
          <li class="clk" @click="router.push({ name: 'follow-list', query: { tab: 'following' } })"><span>{{ me.stats.followingCount }}</span>팔로잉</li>
        </ul>
      </div>

      <!-- 2단: 찜한 레시피 | 내 리뷰 -->
      <div class="cols">
        <div class="panel">
          <h3 class="sec">찜한 레시피 <span class="cnt">{{ wishes.length }}</span></h3>
          <p v-if="wishes.length === 0" class="muted">아직 찜한 레시피가 없어요.</p>
          <ul v-else class="list">
            <li v-for="w in wishes" :key="w.wishlistId" class="row" @click="openWish(w)">
              <span class="type" :class="w.type === 'AI' ? 'ai' : 'db'"><template v-if="w.type === 'AI'"><img :src="botUrl" class="bi" alt="" /> AI</template><template v-else>📖</template></span>
              <div class="info">
                <div class="title">{{ w.title }}</div>
                <div class="sub"><span v-if="w.cookTime"><InlineIcon :svg="clockSvg" :size="12" /> {{ w.cookTime }}분</span></div>
              </div>
              <button class="rm" @click.stop="removeWish(w)">♥</button>
            </li>
          </ul>
        </div>

        <div class="panel">
          <h3 class="sec">내 리뷰 <span class="cnt">{{ reviews.length }}</span></h3>
          <p v-if="reviews.length === 0" class="muted">작성한 리뷰가 없어요.</p>
          <ul v-else class="rlist">
            <li v-for="r in reviews" :key="r.reviewId" class="rcard" @click="router.push({ name: 'recipe-detail', params: { recipeId: r.recipeId } })">
              <div class="rtop">
                <span class="rtitle">{{ r.recipeTitle }}</span>
                <span class="date">{{ fmtDate(r.createdAt) }}</span>
              </div>
              <div class="rstars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</div>
              <p class="rcontent">{{ r.content }}</p>
            </li>
          </ul>
        </div>
      </div>

      <!-- 마이 레시피 (전체폭) -->
      <div class="panel mt">
        <div class="myhead">
          <h3 class="sec">마이 레시피</h3>
          <span class="sec-desc">AI로 만들어 담은 내 레시피 · 공개하면 모두가 검색·찜할 수 있어요</span>
        </div>
        <MyRecipeList />
      </div>
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
.profile { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; padding: 20px; margin-bottom: 16px; }
.prow { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.pleft { display: flex; gap: 14px; align-items: center; min-width: 0; }
.avatar-wrap { position: relative; flex: 0 0 auto; width: 56px; height: 56px; padding: 0; border: none; background: none; cursor: pointer; border-radius: 50%; }
.avatar { width: 56px; height: 56px; border-radius: 50%; background: var(--primary-tint); color: var(--primary-deep); font-size: 22px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.avatar-img { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; display: block; }
.cam { position: absolute; right: -2px; bottom: -2px; width: 22px; height: 22px; border-radius: 50%; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); font-size: 11px; display: flex; align-items: center; justify-content: center; }
.pinfo { min-width: 0; }
.nick { font-size: 18px; font-weight: 700; }
.email { font-size: 13px; color: #999; margin-top: 2px; }
.edit { flex: 0 0 auto; border: 1px solid var(--line); background: #fff; border-radius: 8px; padding: 8px 14px; font-size: 13px; cursor: pointer; }
.allergy { margin-top: 8px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.allergy .lbl { font-size: 12px; color: #999; }
.chip { font-size: 12px; background: #fef2f2; color: #ef4444; border-radius: 999px; padding: 3px 10px; }
.stats { list-style: none; display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; padding: 0; margin: 18px 0 0; }
.stats li { background: #f5f7f9; border-radius: 12px; padding: 16px 4px; text-align: center; font-size: 12px; color: #666; }
.stats li span { display: block; font-size: 22px; font-weight: 800; color: var(--primary-deep); margin-bottom: 2px; }
.stats li.clk { cursor: pointer; }

/* 2단 + 섹션 패널 */
.cols { display: grid; grid-template-columns: 1fr 380px; gap: 16px; align-items: start; margin-bottom: 16px; }
.panel { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; padding: 18px 20px; }
.myhead { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; flex-wrap: wrap; }

.sec { font-size: 16px; margin: 0 0 12px; }
.sec .cnt { color: var(--primary-deep); font-size: 14px; }
.sec-desc { font-size: 12px; color: #999; }

.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; }
.row { display: flex; align-items: center; gap: 12px; padding: 12px 10px; border-radius: 10px; cursor: pointer; }
.row + .row { border-top: 1px solid var(--line); }
.row:hover { background: #fafbfc; }
.type { display: inline-flex; align-items: center; gap: 3px; font-size: 13px; flex: 0 0 auto; }
.bi { width: 14px; height: 14px; object-fit: contain; vertical-align: -2px; }
.type.ai { color: #7c3aed; font-weight: 700; }
.info { flex: 1; min-width: 0; }
.title { font-size: 15px; font-weight: 600; }
.sub { font-size: 12px; color: #999; margin-top: 2px; }
.rm { border: none; background: none; color: #ef4444; font-size: 20px; cursor: pointer; }

.rlist { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.rcard { border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 12px; padding: 12px; cursor: pointer; }
.rtop { display: flex; justify-content: space-between; align-items: center; }
.rtitle { font-size: 14px; font-weight: 600; }
.date { font-size: 11px; color: #aaa; }
.rstars { color: #f59e0b; font-size: 13px; margin: 4px 0; }
.rcontent { font-size: 14px; margin: 0; color: #555; }

.muted { color: #999; }

@media (max-width: 880px) {
  .cols { grid-template-columns: 1fr; }
  .stats { grid-template-columns: repeat(3, 1fr); }
}

.overlay { position: fixed; inset: 0; background: rgba(17,24,39,.5); display: flex; align-items: center; justify-content: center; padding: 20px; box-sizing: border-box; z-index: 60; }
.sheet { width: 100%; max-width: 480px; background: #fff; border-radius: 16px; padding: 22px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 48px rgba(0,0,0,.18); }
.shd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.shd h3 { margin: 0; font-size: 17px; }
.x { border: none; background: none; font-size: 18px; cursor: pointer; color: #888; }
.sheet label { display: block; font-size: 13px; color: #555; margin: 12px 0 4px; }
.sheet input { width: 100%; padding: 11px 12px; border: 1px solid var(--line); border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.pw { margin-top: 6px; }
.pw input { margin-bottom: 8px; }
.err { color: #e11d48; font-size: 13px; margin: 10px 0 0; }
.save { width: 100%; margin-top: 16px; padding: 13px; border: none; border-radius: 8px; background: var(--primary); color: var(--on-primary); font-weight: 700; cursor: pointer; }
.save:disabled { opacity: .6; }
</style>