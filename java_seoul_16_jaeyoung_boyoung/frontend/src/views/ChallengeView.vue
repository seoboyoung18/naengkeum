<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listChallenges, myChallenges, fetchChallengeStats } from '../api/challenge'
import { listBadges } from '../api/member'

const router = useRouter()

const TABS = [
  { key: 'active', label: '진행중' },
  { key: 'ended', label: '종료' },
  { key: 'my', label: '내 챌린지' },
]
const tab = ref('active')
const items = ref([])
const badges = ref([])
const activeParticipants = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = tab.value === 'my' ? await myChallenges() : await listChallenges(tab.value)
  } finally {
    loading.value = false
  }
}
function setTab(k) { tab.value = k; load() }

function dDayText(d) {
  if (d < 0) return '종료'
  if (d === 0) return 'D-DAY'
  return `D-${d}`
}

onMounted(async () => {
  await load()
  try { activeParticipants.value = (await fetchChallengeStats()).activeParticipants } catch (_) {}
  try { badges.value = await listBadges() } catch (_) {}
})
</script>

<template>
  <section>
    <h2 class="h">냉파 챌린지</h2>

    <!-- 통계 배너 -->
    <div class="banner">
      <span>🔥 지금 <b>{{ activeParticipants }}</b>명이 챌린지에 도전 중!</span>
      <span class="banner-sub">나도 도전해 식비를 아껴봐요</span>
    </div>

    <!-- 내 배지 -->
    <div class="badges">
      <div class="bsec">내 배지 <span class="cnt">{{ badges.length }}</span></div>
      <div v-if="badges.length === 0" class="muted small">아직 획득한 배지가 없어요. 챌린지를 완료해 보세요!</div>
      <div v-else class="bstrip">
        <div v-for="b in badges" :key="b.badgeId" class="badge">
          <div class="bic"><img v-if="b.iconUrl" :src="b.iconUrl" alt="" /><span v-else>🏅</span></div>
          <span class="bname">{{ b.name }}</span>
        </div>
      </div>
    </div>

    <!-- 탭 -->
    <div class="tabs">
      <button v-for="t in TABS" :key="t.key" :class="{ on: tab === t.key }" @click="setTab(t.key)">{{ t.label }}</button>
    </div>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="items.length === 0" class="muted empty">표시할 챌린지가 없어요.</p>

    <ul v-else class="list">
      <li v-for="c in items" :key="c.challengeId" class="card" @click="router.push({ name: 'challenge-detail', params: { challengeId: c.challengeId } })">
        <div class="top">
          <span class="dday" :class="{ ended: c.dDay < 0 }">{{ dDayText(c.dDay) }}</span>
          <span class="part">👥 {{ c.participantCount }}</span>
        </div>
        <div class="title">{{ c.title }}</div>
        <div class="desc">{{ c.description }}</div>
        <div class="foot">
          <span class="reward">🏅 {{ c.badge?.name }}</span>
          <span v-if="c.myStatus === 'JOINED'" class="joined">참여중 {{ c.myProgress }}%</span>
        </div>
        <div v-if="c.myStatus === 'JOINED'" class="bar"><div class="fill" :style="{ width: c.myProgress + '%' }"></div></div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.banner { background: linear-gradient(135deg, #f59e0b, #ef4444); color: #fff; border-radius: 14px; padding: 18px 22px; font-size: 15px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.banner b { font-size: 18px; }
.banner-sub { font-size: 13px; opacity: .92; }

.badges { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px 14px; margin-bottom: 16px; }
.bsec { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.bsec .cnt { color: #16a34a; }
.bstrip { display: flex; gap: 14px; overflow-x: auto; }
.badge { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 0 0 auto; }
.bic { width: 46px; height: 46px; border-radius: 50%; background: #fff7ed; display: flex; align-items: center; justify-content: center; font-size: 22px; border: 1px solid #fde68a; }
.bic img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.bname { font-size: 11px; color: #555; }

.tabs { display: flex; gap: 6px; margin-bottom: 12px; }
.tabs button { padding: 7px 14px; border: 1px solid #ddd; background: #fff; border-radius: 999px; font-size: 13px; color: #666; cursor: pointer; }
.tabs button.on { border-color: #16a34a; background: #ecfdf3; color: #16a34a; font-weight: 700; }

.list { list-style: none; padding: 0; margin: 0; display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.card { background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 16px; cursor: pointer; }
.card:hover { box-shadow: 0 6px 18px rgba(0,0,0,.06); }
.top { display: flex; justify-content: space-between; align-items: center; }
.dday { font-size: 12px; font-weight: 800; color: #ef4444; background: #fef2f2; padding: 3px 8px; border-radius: 6px; }
.dday.ended { color: #999; background: #f1f3f5; }
.part { font-size: 12px; color: #888; }
.title { font-size: 16px; font-weight: 700; margin-top: 8px; }
.desc { font-size: 13px; color: #888; margin-top: 3px; }
.foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; font-size: 12px; }
.reward { color: #f59e0b; font-weight: 600; }
.joined { color: #16a34a; font-weight: 700; }
.bar { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; margin-top: 8px; }
.bar .fill { height: 100%; background: #16a34a; }
.muted { color: #999; }
.small { font-size: 12px; }
.empty { text-align: center; padding: 36px 0; grid-column: 1 / -1; }

@media (max-width: 900px) { .list { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .list { grid-template-columns: 1fr; } .banner { flex-direction: column; align-items: flex-start; gap: 4px; } }
</style>
