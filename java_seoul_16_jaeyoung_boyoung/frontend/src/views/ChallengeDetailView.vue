<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchChallengeDetail, joinChallenge, unjoinChallenge, updateProgress } from '../api/challenge'
import { useToast } from '../composables/useToast'

const props = defineProps({ challengeId: { type: [String, Number], required: true } })
const router = useRouter()
const toast = useToast()

const c = ref(null)
const loading = ref(true)
const error = ref('')
const busy = ref(false)

const PRESETS = [25, 50, 75, 100]

async function load() {
  loading.value = true
  error.value = ''
  try {
    c.value = await fetchChallengeDetail(props.challengeId)
  } catch (e) {
    error.value = e.response?.status === 404 ? '챌린지를 찾을 수 없습니다' : (e.response?.data?.message || '불러오기 실패')
  } finally {
    loading.value = false
  }
}

async function join() {
  busy.value = true
  try {
    await joinChallenge(props.challengeId)
    toast.success('챌린지에 참여했어요')
    await load()
  } catch (e) {
    if (e.response?.status === 409) toast.info('이미 참여 중이에요')
    else toast.error(e.response?.data?.message || '참여 실패')
  } finally { busy.value = false }
}

async function unjoin() {
  if (!confirm('참여를 취소할까요? 진행률이 초기화됩니다.')) return
  busy.value = true
  try {
    await unjoinChallenge(props.challengeId)
    toast.info('참여를 취소했어요')
    await load()
  } catch (e) {
    toast.error(e.response?.data?.message || '취소 실패')
  } finally { busy.value = false }
}

async function setProgress(p) {
  busy.value = true
  try {
    const res = await updateProgress(props.challengeId, p)
    c.value.myProgress = res.progress
    if (res.badgeEarned) toast.success('🎉 배지를 획득했어요!')
    else if (res.achieved) toast.success('챌린지 달성! 🎯')
    else toast.success(`진행률 ${res.progress}%`)
    await load()
  } catch (e) {
    toast.error(e.response?.data?.message || '진행률 갱신 실패')
  } finally { busy.value = false }
}

function dDayText(d) {
  if (d < 0) return '종료된 챌린지'
  if (d === 0) return '오늘 마감 (D-DAY)'
  return `D-${d}`
}

onMounted(load)
</script>

<template>
  <section>
    <button class="back" @click="router.back()">← 뒤로</button>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="error" class="err">{{ error }}</p>

    <div v-else-if="c" class="detail">
      <div class="dday" :class="{ ended: c.dDay < 0 }">{{ dDayText(c.dDay) }}</div>
      <h2 class="title">{{ c.title }}</h2>
      <p class="desc">{{ c.description }}</p>
      <div class="meta">👥 {{ c.participantCount }}명 참여 · {{ c.startDate }} ~ {{ c.endDate }}</div>

      <!-- 보상 배지 -->
      <div class="reward">
        <div class="ric">🏅</div>
        <div>
          <div class="rlbl">완료 보상</div>
          <div class="rname">{{ c.badge?.name }}</div>
        </div>
      </div>

      <!-- 참여 상태 / 진행률 -->
      <template v-if="c.myStatus === 'JOINED'">
        <div class="prog-head">
          <span>내 진행률</span><span class="pct">{{ c.myProgress }}%</span>
        </div>
        <div class="bar"><div class="fill" :style="{ width: c.myProgress + '%' }"></div></div>

        <div class="sec">진행률 갱신</div>
        <div class="presets">
          <button v-for="p in PRESETS" :key="p" :disabled="busy" :class="{ on: c.myProgress >= p }" @click="setProgress(p)">{{ p }}%</button>
        </div>
        <button class="unjoin" :disabled="busy" @click="unjoin">참여 취소</button>
      </template>

      <template v-else>
        <button class="join" :disabled="busy || c.dDay < 0" @click="join">
          {{ c.dDay < 0 ? '종료된 챌린지' : '챌린지 참여하기' }}
        </button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.back { border: none; background: none; color: #16a34a; font-size: 14px; cursor: pointer; padding: 0 0 14px; }
.detail { max-width: 720px; }
.dday { display: inline-block; font-size: 12px; font-weight: 800; color: #ef4444; background: #fef2f2; padding: 4px 10px; border-radius: 6px; }
.dday.ended { color: #999; background: #f1f3f5; }
.title { font-size: 22px; margin: 12px 0 6px; }
.desc { font-size: 14px; color: #555; margin: 0; }
.meta { font-size: 12px; color: #999; margin-top: 8px; }

.reward { display: flex; align-items: center; gap: 12px; background: #fff7ed; border: 1px solid #fde68a; border-radius: 12px; padding: 14px; margin: 18px 0; }
.ric { width: 46px; height: 46px; border-radius: 50%; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.rlbl { font-size: 12px; color: #b45309; }
.rname { font-size: 16px; font-weight: 700; color: #92400e; }

.prog-head { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; margin-top: 8px; }
.prog-head .pct { font-size: 20px; font-weight: 800; color: #16a34a; }
.bar { height: 10px; background: #f0f0f0; border-radius: 5px; overflow: hidden; margin: 8px 0; }
.bar .fill { height: 100%; background: #16a34a; transition: width .3s ease; }

.sec { font-size: 13px; color: #999; margin: 18px 0 8px; }
.presets { display: flex; gap: 8px; }
.presets button { flex: 1; padding: 11px; border: 1px solid #ddd; background: #fff; border-radius: 8px; font-size: 14px; cursor: pointer; color: #555; }
.presets button.on { border-color: #16a34a; background: #ecfdf3; color: #16a34a; font-weight: 700; }
.presets button:disabled { opacity: .6; }

.join { width: 100%; margin-top: 20px; padding: 14px; border: none; border-radius: 10px; background: #16a34a; color: #fff; font-size: 15px; font-weight: 700; cursor: pointer; }
.join:disabled { opacity: .5; background: #9ca3af; }
.unjoin { width: 100%; margin-top: 18px; padding: 12px; border: 1px solid #eee; background: #fff; border-radius: 8px; color: #888; font-size: 13px; cursor: pointer; }
.muted { color: #999; }
.err { color: #e11d48; }
</style>
