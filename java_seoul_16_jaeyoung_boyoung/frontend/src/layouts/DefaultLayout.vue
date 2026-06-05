<script setup>
import AppHeader from '../components/AppHeader.vue'
import BottomNav from '../components/BottomNav.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />
    <main class="app-main">
      <slot />
    </main>
    <BottomNav />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}
.app-main {
  flex: 1;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  padding: 16px 16px 84px;
  box-sizing: border-box;
}
</style>
