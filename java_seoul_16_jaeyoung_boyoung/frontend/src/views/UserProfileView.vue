<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchProfile } from '../api/member'
import { follow, unfollow } from '../api/follow'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const props = defineProps({ userId: { type: [String, Number], required: true } })
const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const profile = ref(null)
const loading = ref(true)
const error = ref('')
const busy = ref(false)

const isMe = computed(() => profile.value && auth.memberId === profile.value.memberId)

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await fetchProfile(props.userId)
  } catch (e) {
    error.value = e.response?.status === 404 ? '사용자를 찾을 수 없습니다' : (e.response?.data?.message || '불러오기 실패')
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  busy.value = true
  try {
    const res = profile.value.isFollowing
      ? await unfollow(profile.value.memberId)
      : await follow(profile.value.memberId)
    profile.value.isFollowing = res.following
    profile.value.followerCount = res.followerCount
    toast.success(res.following ? '팔로우했어요' : '팔로우를 취소했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '처리에 실패했어요')
  } finally {
    busy.value = false
  }
}

watch(() => props.userId, load)
onMounted(load)
</script>

<template>
  <section>
    <button class="back" @click="router.back()">← 뒤로</button>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="error" class="err">{{ error }}</p>

    <template v-else-if="profile">
      <div class="card">
        <div class="avatar">{{ profile.nickname?.[0] || '👤' }}</div>
        <div class="nick">{{ profile.nickname }}</div>
        <ul class="stats">
          <li><span>{{ profile.reviewCount }}</span>리뷰</li>
          <li><span>{{ profile.followerCount }}</span>팔로워</li>
        </ul>

        <button v-if="isMe" class="me" disabled>나</button>
        <button
          v-else
          class="follow"
          :class="{ on: profile.isFollowing }"
          :disabled="busy"
          @click="toggleFollow"
        >
          {{ profile.isFollowing ? '팔로잉' : '+ 팔로우' }}
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.back { border: none; background: none; color: var(--primary-deep); font-size: 14px; cursor: pointer; padding: 0 0 14px; }
.card { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 16px; padding: 32px 24px; text-align: center; max-width: 420px; }
.avatar { width: 72px; height: 72px; border-radius: 50%; background: var(--primary-tint); color: var(--primary-deep); font-size: 30px; font-weight: 800; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.nick { font-size: 20px; font-weight: 700; }
.stats { list-style: none; display: flex; justify-content: center; gap: 28px; padding: 0; margin: 16px 0 18px; }
.stats li { font-size: 12px; color: #888; }
.stats li span { display: block; font-size: 18px; font-weight: 800; color: #333; }
.follow { padding: 11px 28px; border: none; border-radius: 999px; background: var(--primary); color: var(--on-primary); font-size: 14px; font-weight: 700; cursor: pointer; }
.follow.on { background: #fff; color: var(--primary-deep); border: 1px solid var(--primary-deep); }
.follow:disabled { opacity: .6; }
.me { padding: 11px 28px; border: 1px solid var(--line); border-radius: 999px; background: #f5f7f9; color: #999; font-size: 14px; }
.muted { color: #999; }
.err { color: #e11d48; }
</style>
