<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="hd">
    <RouterLink to="/home" class="logo">🧊 냉큼</RouterLink>
    <div class="right">
      <span v-if="auth.isAuthenticated" class="nick">{{ auth.nickname }}</span>
      <button v-if="auth.isAuthenticated" class="logout" @click="logout">로그아웃</button>
    </div>
  </header>
</template>

<style scoped>
.hd {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.logo { font-weight: 800; font-size: 18px; color: #16a34a; text-decoration: none; }
.right { display: flex; align-items: center; gap: 10px; }
.nick { font-size: 13px; color: #444; }
.logout {
  font-size: 12px; border: 1px solid #ddd; background: #fafafa;
  border-radius: 6px; padding: 5px 10px; cursor: pointer;
}
</style>
