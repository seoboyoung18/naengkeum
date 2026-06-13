<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { fetchDashboard } from '../api/fridge'
import { myChallenges } from '../api/challenge'
import LottieIcon from '../components/LottieIcon.vue'
import winkAnim from '../assets/wink.json'
import botAnim from '../assets/bot.json'
import hourglassAnim from '../assets/hourglass.json'

const router = useRouter()
const auth = useAuthStore()

const me = ref(null)
const summary = ref({ fridgeCount: 0, freezerCount: 0, roomTempCount: 0 })
const expiring = ref([])
const activeChallenge = ref(null)
const keyword = ref('')
const loading = ref(true)
const err = ref('')

const totalFridge = computed(() =>
  summary.value.fridgeCount + summary.value.freezerCount + summary.value.roomTempCount)

async function load() {
  loading.value = true
  err.value = ''
  try {
    const [m, dash, chs] = await Promise.all([
      auth.fetchMe(),
      fetchDashboard(),
      myChallenges().catch(() => []),
    ])
    me.value = m
    summary.value = dash.summary
    expiring.value = dash.expiringItems
    const joined = (chs || []).filter((c) => c.myStatus === 'JOINED')
    activeChallenge.value = joined.find((c) => c.myProgress < 100) || joined[0] || null
  } catch (e) {
    err.value = e.response?.data?.message || '대시보드를 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

function dDayText(d) {
  if (d < 0) return `${Math.abs(d)}일 지남`
  if (d === 0) return '오늘까지'
  return `${d}일 남음`
}
function dDayClass(d) { return d < 0 ? 'expired' : 'soon' }
function search() {
  const kw = keyword.value.trim()
  router.push({ name: 'recipe', query: kw ? { keyword: kw } : {} })
}

const STORAGE = { FRIDGE: '냉장', FREEZER: '냉동', ROOM_TEMP: '실온' }

onMounted(load)
</script>

<template>
  <section>
    <!-- 상단: 인사 + 검색 -->
    <div class="top">
      <p class="hi" v-if="me">안녕하세요, <b>{{ me.nickname }}</b>님 <LottieIcon :data="winkAnim" :size="28" /></p>
      <p class="hi" v-else>&nbsp;</p>
      <div class="search">
        <span class="ic">🔍</span>
        <input v-model="keyword" placeholder="레시피·재료 검색" @keyup.enter="search" />
      </div>
    </div>

    <!-- AI 추천 배너 -->
    <RouterLink to="/ai-recommend" class="ai-banner">
      <div class="ai-txt">
        <strong><LottieIcon :data="botAnim" :size="30" :zoom="1.5" style="margin-right: 12px; transform: translateY(-2px);" />냉장고 재료로 AI 추천 받기</strong>
        <span>남은 재료로 만들 수 있는 레시피를 실시간으로 추천해 드려요</span>
      </div>
      <span class="ai-btn">추천 받기</span>
    </RouterLink>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="err" class="err">{{ err }}</p>

    <template v-else>
      <!-- 4-스탯 -->
      <ul class="stats">
        <li @click="router.push('/fridge')"><span>{{ totalFridge }}</span>냉장고 재료</li>
        <li @click="router.push('/fridge')"><span :class="{ warn: expiring.length }">{{ expiring.length }}</span>임박</li>
        <li @click="router.push('/recipe')"><span>{{ me?.stats?.wishlistCount ?? 0 }}</span>찜한 레시피</li>
        <li @click="router.push('/mypage')"><span>{{ me?.stats?.reviewCount ?? 0 }}</span>내 리뷰</li>
      </ul>

      <!-- 2단 그리드 -->
      <div class="grid">
        <!-- 좌: 유통기한 임박 -->
        <div class="card imminent" :class="{ none: expiring.length === 0 }" @click="router.push('/fridge')">
          <div class="chead">
            <span class="ititle"><LottieIcon :data="hourglassAnim" :size="24" :zoom="2.2" style="margin-right: 4px;" />유통기한 임박</span>
            <span class="ibadge">{{ expiring.length }}</span>
          </div>
          <p v-if="expiring.length === 0" class="iok">임박한 재료가 없어요. 잘 관리하고 있어요! 👍</p>
          <ul v-else class="ilist">
            <li v-for="it in expiring.slice(0, 6)" :key="it.fridgeItemId">
              <span class="iname">{{ it.name }} <span class="iqty">{{ it.qty }}{{ it.unit }}</span></span>
              <span class="idday" :class="dDayClass(it.dDay)">{{ dDayText(it.dDay) }}</span>
            </li>
          </ul>
          <p v-if="expiring.length > 6" class="imore">+{{ expiring.length - 6 }}개 더 · 냉장고에서 보기 ›</p>
        </div>

        <!-- 우: 내 냉장고 + 진행 중 챌린지 -->
        <div class="side">
          <div class="card" @click="router.push('/fridge')">
            <div class="chead"><span>🧊 내 냉장고</span><span class="total">{{ totalFridge }}개</span></div>
            <ul class="storage">
              <li><span>{{ summary.fridgeCount }}</span>{{ STORAGE.FRIDGE }}</li>
              <li><span>{{ summary.freezerCount }}</span>{{ STORAGE.FREEZER }}</li>
              <li><span>{{ summary.roomTempCount }}</span>{{ STORAGE.ROOM_TEMP }}</li>
            </ul>
          </div>

          <div class="card chal" @click="router.push('/challenge')">
            <div class="chead"><span>🏆 진행 중 챌린지</span></div>
            <template v-if="activeChallenge">
              <div class="ctitle">{{ activeChallenge.title }}</div>
              <div class="cbar"><div class="cfill" :style="{ width: activeChallenge.myProgress + '%' }"></div></div>
              <div class="cpct">{{ activeChallenge.myProgress }}% 달성</div>
            </template>
            <p v-else class="muted sm">참여 중인 챌린지가 없어요. 도전해 보세요!</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.top { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.hi { font-size: 20px; margin: 0; }
.search { position: relative; width: 320px; max-width: 40%; }
.search .ic { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 13px; }
.search input { width: 100%; box-sizing: border-box; padding: 10px 12px 10px 34px; border: 1px solid var(--line);
  border-radius: 10px; font-size: 14px; background: #fff; }

.ai-banner { display: flex; align-items: center; justify-content: space-between; gap: 12px; text-decoration: none;
  background: linear-gradient(135deg, var(--primary-deep), var(--primary-deep)); color: #fff; border-radius: 16px; padding: 22px 26px; margin-bottom: 18px; }
.ai-banner .ai-txt { display: flex; flex-direction: column; gap: 4px; }
.ai-banner strong { font-size: 17px; }
.ai-banner .ai-txt span { font-size: 13px; opacity: .9; }
.ai-btn { flex: 0 0 auto; background: #fff; color: var(--primary-deep); font-weight: 700; font-size: 14px;
  border-radius: 10px; padding: 10px 18px; }

/* 4-스탯 */
.stats { list-style: none; display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; padding: 0; margin: 0 0 16px; }
.stats li { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; padding: 18px; cursor: pointer; font-size: 13px; color: #777; }
.stats li span { display: block; font-size: 26px; font-weight: 800; color: var(--primary-deep); margin-bottom: 4px; }
.stats li span.warn { color: #f59e0b; }

/* 2단 그리드 */
.grid { display: grid; grid-template-columns: 1fr 360px; gap: 16px; align-items: start; }
.side { display: flex; flex-direction: column; gap: 16px; }
.card { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; padding: 18px 20px; cursor: pointer; }
.chead { display: flex; align-items: center; justify-content: space-between; font-size: 15px; font-weight: 700; }
.chead .total { color: var(--primary-deep); font-weight: 700; }

.imminent { border-color: #fed7aa; }
.imminent.none { border-color: #bbf7d0; }
.ititle { color: #c2410c; }
.imminent.none .ititle { color: var(--primary-deep); }
.ibadge { background: #f97316; color: #fff; font-size: 12px; font-weight: 700; border-radius: 999px; min-width: 22px; height: 22px;
  display: inline-flex; align-items: center; justify-content: center; padding: 0 7px; }
.imminent.none .ibadge { background: var(--primary-deep); }
.iok { font-size: 14px; color: var(--primary-deep); margin: 14px 0 0; }
.ilist { list-style: none; padding: 0; margin: 14px 0 0; }
.ilist li { display: flex; justify-content: space-between; align-items: center; padding: 11px 0; border-top: 1px solid var(--line); font-size: 14px; }
.iname { color: #333; }
.iqty { color: #aaa; font-size: 12px; }
.idday { font-size: 13px; font-weight: 700; }
.idday.soon { color: #f59e0b; }
.idday.expired { color: #ef4444; }
.imore { font-size: 12px; color: #c2410c; margin: 10px 0 0; text-align: right; }

.storage { list-style: none; display: flex; gap: 10px; padding: 0; margin: 14px 0 0; }
.storage li { flex: 1; background: #f5f7f9; border-radius: 10px; padding: 14px 8px; text-align: center; font-size: 12px; color: #666; }
.storage li span { display: block; font-size: 22px; font-weight: 800; color: var(--primary-deep); margin-bottom: 2px; }

.chal .ctitle { font-size: 14px; font-weight: 600; margin-top: 14px; }
.cbar { height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; margin: 10px 0 6px; }
.cfill { height: 100%; background: var(--primary-deep); }
.cpct { font-size: 12px; color: var(--primary-deep); font-weight: 700; }

.muted { color: #999; }
.muted.sm { font-size: 13px; margin: 14px 0 0; }
.err { color: #e11d48; }

@media (max-width: 880px) {
  .grid { grid-template-columns: 1fr; }
  .stats { grid-template-columns: repeat(2, 1fr); }
  .search { max-width: none; }
}
</style>
