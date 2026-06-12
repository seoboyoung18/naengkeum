<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listFollowing, listFollowers } from '../api/member'
import { follow, unfollow } from '../api/follow'
import { useToast } from '../composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const tab = ref(route.query.tab === 'followers' ? 'followers' : 'following')
const items = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = tab.value === 'followers' ? await listFollowers() : await listFollowing()
  } finally {
    loading.value = false
  }
}
function setTab(t) { tab.value = t; load() }

async function toggle(u) {
  const prev = u.isFollowing
  u.isFollowing = !prev
  try {
    if (prev) await unfollow(u.memberId)
    else await follow(u.memberId)
    toast.success(prev ? '팔로우를 취소했어요' : '팔로우했어요')
  } catch (e) {
    u.isFollowing = prev
    toast.error(e.response?.data?.message || '처리에 실패했어요')
  }
}

function goProfile(u) { router.push({ name: 'user-profile', params: { userId: u.memberId } }) }

onMounted(load)
</script>

<template>
  <section>
    <h2 class="h">팔로우</h2>

    <div class="tabs">
      <button :class="{ on: tab === 'following' }" @click="setTab('following')">팔로잉</button>
      <button :class="{ on: tab === 'followers' }" @click="setTab('followers')">팔로워</button>
    </div>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="items.length === 0" class="muted empty">
      {{ tab === 'followers' ? '아직 나를 팔로우하는 사람이 없어요.' : '아직 팔로우한 사람이 없어요.' }}
    </p>

    <ul v-else class="list">
      <li v-for="u in items" :key="u.memberId" class="row">
        <div class="who" @click="goProfile(u)">
          <div class="avatar">{{ u.nickname?.[0] || '👤' }}</div>
          <div>
            <div class="nick">{{ u.nickname }}</div>
            <div class="sub">리뷰 {{ u.reviewCount }}</div>
          </div>
        </div>
        <button class="follow" :class="{ on: u.isFollowing }" @click="toggle(u)">
          {{ u.isFollowing ? '팔로잉' : '+ 팔로우' }}
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.tabs { display: flex; gap: 8px; margin-bottom: 14px; max-width: 720px; }
.tabs button { flex: 1; padding: 10px; border: 1px solid var(--line); background: #fff; border-radius: 8px; font-size: 14px; color: #666; cursor: pointer; }
.tabs button.on { border-color: var(--primary-deep); background: var(--primary-tint); color: var(--primary-deep); font-weight: 700; }
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; max-width: 720px; }
.row { display: flex; align-items: center; justify-content: space-between; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 12px; padding: 10px 12px; }
.who { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.avatar { width: 40px; height: 40px; border-radius: 50%; background: var(--primary-tint); color: var(--primary-deep); font-weight: 800; display: flex; align-items: center; justify-content: center; }
.nick { font-size: 14px; font-weight: 600; }
.sub { font-size: 12px; color: #999; }
.follow { padding: 7px 14px; border: none; border-radius: 999px; background: var(--primary); color: var(--on-primary); font-size: 12px; font-weight: 700; cursor: pointer; }
.follow.on { background: #fff; color: var(--primary-deep); border: 1px solid var(--primary-deep); }
.muted { color: #999; }
.empty { text-align: center; padding: 36px 0; }
</style>
