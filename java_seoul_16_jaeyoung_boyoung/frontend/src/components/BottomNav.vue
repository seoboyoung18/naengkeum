<script setup>
const items = [
  { to: '/home', label: '홈', icon: '🏠' },
  { to: '/fridge', label: '냉장고', icon: '🧊' },
  { to: '/recipe', label: '레시피', icon: '🍳' },
  { to: '/challenge', label: '챌린지', icon: '🏆' },
  { to: '/mypage', label: '마이', icon: '👤' },
]
</script>

<template>
  <nav class="bn">
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
</template>

<style scoped>
.bn {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  height: 64px;
  display: flex;
  background: #fff;
  border-top: 1px solid #eee;
  max-width: 480px;
  margin: 0 auto;
}
.item {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 2px; text-decoration: none; color: #9aa0a6; font-size: 11px;
}
.item.on { color: #16a34a; font-weight: 700; }
.ic { font-size: 20px; }
</style>
