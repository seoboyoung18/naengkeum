<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LottieIcon from './LottieIcon.vue'
import fridgeAnim from '../assets/fridge.json'
import homeIcon from '../assets/icons/home-alt.svg?raw'
import fridgeIcon from '../assets/icons/fridge.svg?raw'
import recipeIcon from '../assets/icons/pizza-slice.svg?raw'
import challengeIcon from '../assets/icons/trophy.svg?raw'
import userIcon from '../assets/icons/user.svg?raw'
import logoutIcon from '../assets/icons/log-out.svg?raw'
import adminIcon from '../assets/icons/shield-lock.svg?raw'
import WithdrawModal from './WithdrawModal.vue'
import { useToast } from '../composables/useToast'
import { computed, ref } from 'vue'

const auth = useAuthStore()
const router = useRouter()
const toast = useToast()
const showWithdraw = ref(false)

const items = computed(() => {
  const base = [
    { to: '/home', label: '홈', icon: homeIcon },
    { to: '/fridge', label: '냉장고', icon: fridgeIcon },
    { to: '/recipe', label: '레시피', icon: recipeIcon },
    { to: '/challenge', label: '챌린지', icon: challengeIcon },
    { to: '/mypage', label: '마이', icon: userIcon },
  ]
  if (auth.isAdmin) base.push({ to: '/admin', label: '관리자', icon: adminIcon })
  return base
})

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}

function onWithdrawn() {
  showWithdraw.value = false
  toast.success('탈퇴가 완료되었습니다')
  router.push({ name: 'login' })
}
</script>

<template>
  <aside class="side">
    <RouterLink to="/home" class="logo"><LottieIcon :data="fridgeAnim" :size="80" /> 냉큼</RouterLink>

    <nav class="nav">
      <RouterLink
        v-for="it in items"
        :key="it.to"
        :to="it.to"
        class="item"
        active-class="on"
      >
        <span class="ic" v-html="it.icon"></span>
        <span class="lb">{{ it.label }}</span>
      </RouterLink>
    </nav>

    <div class="bottom">
      <span v-if="auth.nickname" class="nick">{{ auth.nickname }}</span>
      <button class="logout" @click="logout">
        <span class="ic" v-html="logoutIcon"></span><span>로그아웃</span>
      </button>
      <button class="withdraw" @click="showWithdraw = true">회원탈퇴</button>
    </div>

    <WithdrawModal v-if="showWithdraw" @close="showWithdraw = false" @withdrawn="onWithdrawn" />
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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-weight: 800;
  font-size: 26px;
  color: var(--primary);
  text-decoration: none;
  letter-spacing: -0.3px;
  padding: 8px 8px 18px;
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.22);
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
.ic { width: 20px; height: 20px; display: inline-flex; align-items: center; justify-content: center; }

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
.withdraw {
  border: none;
  background: none;
  color: var(--mute);
  font-size: 12px;
  text-align: left;
  padding: 4px 12px;
  cursor: pointer;
  text-decoration: underline;
}
.withdraw:hover { color: #ef4444; }
</style>
