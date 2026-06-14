<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { searchRecipes, autocompleteRecipes } from '../api/recipe'
import { API_BASE } from '../api/http'
import { addRecipeWish, removeRecipeWish } from '../api/wishlist'
import { listFridge } from '../api/fridge'
import { useToast } from '../composables/useToast'
import InlineIcon from '../components/InlineIcon.vue'
import clockSvg from '../assets/icons/clock-outline.svg?raw'
import starSvg from '../assets/icons/star.svg?raw'
import botUrl from '../assets/icons/message-bot.svg'

const toast = useToast()

// 업로드 이미지(/images/...)는 백엔드 호스트를 붙여 절대경로로. 외부 URL은 그대로.
function imageUrl(u) {
  if (!u) return null
  return u.startsWith('/') ? API_BASE + u : u
}

const router = useRouter()
const route = useRoute()

const SORTS = [
  { key: 'LATEST', label: '최신' },
  { key: 'POPULAR', label: '인기' },
  { key: 'RATING', label: '평점' },
  { key: 'COOK_TIME', label: '조리시간' },
]
// 구간(band) — 서로 겹치지 않게: 10분=≤10, 20분=11~20, 30분=21~30
const COOK_TIMES = [
  { min: null, max: null, label: '시간 전체' },
  { min: null, max: 10, label: '10분' },
  { min: 11, max: 20, label: '20분' },
  { min: 21, max: 30, label: '30분' },
]
const SIZE = 12

const filters = reactive({ keyword: '', sort: 'LATEST', minCookTime: null, maxCookTime: null, useMyFridge: false, mine: false })

// 출처 배지 (공공/✍️직접/🤖AI)
const SOURCE_BADGE = {
  PUBLIC: { label: '공공', cls: 'pub' },
  USER: { label: '✍️ 직접', cls: 'user' },
  AI_SAVED: { label: 'AI', cls: 'ai' },
}
function badge(source) { return SOURCE_BADGE[source] || SOURCE_BADGE.PUBLIC }
const content = ref([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
let fridgeNames = ''

// 자동완성
const suggestions = ref([])
const showSuggest = ref(false)
let debounceTimer = null

function onKeywordInput() {
  clearTimeout(debounceTimer)
  const kw = filters.keyword.trim()
  if (kw.length < 2) { suggestions.value = []; showSuggest.value = false; return }
  debounceTimer = setTimeout(async () => {
    try {
      suggestions.value = await autocompleteRecipes(kw)
      showSuggest.value = suggestions.value.length > 0
    } catch (_) {}
  }, 200)
}
function pickSuggest(s) {
  filters.keyword = s.title
  showSuggest.value = false
  load(true)
}

async function ensureFridgeNames() {
  if (fridgeNames) return fridgeNames
  try {
    const d = await listFridge({ storageType: 'ALL', sort: 'EXPIRY_ASC' })
    fridgeNames = d.items.map((i) => i.name).join(',')
  } catch (_) { fridgeNames = '' }
  return fridgeNames
}

async function load(reset = true) {
  if (reset) { page.value = 0; loading.value = true } else { loadingMore.value = true }
  error.value = ''
  try {
    const params = {
      sort: filters.sort,
      page: page.value,
      size: SIZE,
    }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.minCookTime) params.minCookTime = filters.minCookTime
    if (filters.maxCookTime) params.maxCookTime = filters.maxCookTime
    if (filters.mine) params.mine = true
    if (filters.useMyFridge) {
      const names = await ensureFridgeNames()
      if (names) params.ingredients = names
    }
    const data = await searchRecipes(params)
    content.value = reset ? data.content : [...content.value, ...data.content]
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch (e) {
    error.value = e.response?.data?.message || '레시피를 불러오지 못했습니다'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function setTab(mine) { if (filters.mine === mine) return; filters.mine = mine; load(true) }
function setSort(k) { filters.sort = k; load(true) }
function setCookTime(c) { filters.minCookTime = c.min; filters.maxCookTime = c.max; load(true) }
function toggleMyFridge() { filters.useMyFridge = !filters.useMyFridge; load(true) }
function loadMore() { page.value += 1; load(false) }

function goDetail(id) { router.push({ name: 'recipe-detail', params: { recipeId: id } }) }

async function toggleWish(item) {
  const prev = item.isWishlisted
  item.isWishlisted = !prev // 낙관적
  try {
    if (prev) await removeRecipeWish(item.recipeId)
    else await addRecipeWish(item.recipeId)
  } catch (e) {
    item.isWishlisted = prev // 롤백
    if (e.response?.status !== 409) toast.error(e.response?.data?.message || '찜 처리에 실패했어요')
  }
}

onMounted(() => {
  if (route.query.keyword) filters.keyword = String(route.query.keyword)
  load(true)
})
onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <section>
    <div class="head">
      <h2 class="h">레시피 탐색</h2>
      <button class="register" @click="router.push({ name: 'recipe-publish' })">＋ 레시피 등록</button>
    </div>

    <!-- 탭: 전체 / 내가 등록한 -->
    <div class="tabs">
      <button :class="{ on: !filters.mine }" @click="setTab(false)">전체</button>
      <button :class="{ on: filters.mine }" @click="setTab(true)">내가 등록한 레시피</button>
    </div>

    <!-- 검색 -->
    <div class="search">
      <input
        v-model="filters.keyword"
        type="text"
        placeholder="레시피명 또는 재료로 검색"
        @input="onKeywordInput"
        @keyup.enter="load(true); showSuggest = false"
        @blur="() => setTimeout(() => (showSuggest = false), 150)"
      />
      <button class="go" @click="load(true)">검색</button>
      <ul v-if="showSuggest" class="ac-list">
        <li v-for="s in suggestions" :key="s.recipeId" @mousedown.prevent="pickSuggest(s)">{{ s.title }}</li>
      </ul>
    </div>

    <!-- 정렬 -->
    <div class="chips">
      <button v-for="s in SORTS" :key="s.key" :class="{ on: filters.sort === s.key }" @click="setSort(s.key)">{{ s.label }}</button>
    </div>

    <!-- 조리시간 + 내 재료 -->
    <div class="chips">
      <button v-for="c in COOK_TIMES" :key="c.label" :class="{ on: filters.minCookTime === c.min && filters.maxCookTime === c.max }" @click="setCookTime(c)">{{ c.label }}</button>
      <button class="mine" :class="{ on: filters.useMyFridge }" @click="toggleMyFridge">내 재료</button>
    </div>

    <p class="count" v-if="!loading">{{ totalElements }}개</p>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="error" class="err">{{ error }}</p>
    <p v-else-if="content.length === 0" class="muted empty">검색 결과가 없습니다.</p>

    <ul v-else class="grid">
      <li v-for="r in content" :key="r.recipeId" class="card" @click="goDetail(r.recipeId)">
        <div class="thumb" :style="imageUrl(r.thumbnailUrl) ? { backgroundImage: `url(${imageUrl(r.thumbnailUrl)})` } : null">
          <span v-if="!r.thumbnailUrl">🍽️</span>
          <button class="heart" :class="{ on: r.isWishlisted }" @click.stop="toggleWish(r)">
            {{ r.isWishlisted ? '♥' : '♡' }}
          </button>
        </div>
        <div class="info">
          <div class="trow">
            <div class="title">{{ r.title }}</div>
            <span class="src" :class="badge(r.source).cls"><img v-if="r.source === 'AI_SAVED'" :src="botUrl" class="bi" alt="" />{{ badge(r.source).label }}</span>
          </div>
          <div class="meta">
            <span v-if="r.cookTime"><InlineIcon :svg="clockSvg" :size="12" /> {{ r.cookTime }}분</span>
            <span><InlineIcon :svg="starSvg" :size="13" style="transform: translateY(-2px)" /> {{ Number(r.avgRating).toFixed(1) }}<template v-if="r.reviewCount"> ({{ r.reviewCount }})</template></span>
          </div>
          <div class="ings" v-if="r.mainIngredients?.length">{{ r.mainIngredients.join(' · ') }}</div>
        </div>
      </li>
    </ul>

    <button v-if="!loading && page + 1 < totalPages" class="more" :disabled="loadingMore" @click="loadMore">
      {{ loadingMore ? '불러오는 중…' : '더 보기' }}
    </button>
  </section>
</template>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.register { flex: 0 0 auto; border: none; background: var(--primary); color: var(--on-primary); border-radius: 8px;
  padding: 10px 16px; font-size: 14px; font-weight: 700; cursor: pointer; }

.tabs { display: flex; gap: 8px; margin-bottom: 12px; }
.tabs button { padding: 8px 16px; border: 1px solid var(--line); background: #fff; border-radius: 999px;
  font-size: 14px; color: #666; cursor: pointer; }
.tabs button.on { border-color: var(--primary-deep); background: var(--primary-tint); color: var(--primary-deep); font-weight: 700; }

.search { position: relative; display: flex; gap: 8px; margin-bottom: 12px; }
.search input { flex: 1; padding: 11px 12px; border: 1px solid var(--line); border-radius: 8px; font-size: 14px; }
.go { border: none; background: var(--primary); color: var(--on-primary); border-radius: 8px; padding: 0 16px; font-size: 14px; font-weight: 700; cursor: pointer; }
.ac-list { position: absolute; top: 46px; left: 0; right: 70px; background: #fff; border: 1px solid var(--line); border-radius: 8px; list-style: none; margin: 0; padding: 4px 0; z-index: 5; box-shadow: 0 6px 18px rgba(0,0,0,.08); max-height: 220px; overflow-y: auto; }
.ac-list li { padding: 9px 12px; font-size: 14px; cursor: pointer; }
.ac-list li:hover { background: #f5f7f9; }

.chips { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
.chips button { padding: 6px 12px; border: 1px solid var(--line); background: #fff; border-radius: 999px; font-size: 13px; color: #666; cursor: pointer; }
.chips button.on { border-color: var(--primary-deep); background: var(--primary-tint); color: var(--primary-deep); font-weight: 700; }
.chips .mine.on { border-color: #2563eb; background: #eff6ff; color: #2563eb; }

.count { font-size: 12px; color: #999; margin: 0 0 10px; }

.grid { list-style: none; padding: 0; margin: 0; display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.card { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; overflow: hidden; cursor: pointer; transition: box-shadow .15s; }
.card:hover { box-shadow: 0 6px 18px rgba(0,0,0,.06); }
.thumb { position: relative; height: 150px; background: #f1f3f5 center/cover no-repeat; display: flex; align-items: center; justify-content: center; font-size: 34px; color: #c7ccd1; }
.heart { position: absolute; top: 10px; right: 10px; border: none; background: rgba(255,255,255,.9); width: 32px; height: 32px; border-radius: 50%; font-size: 17px; color: #bbb; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.heart.on { color: #ef4444; }
.info { padding: 14px 16px 16px; }
.trow { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.title { font-size: 15px; font-weight: 700; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.src { flex: 0 0 auto; font-size: 11px; font-weight: 700; border-radius: 999px; padding: 3px 8px; background: #eef2f7; color: #64748b; }
.src.user { background: #fff7ed; color: #c2410c; }
.src.ai { background: #f5f3ff; color: #7c3aed; }
.bi { width: 14px; height: 14px; object-fit: contain; vertical-align: -2px; }
.meta { display: flex; gap: 10px; font-size: 12px; color: #888; margin-top: 8px; }
.ings { font-size: 12px; color: #aaa; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (max-width: 900px) { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .grid { grid-template-columns: 1fr; } }

.muted { color: #999; }
.empty { text-align: center; padding: 40px 0; }
.err { color: #e11d48; }
.more { width: 100%; margin-top: 14px; padding: 12px; border: 1px solid var(--line); background: #fff; border-radius: 8px; font-size: 14px; cursor: pointer; }
</style>
