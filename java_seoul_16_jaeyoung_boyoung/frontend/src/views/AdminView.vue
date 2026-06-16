<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '../composables/useToast'
import InlineIcon from '../components/InlineIcon.vue'
import searchSvg from '../assets/icons/search.svg?raw'
import trashSvg from '../assets/icons/trash.svg?raw'
import shieldSvg from '../assets/icons/shield-lock.svg?raw'
import starSvg from '../assets/icons/star.svg?raw'
import {
  getStats, getUsers, setUserActive,
  getRecipes, deleteRecipe,
  getReviews, deleteReview,
  getReports, resolveRecipeReports, resolveReviewReports,
} from '../api/admin'

const router = useRouter()
const toast = useToast()

const tab = ref('dash')
const stats = ref(null)
const users = ref([])
const recipes = ref([])
const reviews = ref([])
const reports = ref([])
const loaded = reactive({ users: false, recipes: false, reviews: false, reports: false })
const loading = ref(false)
const userKeyword = ref('')
const recipeKeyword = ref('')

const pendingReports = computed(() => stats.value?.pendingReports ?? 0)

const TABS = computed(() => [
  { key: 'dash', label: '대시보드' },
  { key: 'users', label: '회원' },
  { key: 'recipes', label: '레시피' },
  { key: 'reviews', label: '리뷰' },
  { key: 'reports', label: '신고', badge: pendingReports.value },
])

async function loadStats() {
  try { stats.value = await getStats() } catch (e) { toast.error('통계를 불러오지 못했어요') }
}
async function loadUsers() {
  loading.value = true
  try { users.value = await getUsers(userKeyword.value.trim()); loaded.users = true }
  catch (e) { toast.error('회원 목록을 불러오지 못했어요') }
  finally { loading.value = false }
}
async function loadRecipes() {
  loading.value = true
  try { recipes.value = await getRecipes(recipeKeyword.value.trim()); loaded.recipes = true }
  catch (e) { toast.error('레시피 목록을 불러오지 못했어요') }
  finally { loading.value = false }
}
async function loadReviews() {
  loading.value = true
  try { reviews.value = await getReviews(); loaded.reviews = true }
  catch (e) { toast.error('리뷰 목록을 불러오지 못했어요') }
  finally { loading.value = false }
}
async function loadReports() {
  loading.value = true
  try { reports.value = await getReports(); loaded.reports = true }
  catch (e) { toast.error('신고 목록을 불러오지 못했어요') }
  finally { loading.value = false }
}

function setTab(key) {
  tab.value = key
  if (key === 'users' && !loaded.users) loadUsers()
  if (key === 'recipes' && !loaded.recipes) loadRecipes()
  if (key === 'reviews' && !loaded.reviews) loadReviews()
  if (key === 'reports' && !loaded.reports) loadReports()
}

// 검색 디바운스 (서버 검색)
let userTimer = null
watch(userKeyword, () => { clearTimeout(userTimer); userTimer = setTimeout(loadUsers, 250) })
let recipeTimer = null
watch(recipeKeyword, () => { clearTimeout(recipeTimer); recipeTimer = setTimeout(loadRecipes, 250) })

// ---- 액션 ----
async function toggleActive(u) {
  if (u.role === 'ADMIN') return // 관리자 차단 불가
  const next = !u.active
  try {
    await setUserActive(u.memberId, next)
    u.active = next
    toast.success(next ? '차단을 해제했어요' : '회원을 차단했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '처리에 실패했어요')
  }
}

async function removeRecipe(r) {
  if (!confirm(`'${r.title}' 레시피를 삭제할까요? 재료·단계·리뷰·신고가 함께 삭제됩니다.`)) return
  try {
    await deleteRecipe(r.recipeId)
    recipes.value = recipes.value.filter((x) => x.recipeId !== r.recipeId)
    toast.success('레시피를 삭제했어요')
    refreshAfterModeration()
  } catch (e) {
    toast.error(e.response?.data?.message || '삭제에 실패했어요')
  }
}

async function removeReview(rv) {
  if (!confirm('이 리뷰를 삭제할까요?')) return
  try {
    await deleteReview(rv.reviewId)
    reviews.value = reviews.value.filter((x) => x.reviewId !== rv.reviewId)
    toast.success('리뷰를 삭제했어요')
    refreshAfterModeration()
  } catch (e) {
    toast.error(e.response?.data?.message || '삭제에 실패했어요')
  }
}

async function removeFromReport(rep) {
  const isRecipe = rep.targetType === 'RECIPE'
  if (!confirm(isRecipe ? '신고된 레시피를 삭제할까요?' : '신고된 리뷰를 삭제할까요?')) return
  try {
    if (isRecipe) await deleteRecipe(rep.recipeId)
    else await deleteReview(rep.reviewId)
    reports.value = reports.value.filter((x) => x !== rep)
    toast.success('삭제했어요')
    invalidateModerated()
  } catch (e) {
    toast.error(e.response?.data?.message || '삭제에 실패했어요')
  }
}

async function ignoreReport(rep) {
  try {
    if (rep.targetType === 'RECIPE') await resolveRecipeReports(rep.recipeId)
    else await resolveReviewReports(rep.reviewId)
    reports.value = reports.value.filter((x) => x !== rep)
    toast.success('신고를 처리했어요')
    invalidateModerated()
  } catch (e) {
    toast.error(e.response?.data?.message || '처리에 실패했어요')
  }
}

// 신고/삭제 후 통계·관련 탭 갱신
function refreshAfterModeration() {
  loadStats()
  loaded.reports = false
}
function invalidateModerated() {
  loadStats()
  loaded.recipes = false
  loaded.reviews = false
}

function goRecipe(recipeId) {
  if (recipeId) router.push({ name: 'recipe-detail', params: { recipeId } })
}

onMounted(loadStats)
</script>

<template>
  <section>
    <div class="head">
      <span class="hicon" v-html="shieldSvg"></span>
      <h2 class="h">관리자 대시보드</h2>
    </div>
    <p class="sub">서비스 현황을 확인하고 회원·레시피·리뷰·신고를 관리하세요.</p>

    <!-- 탭 -->
    <div class="tabs" role="tablist">
      <button
        v-for="t in TABS"
        :key="t.key"
        class="tab"
        :class="{ on: tab === t.key }"
        @click="setTab(t.key)"
      >
        {{ t.label }}
        <span v-if="t.badge" class="tbadge">{{ t.badge }}</span>
      </button>
    </div>

    <!-- 대시보드 -->
    <section v-show="tab === 'dash'">
      <div class="cards">
        <div class="stat"><p class="lbl">전체 회원</p><p class="val">{{ stats?.totalMembers ?? '–' }}</p></div>
        <div class="stat"><p class="lbl">레시피</p><p class="val">{{ stats?.totalRecipes ?? '–' }}</p></div>
        <div class="stat"><p class="lbl">리뷰</p><p class="val">{{ stats?.totalReviews ?? '–' }}</p></div>
        <div class="stat"><p class="lbl">챌린지 참여</p><p class="val">{{ stats?.activeChallengeParticipants ?? '–' }}</p></div>
        <div class="stat danger"><p class="lbl">미처리 신고</p><p class="val">{{ stats?.pendingReports ?? '–' }}</p></div>
      </div>
      <div class="note">
        <p class="note-t">최근 활동 요약</p>
        <p class="note-d">신고가 누적된 콘텐츠는 신고 탭에서 확인하고 처리하세요. 각 탭에서 회원·레시피·리뷰를 직접 관리할 수 있어요.</p>
      </div>
    </section>

    <!-- 회원 -->
    <section v-show="tab === 'users'">
      <div class="search">
        <span class="sic" v-html="searchSvg"></span>
        <input v-model="userKeyword" placeholder="닉네임 또는 이메일 검색" />
      </div>
      <div class="tablewrap">
        <table>
          <thead><tr><th>닉네임</th><th>이메일</th><th>역할</th><th class="r">상태</th></tr></thead>
          <tbody>
            <tr v-for="u in users" :key="u.memberId">
              <td class="strong">{{ u.nickname }}</td>
              <td class="muted">{{ u.email }}</td>
              <td><span class="role" :class="u.role === 'ADMIN' ? 'admin' : 'user'">{{ u.role }}</span></td>
              <td class="r">
                <button
                  class="btn"
                  :class="u.active ? 'warn' : 'ok'"
                  :disabled="u.role === 'ADMIN'"
                  @click="toggleActive(u)"
                >{{ u.role === 'ADMIN' ? '차단 불가' : (u.active ? '차단' : '해제') }}</button>
              </td>
            </tr>
            <tr v-if="loaded.users && users.length === 0"><td colspan="4" class="empty">회원이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 레시피 -->
    <section v-show="tab === 'recipes'">
      <div class="search">
        <span class="sic" v-html="searchSvg"></span>
        <input v-model="recipeKeyword" placeholder="레시피 제목 검색" />
      </div>
      <p class="hint">사용자가 등록한 레시피만 표시 (공공 레시피 제외) · 신고 누적순.</p>
      <div class="tablewrap">
        <table>
          <thead><tr><th>제목</th><th>작성자</th><th>신고</th><th class="r">관리</th></tr></thead>
          <tbody>
            <tr v-for="r in recipes" :key="r.recipeId">
              <td class="strong link" @click="goRecipe(r.recipeId)">{{ r.title }}</td>
              <td class="muted">{{ r.authorNickname }}</td>
              <td><span class="rep" :class="{ on: r.reportCount > 0 }">{{ r.reportCount }}</span></td>
              <td class="r"><button class="btn del" @click="removeRecipe(r)"><InlineIcon :svg="trashSvg" :size="13" /> 삭제</button></td>
            </tr>
            <tr v-if="loaded.recipes && recipes.length === 0"><td colspan="4" class="empty">레시피가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 리뷰 -->
    <section v-show="tab === 'reviews'">
      <p class="hint">신고 누적순.</p>
      <div class="tablewrap">
        <table>
          <thead><tr><th>레시피</th><th>작성자</th><th>평점</th><th>내용</th><th>신고</th><th class="r">관리</th></tr></thead>
          <tbody>
            <tr v-for="rv in reviews" :key="rv.reviewId">
              <td class="strong link" @click="goRecipe(rv.recipeId)">{{ rv.recipeTitle }}</td>
              <td class="muted">{{ rv.authorNickname }}</td>
              <td class="rating"><InlineIcon :svg="starSvg" :size="12" style="transform: translateY(-1px)" /> {{ Number(rv.rating).toFixed(1) }}</td>
              <td class="muted clip">{{ rv.content }}</td>
              <td><span class="rep" :class="{ on: rv.reportCount > 0 }">{{ rv.reportCount }}</span></td>
              <td class="r"><button class="btn del" @click="removeReview(rv)"><InlineIcon :svg="trashSvg" :size="13" /> 삭제</button></td>
            </tr>
            <tr v-if="loaded.reviews && reviews.length === 0"><td colspan="6" class="empty">리뷰가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 신고 -->
    <section v-show="tab === 'reports'">
      <p class="hint">사용자가 신고한 레시피·리뷰. 제목 클릭 시 레시피 상세로 이동, 부적절하면 삭제하세요.</p>
      <div class="tablewrap">
        <table>
          <thead><tr><th>대상</th><th>제목/내용</th><th>사유</th><th>신고수</th><th class="r">관리</th></tr></thead>
          <tbody>
            <tr v-for="rep in reports" :key="rep.targetType + '-' + (rep.recipeId ?? '') + '-' + (rep.reviewId ?? '')">
              <td><span class="ttype" :class="rep.targetType === 'RECIPE' ? 'recipe' : 'review'">{{ rep.targetType === 'RECIPE' ? '레시피' : '리뷰' }}</span></td>
              <td class="link clip" @click="goRecipe(rep.recipeId)">{{ rep.title }}</td>
              <td class="muted">{{ rep.reasons || '–' }}</td>
              <td><span class="rep on">{{ rep.reportCount }}</span></td>
              <td class="r">
                <button class="btn del" @click="removeFromReport(rep)">삭제</button>
                <button class="btn ghost" @click="ignoreReport(rep)">무시</button>
              </td>
            </tr>
            <tr v-if="loaded.reports && reports.length === 0"><td colspan="5" class="empty">미처리 신고가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.head { display: flex; align-items: center; gap: 8px; }
.hicon { width: 22px; height: 22px; color: var(--mute); display: inline-flex; }
.hicon :deep(svg) { width: 22px; height: 22px; }
.h { margin: 0; font-size: 20px; }
.sub { margin: 4px 0 18px; font-size: 13px; color: #777; }

/* 탭 */
.tabs { display: flex; gap: 2px; border-bottom: 1px solid var(--line); margin-bottom: 18px; }
.tab { border: none; background: none; padding: 9px 16px; font-size: 14px; cursor: pointer; color: #888; border-bottom: 2px solid transparent; display: inline-flex; align-items: center; gap: 6px; }
.tab.on { color: var(--text, #23272e); border-bottom-color: var(--primary-deep); font-weight: 700; }
.tbadge { background: #e24b4a; color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 999px; font-family: var(--font-mono); }

/* 대시보드 카드 */
.cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 18px; }
.stat { background: #fff; border: 1px solid var(--line); border-radius: var(--r-md); padding: 16px; box-shadow: var(--shadow-card); }
.stat .lbl { margin: 0; font-size: 12px; color: #888; }
.stat .val { margin: 6px 0 0; font-size: 24px; font-weight: 700; font-family: var(--font-mono); color: #23272e; }
.stat.danger { background: #fcebeb; border-color: #f7c1c1; }
.stat.danger .lbl, .stat.danger .val { color: #a32d2d; }
.note { background: #fff; border: 1px solid var(--line); border-radius: var(--r-md); padding: 16px; box-shadow: var(--shadow-card); }
.note-t { margin: 0 0 4px; font-size: 14px; font-weight: 700; color: #23272e; }
.note-d { margin: 0; font-size: 13px; color: #666; line-height: 1.7; }

/* 검색 */
.search { position: relative; max-width: 300px; margin-bottom: 12px; }
.search .sic { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); width: 16px; height: 16px; color: #999; display: inline-flex; }
.search .sic :deep(svg) { width: 16px; height: 16px; }
.search input { width: 100%; box-sizing: border-box; padding: 9px 12px 9px 34px; font-size: 13px; border: 1px solid var(--line); border-radius: var(--r-sm); background: #fff; }
.hint { margin: 0 0 10px; font-size: 12px; color: #999; }

/* 테이블 */
.tablewrap { overflow-x: auto; border: 1px solid var(--line); border-radius: var(--r-md); box-shadow: var(--shadow-card); }
table { width: 100%; border-collapse: collapse; font-size: 13px; background: #fff; }
thead tr { text-align: left; color: #888; background: #fafaf8; }
th, td { padding: 11px 12px; white-space: nowrap; }
th.r, td.r { text-align: right; }
tbody tr { border-top: 1px solid #eeede8; }
.strong { color: #23272e; font-weight: 600; }
.muted { color: #6b6b6b; }
.clip { max-width: 280px; overflow: hidden; text-overflow: ellipsis; }
.link { color: #185fa5; cursor: pointer; }
.link:hover { text-decoration: underline; }
.rating { color: #ba7517; font-family: var(--font-mono); }
.empty { text-align: center; color: #999; padding: 28px 0; }

/* 역할 배지 */
.role { font-size: 11px; padding: 2px 8px; border-radius: 6px; font-family: var(--font-mono); }
.role.admin { background: #e1f5ee; color: #0f6e56; }
.role.user { background: #f1efe8; color: #5f5e5a; }

/* 신고수 */
.rep { font-size: 12px; color: #999; font-family: var(--font-mono); }
.rep.on { background: #fcebeb; color: #a32d2d; padding: 2px 8px; border-radius: 999px; font-weight: 700; }

/* 대상 타입 */
.ttype { font-size: 11px; padding: 2px 8px; border-radius: 6px; }
.ttype.recipe { background: #faeeda; color: #854f0b; }
.ttype.review { background: #e6f1fb; color: #0c447c; }

/* 버튼 */
.btn { font-size: 12px; padding: 5px 11px; border-radius: var(--r-sm); background: #fff; cursor: pointer; border: 1px solid var(--line); color: #555; }
.btn + .btn { margin-left: 4px; }
.btn.warn { color: #a32d2d; border-color: #f0995f; }
.btn.ok { color: #0f6e56; border-color: #9fe1cb; }
.btn.del { color: #a32d2d; border-color: #f09595; display: inline-flex; align-items: center; gap: 3px; }
.btn.ghost { color: #5f5e5a; }
.btn:disabled { opacity: .4; cursor: not-allowed; }

@media (max-width: 720px) {
  .cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
