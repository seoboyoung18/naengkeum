<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const me = ref(null)
const err = ref('')

onMounted(async () => {
  try {
    me.value = await auth.fetchMe()
  } catch (e) {
    err.value = e.response?.data?.message || '내 정보를 불러오지 못했습니다'
  }
})
</script>

<template>
  <section>
    <h2 class="h">홈 (대시보드)</h2>

    <div v-if="me" class="box">
      <p class="hi">안녕하세요, <b>{{ me.nickname }}</b>님 👋</p>
      <ul class="stats">
        <li><span>{{ me.stats?.fridgeCount ?? 0 }}</span>냉장고</li>
        <li><span>{{ me.stats?.reviewCount ?? 0 }}</span>리뷰</li>
        <li><span>{{ me.stats?.wishlistCount ?? 0 }}</span>찜</li>
      </ul>
      <p class="ok">✅ 인증 API(/api/member/me) 호출 성공 — 프론트 셋업 검증 완료</p>
    </div>

    <p v-else-if="err" class="err">{{ err }}</p>
    <p v-else class="muted">불러오는 중…</p>
  </section>
</template>

<style scoped>
.box { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 18px; }
.hi { margin: 0 0 14px; font-size: 16px; }
.stats { list-style: none; display: flex; gap: 12px; padding: 0; margin: 0 0 14px; }
.stats li { flex: 1; background: #f5f7f9; border-radius: 10px; padding: 12px; text-align: center; font-size: 12px; color: #666; }
.stats li span { display: block; font-size: 20px; font-weight: 800; color: #16a34a; margin-bottom: 2px; }
.ok { color: #16a34a; font-size: 13px; margin: 0; }
.err { color: #e11d48; }
.muted { color: #999; }
</style>
