<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { fetchDashboard } from '../api/fridge'

const router = useRouter()
const auth = useAuthStore()

const me = ref(null)
const summary = ref({ fridgeCount: 0, freezerCount: 0, roomTempCount: 0 })
const expiring = ref([])
const loading = ref(true)
const err = ref('')

const totalFridge = computed(() =>
  summary.value.fridgeCount + summary.value.freezerCount + summary.value.roomTempCount)

async function load() {
  loading.value = true
  err.value = ''
  try {
    const [m, dash] = await Promise.all([auth.fetchMe(), fetchDashboard()])
    me.value = m
    summary.value = dash.summary
    expiring.value = dash.expiringItems
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

const STORAGE = { FRIDGE: '냉장', FREEZER: '냉동', ROOM_TEMP: '실온' }

onMounted(load)
</script>

<template>
  <section>
    <p class="hi" v-if="me">안녕하세요, <b>{{ me.nickname }}</b>님 👋</p>

    <!-- AI 추천 배너 -->
    <RouterLink to="/ai-recommend" class="ai-banner">
      <div class="ai-txt">
        <strong>🤖 냉장고 재료로 AI 추천 받기</strong>
        <span>남은 재료로 만들 수 있는 레시피를 실시간으로</span>
      </div>
      <span class="arrow">›</span>
    </RouterLink>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="err" class="err">{{ err }}</p>

    <template v-else>
      <!-- D-3 임박 경고 -->
      <div class="imminent" :class="{ none: expiring.length === 0 }" @click="router.push('/fridge')">
        <div class="ihead">
          <span class="ititle">⏰ 유통기한 임박</span>
          <span class="ibadge">{{ expiring.length }}</span>
        </div>
        <p v-if="expiring.length === 0" class="iok">임박한 재료가 없어요. 잘 관리하고 있어요! 👍</p>
        <ul v-else class="ilist">
          <li v-for="it in expiring.slice(0, 5)" :key="it.fridgeItemId">
            <span class="iname">{{ it.name }} <span class="iqty">{{ it.qty }}{{ it.unit }}</span></span>
            <span class="idday" :class="dDayClass(it.dDay)">{{ dDayText(it.dDay) }}</span>
          </li>
        </ul>
        <p v-if="expiring.length > 5" class="imore">+{{ expiring.length - 5 }}개 더 · 냉장고에서 보기 ›</p>
      </div>

      <!-- 냉장고 요약 -->
      <div class="card" @click="router.push('/fridge')">
        <div class="chead"><span>🧊 내 냉장고</span><span class="total">{{ totalFridge }}개</span></div>
        <ul class="storage">
          <li><span>{{ summary.fridgeCount }}</span>{{ STORAGE.FRIDGE }}</li>
          <li><span>{{ summary.freezerCount }}</span>{{ STORAGE.FREEZER }}</li>
          <li><span>{{ summary.roomTempCount }}</span>{{ STORAGE.ROOM_TEMP }}</li>
        </ul>
      </div>

      <!-- 활동 요약 -->
      <ul class="acts" v-if="me">
        <li @click="router.push('/recipe')"><span>{{ me.stats?.wishlistCount ?? 0 }}</span>찜</li>
        <li @click="router.push('/mypage')"><span>{{ me.stats?.reviewCount ?? 0 }}</span>리뷰</li>
        <li @click="router.push('/challenge')"><span>🏆</span>챌린지</li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.hi { font-size: 18px; margin: 4px 0 14px; }
.ai-banner { display: flex; align-items: center; justify-content: space-between; gap: 12px; text-decoration: none;
  background: linear-gradient(135deg, #16a34a, #15803d); color: #fff; border-radius: 14px; padding: 16px 18px; margin-bottom: 16px; }
.ai-banner .ai-txt { display: flex; flex-direction: column; gap: 3px; }
.ai-banner strong { font-size: 15px; }
.ai-banner span { font-size: 12px; opacity: .85; }
.ai-banner .arrow { font-size: 24px; opacity: .8; }

.imminent { background: #fff7ed; border: 1px solid #fed7aa; border-radius: 14px; padding: 14px 16px; margin-bottom: 14px; cursor: pointer; }
.imminent.none { background: #f0fdf4; border-color: #bbf7d0; }
.ihead { display: flex; align-items: center; gap: 8px; }
.ititle { font-size: 15px; font-weight: 700; color: #c2410c; }
.imminent.none .ititle { color: #16a34a; }
.ibadge { background: #f97316; color: #fff; font-size: 12px; font-weight: 700; border-radius: 999px; min-width: 20px; height: 20px; display: inline-flex; align-items: center; justify-content: center; padding: 0 6px; }
.imminent.none .ibadge { background: #16a34a; }
.iok { font-size: 13px; color: #16a34a; margin: 8px 0 0; }
.ilist { list-style: none; padding: 0; margin: 10px 0 0; }
.ilist li { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-top: 1px solid #fde8d4; font-size: 14px; }
.iname { color: #333; }
.iqty { color: #aaa; font-size: 12px; }
.idday { font-size: 12px; font-weight: 700; }
.idday.soon { color: #f59e0b; }
.idday.expired { color: #ef4444; }
.imore { font-size: 12px; color: #c2410c; margin: 8px 0 0; text-align: right; }

.card { background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 14px 16px; margin-bottom: 14px; cursor: pointer; }
.chead { display: flex; justify-content: space-between; font-size: 14px; font-weight: 600; }
.chead .total { color: #16a34a; }
.storage { list-style: none; display: flex; gap: 10px; padding: 0; margin: 12px 0 0; }
.storage li { flex: 1; background: #f5f7f9; border-radius: 10px; padding: 10px; text-align: center; font-size: 12px; color: #666; }
.storage li span { display: block; font-size: 18px; font-weight: 800; color: #16a34a; }

.acts { list-style: none; display: flex; gap: 10px; padding: 0; margin: 0; }
.acts li { flex: 1; background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 14px 4px; text-align: center; font-size: 12px; color: #666; cursor: pointer; }
.acts li span { display: block; font-size: 18px; font-weight: 800; color: #333; margin-bottom: 2px; }

.muted { color: #999; }
.err { color: #e11d48; }
</style>
