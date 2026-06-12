<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LottieIcon from './LottieIcon.vue'

const auth = useAuthStore()
const router = useRouter()

const items = [
  { to: '/home', label: '홈', icon: '🏠' },
  { to: '/fridge', label: '냉장고', icon: '🧊' },
  { to: '/recipe', label: '레시피', icon: '🍳' },
  { to: '/challenge', label: '챌린지', icon: '🏆' },
  { to: '/mypage', label: '마이', icon: '👤' },
]

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <aside class="side">
    <RouterLink to="/home" class="logo"><LottieIcon :size="38" /> 냉큼</RouterLink>

    <nav class="nav">
      <RouterLink
        v-for="it in items"
        :key="it.to"
        :to="it.to"
        class="item"
        active-class="on"
      >
        <span class="ic">{{ it.icon }}</span>
        <span class="lb">{{ it.label }}</span>
      </RouterLink>
    </nav>

    <div class="bottom">
      <span v-if="auth.nickname" class="nick">{{ auth.nickname }}</span>
      <button class="logout" @click="logout">
        <span class="ic">↩️</span><span>로그아웃</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.side {
  width: 240px;
  flex-shrink: 0;
  background: var(--canvas);
  border-right: 1px solid var(--hairline-dark);
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  box-sizing: border-box;
}
.logo {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
  font-size: 20px;
  color: var(--primary);
  text-decoration: none;
  padding: 6px 8px 24px;
  letter-spacing: -0.3px;
}
.nav { display: flex; flex-direction: column; gap: 4px; }
.item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 12px;
  border-radius: var(--r-sm);
  text-decoration: none;
  color: var(--body-dark);
  font-size: 15px;
}
.item:hover { background: var(--canvas-soft); color: var(--ink); }
.item.on { background: rgba(0, 217, 146, 0.12); color: var(--primary); font-weight: 700; }
.ic { font-size: 18px; width: 20px; text-align: center; }

.bottom { margin-top: auto; display: flex; flex-direction: column; gap: 4px; }
.nick { font-size: 13px; color: var(--mute); padding: 0 12px 4px; }
.logout {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 12px;
  border: none;
  background: none;
  color: var(--body-dark);
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--r-sm);
}
.logout:hover { background: var(--canvas-soft); color: var(--ink); }
</style>
